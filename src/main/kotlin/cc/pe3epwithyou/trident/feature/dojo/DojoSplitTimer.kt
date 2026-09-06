package cc.pe3epwithyou.trident.feature.dojo

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.interfaces.DialogCollection
import cc.pe3epwithyou.trident.state.FontCollection
import cc.pe3epwithyou.trident.state.Game
import cc.pe3epwithyou.trident.state.MCCIState
import cc.pe3epwithyou.trident.utils.Logger
import cc.pe3epwithyou.trident.utils.Resources
import cc.pe3epwithyou.trident.utils.ScoreboardUtils
import cc.pe3epwithyou.trident.utils.minecraft
import net.minecraft.ChatFormatting
import net.minecraft.data.AtlasIds
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.regex.Pattern

/**
 * Key the [cc.pe3epwithyou.trident.interfaces.dojo.DojoSplitsDialog] is opened/refreshed under.
 */
const val DOJO_SPLITS_DIALOG_KEY = "dojo_splits"

/**
 * One completed split in the current Dojo run, for the LiveSplit-style split list.
 *
 * [levelUid] is a composite `origin_destination` key (e.g. `"START_M1-1"`, `"B1-3_M1-1"`) — the
 * unnamed transition leading into a level takes a genuinely different amount of time depending
 * on where you came from, so it's tracked and saved separately from the level's own obstacle
 * time (which is identical no matter the route). [timeSeconds] is the two combined, since
 * that's what's actually displayed as this row's split time.
 */
data class DojoSplitRow(
    val levelUid: String,
    val levelName: String,
    val timeSeconds: Double,
    val deltaSeconds: Double?
)

/**
 * Tracks split times for a single Parkour Warrior: Dojo run.
 *
 * A new instance is created every time the "go" countdown sound plays. It's no longer
 * discarded when the round ends — [finish] just freezes its clocks — so the splits dialog can
 * keep showing the finished run's results until the next run actually starts.
 */
class DojoSplitTimer private constructor(val courseName: String?) {
    /** When the current segment (transition + level) started: the previous medal, or "go" for the first. */
    private var transitionStartTimestamp: Long = System.currentTimeMillis()

    /** When the current level's own subtitle arrived — the transition/level boundary. Null while still in the unnamed transition. */
    private var levelStartTimestamp: Long? = null

    private val runStartTimestamp: Long = System.currentTimeMillis()
    private var finishTimestamp: Long? = null

    /** Display name of the just-finished/previous level, used to key the next composite uid. */
    private var previousLevelName: String = "START"

    /** Destination-only name of the active/last level, e.g. "M1-1". Empty until the first level subtitle arrives. Also the key its (route-independent) obstacle time is saved under. */
    var levelName: String = ""
        private set

    /** Composite "origin_destination" key of the active/last level, e.g. "START_M1-1". Empty until the first level subtitle arrives. Also the key its (route-specific) transition time is saved under. */
    var currentLevelUid: String = ""
        private set

    /** Whether we're currently in the unnamed transition before a level's identity is known (no row can be shown as "live" yet). */
    var isBetween: Boolean = true
        private set

    /** Once true (round ended/restarted/left), all times are frozen and no more splits are recorded. */
    val isFinished: Boolean get() = finishTimestamp != null

    /** Uid of the last segment actually saved, so a duplicate save attempt (e.g. a race between the medal subtitle and the round-end sound both firing for the same final split) can't double-count it. */
    private var lastSavedUid: String? = null

    /** Splits completed so far this run, in order, for the LiveSplit-style split list. */
    val completedSplits: MutableList<DojoSplitRow> = mutableListOf()

    /**
     * Handles a subtitle packet while a Dojo run is active.
     *
     * The server sends two kinds of subtitle: `[LevelName]` when a new level starts, and a
     * very short one (a medal icon) the instant a level is completed. We treat any subtitle
     * shorter than 4 characters as a completed split.
     */
    fun handleSubtitle(packet: ClientboundSetSubtitleTextPacket, ci: CallbackInfo) {
        if (isFinished) return
        val component = packet.text()
        val string = component.string
        if (string.length < 4) {
            modifyMedalTitle(component, ci)
            return
        }

        if (!string.startsWith("[")) return
        val matcher = LEVEL_NAME_PATTERN.matcher(string)
        if (!matcher.find()) return

        // This is the transition/level boundary: the unnamed corridor leading here ends now,
        // and the (route-independent) obstacle begins. transitionStartTimestamp is untouched,
        // so the two pieces can be measured separately once this level's medal is found.
        levelStartTimestamp = System.currentTimeMillis()
        levelName = matcher.group(1)
        currentLevelUid = "${previousLevelName}_$levelName"
        isBetween = false
        Logger.debugLog("DojoSplitTimer - Detected level with id: $currentLevelUid")

        DialogCollection.refreshDialog(DOJO_SPLITS_DIALOG_KEY)
    }

    private fun modifyMedalTitle(component: Component, ci: CallbackInfo) {
        var modified: MutableComponent = component.copy()
        if (Config.Dojo.showSplitImprovements) {
            modified = modified.append(splitImprovementComponent())
        }
        minecraft().gui.hud.setSubtitle(modified)
        ci.cancel()

        previousLevelName = levelName
        saveSplit()
        transitionStartTimestamp = System.currentTimeMillis()
        levelStartTimestamp = null
        isBetween = true
    }

    fun saveSplit() {
        val course = courseName ?: return
        val levelStart = levelStartTimestamp ?: return // nothing has actually started yet
        if (currentLevelUid.isEmpty()) return
        // Guards against this exact segment being saved twice — e.g. the round-end sound and
        // the final medal's subtitle can arrive in either order, and if the sound is
        // processed first, both the fallback save (see onSound) and the normal medal-detected
        // save below would otherwise both fire for the same split.
        if (currentLevelUid == lastSavedUid) return
        lastSavedUid = currentLevelUid

        val now = finishTimestamp ?: System.currentTimeMillis()
        val transitionSeconds = (levelStart - transitionStartTimestamp) / 1000.0
        val levelSeconds = (now - levelStart) / 1000.0
        val totalSeconds = transitionSeconds + levelSeconds

        val finishedUid = currentLevelUid
        val finishedName = levelName
        val delta = if (Config.Dojo.showSplitImprovements) splitImprovement() else null
        completedSplits.add(DojoSplitRow(finishedUid, finishedName, totalSeconds, delta))

        sendSplitCompleteMessage(totalSeconds, delta)

        // Saved as two separate historical records under different keys: the transition
        // (route-specific — depends on where you came from) and the level itself
        // (route-independent — the same obstacle no matter how you got there).
        DojoSplitManager.saveSplit(course, finishedUid, finishedName, (transitionSeconds * 1000).toLong())
        DojoSplitManager.saveSplit(course, finishedName, finishedName, (levelSeconds * 1000).toLong())
        DialogCollection.refreshDialog(DOJO_SPLITS_DIALOG_KEY)
    }

    private fun sendSplitCompleteMessage(totalSeconds: Double, delta: Double?) {
        if (!Config.Dojo.sendSplitTime) return
        val time = String.format("%.3fs", totalSeconds)

        val component: MutableComponent = Component.literal("[").withStyle(ChatFormatting.GREEN)
            .append(FontCollection.get("_fonts/icon/tick_small"))
            .append("] ")
            .append(Component.translatable("trident.dojo.split_complete", levelName))
            .append(Component.literal(time).withStyle(ChatFormatting.WHITE))
        if (Config.Dojo.showSplitImprovements) {
            component.append(Component.empty().withStyle(ChatFormatting.WHITE).append(splitImprovementComponent(delta)))
        }
        Logger.sendMessage(component)
    }

    private fun splitImprovementComponent(overrideDelta: Double? = null): MutableComponent {
        val improvement = overrideDelta ?: splitImprovement() ?: 0.0
        val formatted = String.format("%.2f", improvement)

        val color: ChatFormatting
        val icon: Component
        val text: String
        when {
            improvement > 0 -> {
                color = ChatFormatting.RED
                icon = splitIcon(up = true)
                text = "+$formatted"
            }

            improvement < 0 -> {
                color = ChatFormatting.GREEN
                icon = splitIcon(up = false)
                text = formatted
            }

            else -> {
                color = ChatFormatting.YELLOW
                icon = Component.literal("-").withStyle(color)
                text = formatted
            }
        }

        return Component.literal(" (")
            .append(icon)
            .append(Component.literal(" $text").withStyle(color))
            .append(")")
    }

    private fun splitIcon(up: Boolean): Component =
        FontCollection.texture(Resources.trident(if (up) "dojo/split_up" else "dojo/split_down"), AtlasIds.GUI)

    fun currentSplitTimeMillis(): Long = (finishTimestamp ?: System.currentTimeMillis()) - transitionStartTimestamp
    fun currentSplitTimeSeconds(): Double = currentSplitTimeMillis() / 1000.0
    fun totalElapsedSeconds(): Double = ((finishTimestamp ?: System.currentTimeMillis()) - runStartTimestamp) / 1000.0

    /**
     * Compares the current split's elapsed time against the combined best (transition best +
     * level best). Only returns a value once both pieces have historical data — no partial
     * comparisons mid-run.
     */
    fun splitImprovement(): Double? {
        val course = courseName ?: return null
        val transitionBest = DojoSplitManager.getSplitSeconds(course, currentLevelUid) ?: return null
        val levelBest = DojoSplitManager.getSplitSeconds(course, levelName) ?: return null
        return currentSplitTimeSeconds() - (transitionBest + levelBest)
    }

    /** Freezes this run's clocks in place. Called on round end — the timer keeps existing (and displaying) until a new run starts. */
    fun finish() {
        if (finishTimestamp == null) finishTimestamp = System.currentTimeMillis()
    }

    companion object {
        private val LEVEL_NAME_PATTERN: Pattern = Pattern.compile("\\[(.*)]")
        private val COURSE_NAME_PATTERN = Regex("""COURSE: (.*)""")

        var instance: DojoSplitTimer? = null
            private set

        fun setInstance(timer: DojoSplitTimer?) {
            instance = timer
            DialogCollection.refreshDialog(DOJO_SPLITS_DIALOG_KEY)
        }

        /** Clears the splits box back to showing just historical bests, without touching saved history. */
        fun clearDisplay() {
            instance = null
            DialogCollection.refreshDialog(DOJO_SPLITS_DIALOG_KEY)
        }

        @JvmStatic
        fun onSound(packet: ClientboundSoundPacket) {
            if (!Config.Dojo.enabled) return
            val soundLoc = packet.sound.value().location()
            val path = soundLoc.path
            val isRoundEnd = path == "games.global.timer.round_end"

            if (path.contains("games.parkour_warrior.mode_swap") ||
                path.contains("games.parkour_warrior.restart_course") ||
                isRoundEnd ||
                path == "ui.queue_teleport"
            ) {
                val current = instance
                // Only a fallback for a round ending abruptly before a final medal was ever
                // detected. The normal case (medal -> then round-end sound) already saved this
                // split via modifyMedalTitle, at which point isBetween is true — calling
                // saveSplit() again here would double-save it, covering the gap between the
                // medal and this sound as a bogus extra split.
                if (current != null && isRoundEnd && !current.isBetween) current.saveSplit()
                current?.finish()
                DialogCollection.refreshDialog(DOJO_SPLITS_DIALOG_KEY)
                Logger.debugLog("DojoSplitTimer - Ended timer")
            } else if (path == "games.global.countdown.go") {
                val courseName = if (MCCIState.game == Game.PARKOUR_WARRIOR_DOJO) {
                    ScoreboardUtils.findInScoreboard(COURSE_NAME_PATTERN)?.groupValues?.getOrNull(1)
                } else null
                setInstance(DojoSplitTimer(courseName))
                Logger.debugLog("DojoSplitTimer - Started timer!")
            }
        }
    }
}

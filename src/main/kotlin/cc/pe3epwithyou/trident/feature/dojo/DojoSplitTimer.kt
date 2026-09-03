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
 * One completed level in the current Dojo run, kept for the LiveSplit-style split list.
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
 * A new instance is created every time the "go" countdown sound plays, and lives until the
 * round ends, the course restarts, or the player leaves the game. Ported from IslandUtils'
 * `LevelTimer`.
 */
class DojoSplitTimer private constructor(val courseName: String?) {
    private var lastSplitTimestamp: Long = System.currentTimeMillis()
    private val runStartTimestamp: Long = System.currentTimeMillis()

    var levelName: String = "M1-1"
        private set

    var currentLevelUid: String = ""
        private set

    /** Whether the player is currently between levels (not actively timing a split). */
    var isBetween: Boolean = true
        private set

    /** Levels completed so far this run, in order, for the LiveSplit-style split list. */
    val completedSplits: MutableList<DojoSplitRow> = mutableListOf()

    /**
     * Handles a subtitle packet while a Dojo run is active.
     *
     * The server sends two kinds of subtitle: `[LevelName]` when a new level starts, and a
     * very short one (a medal icon) the instant a level is completed. We treat any subtitle
     * shorter than 4 characters as a completed split.
     */
    fun handleSubtitle(packet: ClientboundSetSubtitleTextPacket, ci: CallbackInfo) {
        val component = packet.text()
        val string = component.string
        if (string.length < 4) {
            modifyMedalTitle(component, ci)
            return
        }

        if (!string.startsWith("[")) return
        val matcher = LEVEL_NAME_PATTERN.matcher(string)
        if (!matcher.find()) return

        // This subtitle is sent ~1.5s after the level actually starts, so compensate
        lastSplitTimestamp = System.currentTimeMillis() - 1500
        levelName = matcher.group(1)
        isBetween = false

        val hash = StringBuilder()
        component.siblings.forEach { sibling -> sibling.style.color?.let { hash.append(it) } }
        hash.append(levelName)
        currentLevelUid = hash.toString()
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

        saveSplit()
        lastSplitTimestamp = System.currentTimeMillis()
        isBetween = true
    }

    fun saveSplit() {
        val course = courseName ?: return
        val finishedUid = currentLevelUid
        val finishedName = levelName
        val finishedTime = currentSplitTimeSeconds()
        val delta = if (Config.Dojo.showSplitImprovements) splitImprovement() else null
        completedSplits.add(DojoSplitRow(finishedUid, finishedName, finishedTime, delta))

        sendSplitCompleteMessage()
        DojoSplitManager.saveSplit(course, finishedUid, finishedName, currentSplitTimeMillis())
        DialogCollection.refreshDialog(DOJO_SPLITS_DIALOG_KEY)
    }

    private fun sendSplitCompleteMessage() {
        if (!Config.Dojo.sendSplitTime) return
        val time = String.format("%.3fs", currentSplitTimeSeconds())

        val component: MutableComponent = Component.literal("[").withStyle(ChatFormatting.GREEN)
            .append(FontCollection.get("_fonts/icon/tick_small"))
            .append("] ")
            .append(Component.translatable("trident.dojo.split_complete", levelName))
            .append(Component.literal(time).withStyle(ChatFormatting.WHITE))
        if (Config.Dojo.showSplitImprovements) {
            component.append(Component.empty().withStyle(ChatFormatting.WHITE).append(splitImprovementComponent()))
        }
        Logger.sendMessage(component)
    }

    private fun splitImprovementComponent(): MutableComponent {
        val improvement = splitImprovement() ?: 0.0
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

    fun currentSplitTimeMillis(): Long = System.currentTimeMillis() - lastSplitTimestamp
    fun currentSplitTimeSeconds(): Double = currentSplitTimeMillis() / 1000.0
    fun totalElapsedSeconds(): Double = (System.currentTimeMillis() - runStartTimestamp) / 1000.0

    fun splitImprovement(): Double? {
        val course = courseName ?: return null
        val split = DojoSplitManager.getSplitSeconds(course, currentLevelUid) ?: return null
        return currentSplitTimeSeconds() - split
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
                if (current != null && isRoundEnd) current.saveSplit()
                setInstance(null)
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

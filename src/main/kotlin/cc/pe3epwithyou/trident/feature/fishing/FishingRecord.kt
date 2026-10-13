package cc.pe3epwithyou.trident.feature.fishing

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.interfaces.DialogCollection
import cc.pe3epwithyou.trident.state.Game
import cc.pe3epwithyou.trident.state.MCCIState
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import java.util.*

const val FISHING_RECORD_DIALOG_KEY = "fishing_record"

/** One row of the Fishing Record, in display order. [glyph] is the icon in the trident icon font. */
enum class RecordStat(val label: String, val glyph: String) {
    REEL_INS("Reel-ins", "\uE019"),
    XP("XP", "\uE01A"),
    JUNK("Junk", "\uE01B"),
    NORMAL("Normal", "\uE01C"),
    ELUSIVE("Elusive", "\uE01D"),
    PEARL("Pearl", "\uE01E"),
    TREASURE("Treasure", "\uE01F"),
    SPIRIT("Spirit", "\uE020"),
}

/**
 * Session-long record of what the player has caught while fishing: time spent, reel-ins, XP and
 * a count per [CatchCategory], each with an optional earn rate. Ported from Compact Fishing
 * Message's record overlay; shown by [cc.pe3epwithyou.trident.interfaces.fishing.FishingRecordDialog].
 *
 * Catches are read from chat: a "You caught" message starts a pending catch, and the
 * "You earned: N Island XP" message that follows completes (and records) it.
 */
object FishingRecord {
    private val CAUGHT_PATTERN = Regex("""\([^)]*\) You caught: \[(.+?)](?: x(\d+))?\s*$""")
    private val TRIGGER_PATTERN = Regex("""^.*? (Triggered|Special): .+? (.+)$""")
    private val XP_PATTERN = Regex(""".* You earned: ([\d,]+) Island XP""")

    /** A catch with no further messages for this long is dropped (its XP message never came). */
    private const val CATCH_TIMEOUT_TICKS = 20 * 3

    private class PendingCatch(val category: CatchCategory, val count: Int) {
        var idleTicks = 0
    }

    private var pending: PendingCatch? = null
    private var tickCounter = 0

    var fishingTime = 0
        private set
    private val amounts = EnumMap<RecordStat, Int>(RecordStat::class.java)

    /** Cached "(x.x/min)" suffixes, used instead of the live rate when the interval is ON_CATCH. */
    private val onCatchRates = EnumMap<RecordStat, String>(RecordStat::class.java)

    fun amount(stat: RecordStat): Int = amounts[stat] ?: 0

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { tick() }
        ClientReceiveMessageEvents.ALLOW_GAME.register allow@{ message, _ ->
            if (!MCCIState.isOnIsland()) return@allow true
            if (MCCIState.game != Game.FISHING || !Config.Fishing.recordModule) return@allow true
            handleMessage(message.string)
            true // never cancels the message
        }
    }

    private fun tick() {
        if (!MCCIState.isOnIsland() || MCCIState.game != Game.FISHING) {
            pending = null
            return
        }
        pending?.let {
            if (++it.idleTicks > CATCH_TIMEOUT_TICKS) pending = null
        }
        if (!Config.Fishing.recordModule) return

        tickCounter++
        val intervalTicks = Config.Fishing.recordUpdateInterval.seconds * 20
        var changed = false
        while (tickCounter >= intervalTicks) {
            fishingTime += intervalTicks / 20
            tickCounter -= intervalTicks
            changed = true
        }
        if (changed) DialogCollection.refreshDialog(FISHING_RECORD_DIALOG_KEY)
    }

    private fun handleMessage(text: String) {
        CAUGHT_PATTERN.find(text)?.let {
            val name = it.groupValues[1].trim()
            val count = it.groupValues[2].takeIf(String::isNotEmpty)?.toIntOrNull() ?: 1
            pending = PendingCatch(CatchCategory.fromItemName(name), count)
            return
        }

        val current = pending ?: return

        if (TRIGGER_PATTERN.containsMatchIn(text)) {
            current.idleTicks = 0
            return
        }

        XP_PATTERN.find(text)?.let {
            val xp = it.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            record(current.category, current.count, xp)
            pending = null
        }
    }

    private fun record(category: CatchCategory, count: Int, xp: Int) {
        add(RecordStat.REEL_INS, 1)
        add(RecordStat.XP, xp)
        add(
            when (category) {
                CatchCategory.JUNK -> RecordStat.JUNK
                CatchCategory.NORMAL_FISH -> RecordStat.NORMAL
                CatchCategory.ELUSIVE_FISH -> RecordStat.ELUSIVE
                CatchCategory.PEARL -> RecordStat.PEARL
                CatchCategory.TREASURE -> RecordStat.TREASURE
                CatchCategory.SPIRIT -> RecordStat.SPIRIT
            }, count
        )
        refreshOnCatchRates()
        DialogCollection.refreshDialog(FISHING_RECORD_DIALOG_KEY)
    }

    private fun add(stat: RecordStat, n: Int) {
        amounts[stat] = amount(stat) + n
    }

    /** The "(x.x/min)" or "(x.x/hr)" suffix for [stat], or an empty string if earn rates are off. */
    fun earnRateSuffix(stat: RecordStat): String {
        val mode = Config.Fishing.recordEarnRate
        if (mode == RecordEarnRate.OFF || fishingTime <= 0) return ""
        if (Config.Fishing.recordUpdateInterval == RecordUpdateInterval.ON_CATCH) {
            return onCatchRates[stat] ?: liveRate(mode, stat)
        }
        return liveRate(mode, stat)
    }

    private fun liveRate(mode: RecordEarnRate, stat: RecordStat): String {
        val amount = amount(stat)
        return if (mode == RecordEarnRate.HOUR) {
            " (%.1f/hr)".format(Locale.ROOT, amount / (fishingTime / 3600.0))
        } else {
            " (%.1f/min)".format(Locale.ROOT, amount / (fishingTime / 60.0))
        }
    }

    private fun refreshOnCatchRates() {
        val mode = Config.Fishing.recordEarnRate
        if (mode == RecordEarnRate.OFF || fishingTime <= 0) return
        RecordStat.entries.forEach { onCatchRates[it] = liveRate(mode, it) }
    }

    fun formatTime(totalSeconds: Int): String {
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return (if (hrs > 0) "${hrs}h " else "") + (if (mins > 0) "${mins}m " else "") + "${secs}s"
    }
}

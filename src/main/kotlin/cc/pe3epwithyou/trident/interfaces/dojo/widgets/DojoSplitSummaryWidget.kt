package cc.pe3epwithyou.trident.interfaces.dojo.widgets

import cc.pe3epwithyou.trident.feature.dojo.DojoSplitManager
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitTimer
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.util.opaqueColor
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/**
 * Two summary lines under the split list:
 * - CURRENT PACE: actual times of splits completed this run, plus the currently active
 *   split's contribution capped at its own best (or the live time once you've gone over it),
 *   plus best-known times for every split not yet started. This only moves when a split
 *   finishes faster than expected, or once the split in progress runs longer than its own
 *   best — it does not just tick up with the clock. While in the unnamed transition between
 *   splits (before the next level's identity is known), it holds steady rather than guessing.
 * - SUM OF BEST: the sum of your best-ever time on every planned section, independent of
 *   how this particular run is going — the theoretical ceiling for the planned route.
 *
 * Only counts levels in [orderedRows], so both numbers reflect whatever route was planned.
 */
class DojoSplitSummaryWidget(
    private val orderedRows: List<Pair<String, String>>,
    width: Int
) : AbstractWidget(0, 0, width, HEIGHT, Component.empty()) {
    companion object {
        const val HEIGHT = 22
        private const val PADDING = 5
        private const val LINE_HEIGHT = 11
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val font = minecraft().font
        val timer = DojoSplitTimer.instance
        val course = timer?.courseName ?: DojoSplitManager.lastCourseName

        fun bestFor(uid: String, levelName: String): Double? {
            val transitionBest = course?.let { DojoSplitManager.getSplitSeconds(it, uid) }
            val levelBest = course?.let { DojoSplitManager.getSplitSeconds(it, levelName) }
            return if (transitionBest != null && levelBest != null) transitionBest + levelBest else null
        }

        var totalBest = 0.0
        var totalKnown = true
        var afterCurrentBest = 0.0
        var afterCurrentKnown = true

        orderedRows.forEach { (uid, levelName) ->
            val best = bestFor(uid, levelName)
            if (best != null) totalBest += best else totalKnown = false

            val alreadyDone = timer != null && timer.completedSplits.any { it.levelUid == uid }
            val isCurrent = timer != null && !timer.isBetween && timer.currentLevelUid == uid
            if (!alreadyDone && !isCurrent) {
                if (best != null) afterCurrentBest += best else afterCurrentKnown = false
            }
        }

        val paceText = if (timer == null) "--" else {
            val completedActual = timer.completedSplits.sumOf { it.timeSeconds }
            // Only counts a "current" contribution if the segment isn't already saved in
            // completedSplits (defends against any state-transition path — e.g. an ending
            // whose completion is only ever signaled by a title, not a medal subtitle — that
            // might leave isBetween stuck false after already finalizing the segment; without
            // this check that segment's time would be added twice) and the transition has
            // resolved into a known level (isBetween == false) — before that we don't know
            // which best to compare against, so hold steady rather than guessing.
            val alreadyDoneCurrent = timer.completedSplits.any { it.levelUid == timer.currentLevelUid }
            val currentContribution = if (!timer.isBetween && !alreadyDoneCurrent) {
                val currentBest = bestFor(timer.currentLevelUid, timer.levelName)
                val live = timer.currentSplitTimeSeconds()
                if (currentBest != null) maxOf(currentBest, live) else live
            } else 0.0

            val paceSeconds = completedActual + currentContribution + afterCurrentBest
            formatTime(paceSeconds) + (if (!afterCurrentKnown) "+" else "")
        }
        val paceLabel = Component.literal("CURRENT PACE ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(paceText).withStyle(if (timer == null) ChatFormatting.DARK_GRAY else ChatFormatting.WHITE))
        graphics.text(font, paceLabel, x + PADDING, y + 1, 0xFFFFFF.opaqueColor())

        val bestText = formatTime(totalBest) + (if (!totalKnown) "+" else "")
        val bestLabel = Component.literal("SUM OF BEST ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(bestText).withStyle(ChatFormatting.WHITE))
        graphics.text(font, bestLabel, x + PADDING, y + LINE_HEIGHT + 1, 0xFFFFFF.opaqueColor())
    }

    private fun formatTime(seconds: Double): String {
        val clamped = seconds.coerceAtLeast(0.0)
        val minutes = (clamped / 60).toInt()
        val remaining = clamped - minutes * 60
        return if (minutes > 0) String.format("%d:%05.2f", minutes, remaining) else String.format("%.2f", remaining)
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) = Unit
}

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
 * - CURRENT PACE: the actual times of splits completed on this run, plus best-known times
 *   for every split not yet completed (including the currently active split). This is a
 *   projection of the final run time that does not move while an active split is being played.
 * - SUM OF BEST: the sum of your best-ever time on every planned section, independent of
 *   how this particular run is going — the theoretical ceiling for the planned route.
 *
 * Only counts levels in [orderedUids], so both numbers reflect whatever route was planned.
 */
class DojoSplitSummaryWidget(
    private val orderedUids: List<String>,
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

        var completedTime = 0.0
        var remainingBest = 0.0
        var remainingKnown = true
        var totalBest = 0.0
        var totalKnown = true

        val completed = timer?.completedSplits?.associateBy { it.levelUid }.orEmpty()

        orderedUids.forEach { uid ->
            val best = course?.let { DojoSplitManager.getSplitSeconds(it, uid) }
            if (best != null) totalBest += best else totalKnown = false

            val completedSplit = completed[uid]
            if (completedSplit != null) {
                // Completed splits use their actual time from this run.
                completedTime += completedSplit.timeSeconds
            } else if (timer?.currentLevelUid == uid && !timer.isBetween) {
                // The active split uses its historical best until the live time exceeds it.
                // Once we're slower than best, switch to the actual current split time so
                // the pace reflects the fact that the run is now behind pace.
                if (best != null) {
                    remainingBest += maxOf(best, timer.currentSplitTimeSeconds())
                } else {
                    remainingKnown = false
                }
            } else {
                // Future / otherwise unfinished splits use their historical best.
                if (best != null) remainingBest += best else remainingKnown = false
            }
        }

        val paceText = if (timer == null) "--" else {
            val paceSeconds = completedTime + remainingBest
            formatTime(paceSeconds) + (if (!remainingKnown) "+" else "")
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

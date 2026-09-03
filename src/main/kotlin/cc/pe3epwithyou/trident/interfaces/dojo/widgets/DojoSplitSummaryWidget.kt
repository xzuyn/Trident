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
 * Shows a running "best possible time" estimate (completed times + best-known times for the
 * rest of the planned route) and a cumulative pace delta, similar to LiveSplit's sum-of-best /
 * possible time save readouts. Only counts levels in [orderedUids], so it reflects whatever
 * route was planned for this run.
 */
class DojoSplitSummaryWidget(
    private val orderedUids: List<String>,
    width: Int
) : AbstractWidget(0, 0, width, HEIGHT, Component.empty()) {
    companion object {
        const val HEIGHT = 11
        private const val PADDING = 5
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val font = minecraft().font
        val timer = DojoSplitTimer.instance
        val course = timer?.courseName ?: DojoSplitManager.lastCourseName

        var bestPossible = 0.0
        var knownRemaining = true
        var cumulativeDelta = 0.0
        var hasDelta = false

        orderedUids.forEach { uid ->
            val completed = timer?.completedSplits?.firstOrNull { it.levelUid == uid }
            when {
                completed != null -> {
                    bestPossible += completed.timeSeconds
                    completed.deltaSeconds?.let {
                        cumulativeDelta += it
                        hasDelta = true
                    }
                }

                timer != null && !timer.isBetween && timer.currentLevelUid == uid -> {
                    val best = course?.let { DojoSplitManager.getSplitSeconds(it, uid) }
                    val live = timer.currentSplitTimeSeconds()
                    bestPossible += if (best != null) maxOf(best, live) else live
                    timer.splitImprovement()?.let {
                        cumulativeDelta += it
                        hasDelta = true
                    }
                }

                else -> {
                    val best = course?.let { DojoSplitManager.getSplitSeconds(it, uid) }
                    if (best != null) bestPossible += best else knownRemaining = false
                }
            }
        }

        val bestLabel = Component.literal("BEST POSSIBLE ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(formatTime(bestPossible) + (if (!knownRemaining) "+" else "")).withStyle(ChatFormatting.WHITE))
        graphics.text(font, bestLabel, x + PADDING, y + 1, 0xFFFFFF.opaqueColor())

        val paceColor = if (!hasDelta) ChatFormatting.GRAY else if (cumulativeDelta > 0) ChatFormatting.RED else ChatFormatting.GREEN
        val paceValue = if (!hasDelta) "--" else {
            val sign = if (cumulativeDelta > 0) "+" else ""
            "$sign${String.format("%.2f", cumulativeDelta)}s"
        }
        val paceComponent = Component.literal(paceValue).withStyle(paceColor)
        val paceWidth = font.width(paceComponent)
        graphics.text(font, paceComponent, x + width - paceWidth - PADDING, y + 1, 0xFFFFFF.opaqueColor())
    }

    private fun formatTime(seconds: Double): String {
        val clamped = seconds.coerceAtLeast(0.0)
        val minutes = (clamped / 60).toInt()
        val remaining = clamped - minutes * 60
        return if (minutes > 0) String.format("%d:%05.2f", minutes, remaining) else String.format("%.2f", remaining)
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) = Unit
}

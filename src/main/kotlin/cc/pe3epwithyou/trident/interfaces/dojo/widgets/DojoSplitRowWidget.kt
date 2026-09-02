package cc.pe3epwithyou.trident.interfaces.dojo.widgets

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitManager
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitTimer
import cc.pe3epwithyou.trident.utils.extensions.GraphicsExtensions.fillRoundedAll
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.util.opaqueColor
import com.noxcrew.sheeplib.util.opacity
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/**
 * One row of the Dojo split list. Draws itself fresh every frame (rather than baking text at
 * construction time) so the active row's time can tick live without needing a dialog relayout.
 */
class DojoSplitRowWidget(
    private val levelUid: String,
    private val fallbackName: String,
    width: Int
) : AbstractWidget(0, 0, width, HEIGHT, Component.empty()) {
    companion object {
        const val HEIGHT = 10
        private const val PADDING = 3
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val font = minecraft().font
        val timer = DojoSplitTimer.instance
        val completedRow = timer?.completedSplits?.firstOrNull { it.levelUid == levelUid }
        val isActive = timer != null && !timer.isBetween && timer.currentLevelUid == levelUid

        if (isActive) {
            graphics.fillRoundedAll(x, y, width, HEIGHT, 0xFFFFFF opacity 24)
        }

        val name: String
        val time: Double?
        val delta: Double?
        val live: Boolean
        val reached: Boolean

        when {
            completedRow != null -> {
                name = completedRow.levelName
                time = completedRow.timeSeconds
                delta = completedRow.deltaSeconds
                live = false
                reached = true
            }

            isActive -> {
                name = timer!!.levelName
                time = timer.currentSplitTimeSeconds()
                delta = timer.splitImprovement()
                    ?.takeIf { Config.Dojo.showSplitImprovements && it >= Config.Dojo.showTimerImprovementAt }
                live = true
                reached = true
            }

            else -> {
                val course = timer?.courseName ?: DojoSplitManager.lastCourseName
                name = fallbackName
                time = course?.let { DojoSplitManager.getSplitSeconds(it, levelUid) }
                delta = null
                live = false
                reached = false
            }
        }

        val nameColor = when {
            live -> ChatFormatting.WHITE
            reached -> ChatFormatting.GRAY
            else -> ChatFormatting.DARK_GRAY
        }
        val nameComponent = Component.literal(name.uppercase()).withStyle(nameColor)
        graphics.text(font, nameComponent, x + PADDING, y + 1, 0xFFFFFF.opaqueColor())

        val timeText = if (time != null) String.format("%.3f", time) else "--.---"
        val timeColor = if (reached) ChatFormatting.WHITE else ChatFormatting.DARK_GRAY
        val timeComponent = Component.literal(timeText).withStyle(timeColor)
        val timeWidth = font.width(timeComponent)
        graphics.text(font, timeComponent, x + width - timeWidth - PADDING, y + 1, 0xFFFFFF.opaqueColor())

        if (delta == null) return
        val deltaColor = if (delta > 0) ChatFormatting.RED else ChatFormatting.GREEN
        val sign = if (delta > 0) "+" else ""
        val deltaComponent = Component.literal("$sign${String.format("%.2f", delta)}").withStyle(deltaColor)
        val deltaWidth = font.width(deltaComponent)
        graphics.text(font, deltaComponent, x + width - timeWidth - deltaWidth - PADDING - 4, y + 1, 0xFFFFFF.opaqueColor())
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) = Unit
}

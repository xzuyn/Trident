package cc.pe3epwithyou.trident.feature.dojo

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.state.MCCIState
import cc.pe3epwithyou.trident.utils.Resources
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.util.opaqueColor
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.FontDescription
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

/**
 * Renders the live split timer bar under the boss bars while a Dojo run is active.
 * Ported from IslandUtils' `DojoSplitUI`.
 */
object DojoSplitBar {
    private val BAR_TEXTURE: Identifier = Resources.trident("dojo/pkw_splits")
    private val HUD_STYLE: Style = Style.EMPTY.withFont(FontDescription.Resource(Resources.mcc("hud")))
    private const val WIDTH = 130
    private const val HEIGHT = 12
    private const val LEVEL_NAME_WIDTH = 25

    @JvmStatic
    fun render(graphics: GuiGraphicsExtractor, bossBars: Int) {
        if (!MCCIState.isOnIsland()) return
        if (!Config.Dojo.enabled || !Config.Dojo.showTimer) return
        val timer = DojoSplitTimer.instance ?: return

        val font = minecraft().font
        val x = Math.round((graphics.guiWidth() / 2.0f) - (WIDTH / 2.0f) - 1.0f)
        val y = (bossBars * 18.5).toInt() + 1

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_TEXTURE, x, y, WIDTH, HEIGHT)

        renderLevelName(graphics, font, timer, x, y)
        val timerWidth = renderSplitTime(graphics, font, timer, x, y)
        if (Config.Dojo.showSplitImprovements) {
            renderSplitImprovement(graphics, font, timer, x, y, timerWidth)
        }
    }

    private fun renderLevelName(graphics: GuiGraphicsExtractor, font: Font, timer: DojoSplitTimer, x: Int, y: Int) {
        val levelName = Component.literal(timer.levelName).withStyle(HUD_STYLE)
        val offset = (LEVEL_NAME_WIDTH / 2) - (font.width(levelName) / 2)
        graphics.text(font, levelName, x + offset + 1, y + 2, 0xFFFFFF.opaqueColor())
    }

    private fun renderSplitTime(graphics: GuiGraphicsExtractor, font: Font, timer: DojoSplitTimer, x: Int, y: Int): Int {
        val splitTime = Component.literal(String.format("%.3f", timer.currentSplitTimeSeconds()))
        val width = font.width(splitTime)
        graphics.text(font, splitTime, x + WIDTH - width - 2, y + 2, 0xFFFFFF.opaqueColor())
        return width
    }

    private fun renderSplitImprovement(
        graphics: GuiGraphicsExtractor, font: Font, timer: DojoSplitTimer, x: Int, y: Int, timerWidth: Int
    ) {
        if (timer.isBetween) return
        val improvement = timer.splitImprovement() ?: return
        if (improvement < Config.Dojo.showTimerImprovementAt) return

        var formatted = String.format("%.2fs", improvement)
        var color = ChatFormatting.GREEN
        if (improvement > 0) {
            color = ChatFormatting.RED
            formatted = "+$formatted"
        }

        val improvementTime = Component.literal(formatted).withStyle(color)
        val tx = x + WIDTH - timerWidth - 2 - font.width(improvementTime) - 8
        graphics.text(font, improvementTime, tx, y + 2, 0xFFFFFF.opaqueColor())
    }
}

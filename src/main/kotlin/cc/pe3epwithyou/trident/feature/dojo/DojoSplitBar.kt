package cc.pe3epwithyou.trident.feature.dojo

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.state.MCCIState
import cc.pe3epwithyou.trident.utils.Resources
import cc.pe3epwithyou.trident.utils.extensions.GraphicsExtensions.fillRoundedAll
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.util.opaqueColor
import com.noxcrew.sheeplib.util.opacity
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style

/**
 * Renders a LiveSplit-style box under the boss bars while a Dojo run is active: every level
 * completed so far this run gets its own row (name, split time, colored delta), with the
 * level currently being run shown as a live-ticking final row.
 */
object DojoSplitBar {
    private val HUD_STYLE: Style = Style.EMPTY.withFont(FontDescription.Resource(Resources.mcc("hud")))
    private const val WIDTH = 130
    private const val ROW_HEIGHT = 10
    private const val HEADER_HEIGHT = 12
    private const val PADDING = 2

    /** Only the most recent rows are shown, like LiveSplit's scrolling split list. */
    private const val MAX_VISIBLE_COMPLETED_ROWS = 5

    private const val BACKGROUND_COLOR = 0x000000
    private const val BACKGROUND_OPACITY = 128
    private const val CURRENT_ROW_OPACITY = 64

    @JvmStatic
    fun render(graphics: GuiGraphicsExtractor, bossBars: Int) {
        if (!MCCIState.isOnIsland()) return
        if (!Config.Dojo.enabled || !Config.Dojo.showTimer) return
        val timer = DojoSplitTimer.instance ?: return

        val font = minecraft().font
        val showCurrentRow = !timer.isBetween
        val completed = timer.completedSplits.takeLast(
            if (showCurrentRow) MAX_VISIBLE_COMPLETED_ROWS - 1 else MAX_VISIBLE_COMPLETED_ROWS
        )
        val rowCount = completed.size + if (showCurrentRow) 1 else 0
        if (rowCount == 0) return

        val height = HEADER_HEIGHT + rowCount * ROW_HEIGHT + PADDING
        val x = Math.round((graphics.guiWidth() / 2.0f) - (WIDTH / 2.0f))
        val y = (bossBars * 18.5).toInt() + 1

        graphics.fillRoundedAll(x, y, WIDTH, height, BACKGROUND_COLOR opacity BACKGROUND_OPACITY)

        val title = Component.literal("SPLITS").withStyle(HUD_STYLE).withStyle(ChatFormatting.GRAY)
        graphics.text(font, title, x + PADDING + 1, y + 2, 0xFFFFFF.opaqueColor())

        var rowY = y + HEADER_HEIGHT
        completed.forEach { row ->
            renderRow(graphics, font, x, rowY, row.levelName, row.timeSeconds, row.deltaSeconds, live = false)
            rowY += ROW_HEIGHT
        }

        if (showCurrentRow) {
            graphics.fillRoundedAll(x, rowY, WIDTH, ROW_HEIGHT, BACKGROUND_COLOR opacity CURRENT_ROW_OPACITY)
            val delta = timer.splitImprovement()
                ?.takeIf { Config.Dojo.showSplitImprovements && it >= Config.Dojo.showTimerImprovementAt }
            renderRow(graphics, font, x, rowY, timer.levelName, timer.currentSplitTimeSeconds(), delta, live = true)
        }
    }

    private fun renderRow(
        graphics: GuiGraphicsExtractor,
        font: Font,
        x: Int,
        y: Int,
        levelName: String,
        timeSeconds: Double,
        deltaSeconds: Double?,
        live: Boolean
    ) {
        val nameColor = if (live) ChatFormatting.WHITE else ChatFormatting.GRAY
        val name = Component.literal(levelName.uppercase()).withStyle(nameColor)
        graphics.text(font, name, x + PADDING + 1, y + 1, 0xFFFFFF.opaqueColor())

        val time = Component.literal(String.format("%.3f", timeSeconds)).withStyle(ChatFormatting.WHITE)
        val timeWidth = font.width(time)
        graphics.text(font, time, x + WIDTH - timeWidth - PADDING - 1, y + 1, 0xFFFFFF.opaqueColor())

        if (deltaSeconds == null) return
        val color = if (deltaSeconds > 0) ChatFormatting.RED else ChatFormatting.GREEN
        val sign = if (deltaSeconds > 0) "+" else ""
        val delta = Component.literal("$sign${String.format("%.2f", deltaSeconds)}").withStyle(color)
        val deltaWidth = font.width(delta)
        graphics.text(font, delta, x + WIDTH - timeWidth - deltaWidth - PADDING - 5, y + 1, 0xFFFFFF.opaqueColor())
    }
}

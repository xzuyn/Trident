package cc.pe3epwithyou.trident.interfaces.dojo.widgets

import com.noxcrew.sheeplib.util.opacity
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/**
 * A thin horizontal rule marking the boundary between section groups (main path, each bonus
 * branch, ending) in the split list.
 */
class DojoSplitDividerWidget(width: Int) : AbstractWidget(0, 0, width, HEIGHT, Component.empty()) {
    companion object {
        const val HEIGHT = 5
        private const val MARGIN = 4
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val lineY = y + HEIGHT / 2
        graphics.fill(x + MARGIN, lineY, x + width - MARGIN, lineY + 1, 0xFFFFFF opacity 32)
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) = Unit
}

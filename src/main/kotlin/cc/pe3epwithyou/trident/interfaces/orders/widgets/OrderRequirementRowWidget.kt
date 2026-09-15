package cc.pe3epwithyou.trident.interfaces.orders.widgets

import cc.pe3epwithyou.trident.feature.orders.OrderFishData
import cc.pe3epwithyou.trident.state.OrderRequirement
import cc.pe3epwithyou.trident.utils.ProgressBar
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.defaultFont
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.util.opaqueColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/**
 * One fish requirement row within an Event Order.
 *
 * Draws the name, progress bar, and fraction as three independent [GuiGraphicsExtractor.text]
 * calls (rather than one [Component] tree built with `.append()`) for two reasons: it lets the
 * bar be positioned at an exact pixel column ([nameColumnWidth]) shared across every row in the
 * dialog regardless of how long each fish's name is, and it avoids Minecraft's style
 * inheritance — an appended child with no explicit color otherwise inherits its parent's
 * color, which was bleeding the fish name's rarity color into the "current/" text.
 */
class OrderRequirementRowWidget(
    private val requirement: OrderRequirement,
    private val nameColumnWidth: Int,
    private val showLocation: Boolean,
    width: Int
) : AbstractWidget(0, 0, width, HEIGHT, Component.empty()) {
    companion object {
        const val HEIGHT = 9
        const val PADDING = 4
        const val GAP = 4
        const val BAR_WIDTH = 10
        private const val COMPLETE_COLOR: Int = 0x80ff82
    }

    private fun nameText(): String {
        val location = OrderFishData.find(requirement.fishName)?.location?.displayName
        return if (showLocation && location != null) {
            "${requirement.fishName} ($location)"
        } else {
            requirement.fishName
        }
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val font = minecraft().font
        val done = requirement.current >= requirement.total
        val fishRarity = OrderFishData.getRarity(requirement.fishName)

        val nameComponent = Component.literal(nameText()).defaultFont().withColor(fishRarity.color)
        graphics.text(font, nameComponent, x + PADDING, y + 1, 0xFFFFFF.opaqueColor())

        val barX = x + PADDING + nameColumnWidth + GAP
        val fraction = (requirement.current.toFloat() / requirement.total.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
        val bar = ProgressBar.progressComponent(fraction, BAR_WIDTH, 0)
        graphics.text(font, bar, barX, y + 1, 0xFFFFFF.opaqueColor())

        val barWidth = font.width(bar)
        val fractionColor = if (done) COMPLETE_COLOR else 0xFFFFFF
        val fractionComponent = Component.literal(" ${requirement.current}/${requirement.total}")
            .defaultFont()
            .withColor(fractionColor)
        graphics.text(font, fractionComponent, barX + barWidth, y + 1, 0xFFFFFF.opaqueColor())
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) = Unit
}

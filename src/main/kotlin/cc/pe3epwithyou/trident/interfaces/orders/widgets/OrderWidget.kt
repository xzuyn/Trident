package cc.pe3epwithyou.trident.interfaces.orders.widgets

import cc.pe3epwithyou.trident.feature.orders.OrderFishData
import cc.pe3epwithyou.trident.state.Order
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.defaultFont
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.mccFont
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.CompoundWidget
import com.noxcrew.sheeplib.LayoutConstants
import com.noxcrew.sheeplib.layout.GridLayout
import com.noxcrew.sheeplib.theme.Themed
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.network.chat.Component

class OrderWidget(
    order: Order,
    themed: Themed
) : CompoundWidget(0, 0, 0, 0) {
    companion object {
        private const val COMPLETE_COLOR: Int = 0x80ff82
    }

    override fun getWidth(): Int = layout.width
    override fun getHeight(): Int = layout.height

    override val layout = GridLayout(themed.theme.dimensions.paddingInner) {
        val mcFont = minecraft().font
        val isComplete = order.isComplete

        val headerText = if (isComplete) "ORDER COMPLETE" else "${order.rarity.name} ORDER"
        val header = Component.literal(headerText.uppercase())
            .mccFont()
            .withColor(if (isComplete) COMPLETE_COLOR else order.rarity.color)
        if (isComplete) header.withStyle(ChatFormatting.ITALIC)

        StringWidget(header, mcFont).atBottom(0, settings = LayoutConstants.LEFT)

        order.requirements.forEach { requirement ->
            val done = requirement.current >= requirement.total
            val fishRarity = OrderFishData.getRarity(requirement.fishName)

            val name = Component.literal(requirement.fishName)
                .defaultFont()
                .withColor(if (done) COMPLETE_COLOR else fishRarity.color)

            val progress = Component.literal(" ${requirement.current}/")
                .defaultFont()
                .append(
                    Component.literal("${requirement.total}")
                        .defaultFont()
                        .withColor(if (done) COMPLETE_COLOR else 0xffffff)
                )

            StringWidget(name.append(progress), mcFont).atBottom(0, settings = LayoutConstants.LEFT)
        }
    }

    init {
        layout.arrangeElements()
        layout.visitWidgets(this::addChild)
    }
}

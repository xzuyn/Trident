package cc.pe3epwithyou.trident.interfaces.orders.widgets

import cc.pe3epwithyou.trident.feature.orders.OrderFishData
import cc.pe3epwithyou.trident.state.Order
import cc.pe3epwithyou.trident.state.FontCollection
import cc.pe3epwithyou.trident.utils.ProgressBar
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.defaultFont
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.mccFont
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.offset
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.CompoundWidget
import com.noxcrew.sheeplib.LayoutConstants
import com.noxcrew.sheeplib.layout.GridLayout
import com.noxcrew.sheeplib.theme.Themed
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.network.chat.Component

/**
 * Renders a single Event Order: a rarity-coloured header line followed by one line per
 * fish requirement. Reuses the same quest-log icon set as [cc.pe3epwithyou.trident.interfaces.questing.widgets.QuestWidget]
 * for the header icon, since those are the only per-rarity inline sprites known to render correctly.
 */
class OrderWidget(
    order: Order,
    themed: Themed
) : CompoundWidget(0, 0, 0, 0) {
    companion object {
        private const val COMPLETE_COLOR: Int = 0x80ff82
        private const val QUEST_ICON_DIRECTORY = "island_interface/quest_log/daily/"
        private const val COMPLETE_ICON = "island_interface/generic/accept"
        private const val BAR_WIDTH = 10
    }

    override fun getWidth(): Int = layout.width
    override fun getHeight(): Int = layout.height

    override val layout = GridLayout(themed.theme.dimensions.paddingInner) {
        val mcFont = minecraft().font
        val isComplete = order.isComplete

        val iconPath = if (isComplete) COMPLETE_ICON else "$QUEST_ICON_DIRECTORY${order.rarity.name.lowercase()}"
        val icon = FontCollection.texture(iconPath).offset(y = 1f)

        val headerText = if (isComplete) "ORDER COMPLETE" else "${order.rarity.name} ORDER"
        val header = Component.literal(headerText.uppercase())
            .mccFont()
            .withColor(if (isComplete) COMPLETE_COLOR else order.rarity.color)
        if (isComplete) header.withStyle(ChatFormatting.ITALIC)

        val headerLine = icon.append(Component.literal(" ")).append(header)
        StringWidget(headerLine, mcFont).atBottom(0, settings = LayoutConstants.LEFT)

        order.requirements.forEach { requirement ->
            val done = requirement.current >= requirement.total
            val fishRarity = OrderFishData.getRarity(requirement.fishName)
            val fraction = (requirement.current.toFloat() / requirement.total.coerceAtLeast(1).toFloat())
                .coerceIn(0f, 1f)

            val name = Component.literal(" ${requirement.fishName} ")
                .defaultFont()
                .withColor(if (done) COMPLETE_COLOR else fishRarity.color)

            val bar = ProgressBar.progressComponent(fraction, BAR_WIDTH, 0)

            val progress = Component.literal(" ${requirement.current}/")
                .defaultFont()
                .append(
                    Component.literal("${requirement.total}")
                        .defaultFont()
                        .withColor(if (done) COMPLETE_COLOR else 0xffffff)
                )

            StringWidget(name.append(bar).append(progress), mcFont).atBottom(0, settings = LayoutConstants.LEFT)
        }
    }

    init {
        layout.arrangeElements()
        layout.visitWidgets(this::addChild)
    }
}

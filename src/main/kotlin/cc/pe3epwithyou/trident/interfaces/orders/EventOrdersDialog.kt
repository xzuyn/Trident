package cc.pe3epwithyou.trident.interfaces.orders

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.interfaces.orders.widgets.OrderDividerWidget
import cc.pe3epwithyou.trident.interfaces.orders.widgets.OrderRequirementRowWidget
import cc.pe3epwithyou.trident.interfaces.shared.TridentDialog
import cc.pe3epwithyou.trident.interfaces.themes.DialogTitle
import cc.pe3epwithyou.trident.interfaces.themes.TridentThemed
import cc.pe3epwithyou.trident.feature.orders.OrderFishData
import cc.pe3epwithyou.trident.feature.orders.OrderStorage
import cc.pe3epwithyou.trident.state.FontCollection
import cc.pe3epwithyou.trident.state.Order
import cc.pe3epwithyou.trident.utils.ProgressBar
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.defaultFont
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.mccFont
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.offset
import cc.pe3epwithyou.trident.utils.minecraft
import cc.pe3epwithyou.trident.utils.playerState
import com.noxcrew.sheeplib.LayoutConstants
import com.noxcrew.sheeplib.dialog.title.DialogTitleWidget
import com.noxcrew.sheeplib.layout.grid
import com.noxcrew.sheeplib.theme.Themed
import com.noxcrew.sheeplib.util.opacity
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.MultiLineTextWidget
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.network.chat.Component

class EventOrdersDialog(x: Int, y: Int, key: String) : TridentDialog(x, y, key),
    Themed by TridentThemed {
    private companion object {
        private val TITLE_COLOR: Int = 0xe9a825 opacity 127
        private const val COMPLETE_COLOR: Int = 0x80ff82
        private const val SUGGESTED_COLOR: Int = 0xffd479
        private const val QUEST_ICON_DIRECTORY = "island_interface/quest_log/daily/"
        private const val COMPLETE_ICON = "island_interface/generic/accept"
        private const val MIN_ROW_WIDTH = 120
    }

    private fun getWidgetTitle(): DialogTitleWidget {
        val icon = FontCollection.get("_fonts/icon/quest_log.png").withoutShadow()
        val text = Component.literal(" EVENT ORDERS".uppercase()).mccFont().offset(y = -0.5f)

        val baseTitle = icon.append(text)

        return DialogTitle(this, baseTitle, TITLE_COLOR)
    }

    override var title = getWidgetTitle()

    private fun requirementNameText(fishName: String, showLocation: Boolean): String {
        val location = OrderFishData.find(fishName)?.location?.displayName
        return if (showLocation && location != null) "$fishName ($location)" else fishName
    }

    private fun buildHeader(order: Order): Component {
        val isComplete = order.isComplete
        val iconPath = if (isComplete) COMPLETE_ICON else "$QUEST_ICON_DIRECTORY${order.rarity.name.lowercase()}"
        val icon = FontCollection.texture(iconPath).offset(y = 1f)

        val headerText = if (isComplete) "ORDER COMPLETE" else "${order.rarity.name} ORDER"
        val header = Component.literal(headerText.uppercase())
            .mccFont()
            .withColor(if (isComplete) COMPLETE_COLOR else order.rarity.color)
        if (isComplete) header.withStyle(ChatFormatting.ITALIC)

        return icon.append(Component.literal(" ")).append(header)
    }

    override fun layout(): GridLayout = grid {
        val mcFont = minecraft().font
        val eventOrders = playerState().eventOrders
        val showLocation = Config.Fishing.eventOrdersShowLocation

        if (eventOrders.needsUpdating || eventOrders.orders.isEmpty()) {
            StringWidget(
                Component.literal("No orders found".uppercase()).mccFont()
                    .withStyle(ChatFormatting.GRAY), mcFont
            ).atBottom(0, settings = LayoutConstants.CENTRE)
            MultiLineTextWidget(
                Component.literal(
                    """
                    In order to update
                    the Event Orders Module,
                    please open the following
                    menu: Island Rewards ->
                    Event Orders
                """.trimIndent()
                ).defaultFont().withStyle(ChatFormatting.GRAY), mcFont
            ).atBottom(0, settings = LayoutConstants.LEFT)
            return@grid
        }

        val orders = eventOrders.orders
        val allRequirements = orders.flatMap { it.requirements }

        // Shared across every row in the whole dialog (not just within one order) so every
        // progress bar lines up at the same horizontal position.
        val nameColumnWidth = allRequirements.maxOfOrNull { req ->
            mcFont.width(requirementNameText(req.fishName, showLocation))
        } ?: 0

        val maxFractionWidth = allRequirements.maxOfOrNull { req ->
            mcFont.width(" ${req.total}/${req.total}")
        } ?: 0

        val barWidth = mcFont.width(
            ProgressBar.progressComponent(1f, OrderRequirementRowWidget.BAR_WIDTH, 0)
        )

        val rowWidth = (
            OrderRequirementRowWidget.PADDING * 2 +
                nameColumnWidth +
                OrderRequirementRowWidget.GAP +
                barWidth +
                maxFractionWidth
            ).coerceAtLeast(MIN_ROW_WIDTH)

        OrderStorage.suggestedLocation()?.let { location ->
            StringWidget(
                Component.literal("Suggested: ${location.displayName}")
                    .mccFont()
                    .withColor(SUGGESTED_COLOR),
                mcFont
            ).atBottom(0, settings = LayoutConstants.LEFT)
            OrderDividerWidget(rowWidth).atBottom(0, settings = LayoutConstants.LEFT)
        }

        orders.forEachIndexed { index, order ->
            StringWidget(buildHeader(order), mcFont).atBottom(0, settings = LayoutConstants.LEFT)

            order.requirements.forEach { requirement ->
                OrderRequirementRowWidget(requirement, nameColumnWidth, showLocation, rowWidth)
                    .atBottom(0, settings = LayoutConstants.LEFT)
            }

            if (index != orders.lastIndex) {
                OrderDividerWidget(rowWidth).atBottom(0, settings = LayoutConstants.LEFT)
            }
        }
    }

    override fun refresh() {
        title = getWidgetTitle()
        super.refresh()
    }
}

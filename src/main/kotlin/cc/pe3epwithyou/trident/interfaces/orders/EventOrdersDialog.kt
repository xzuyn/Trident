package cc.pe3epwithyou.trident.interfaces.orders

import cc.pe3epwithyou.trident.interfaces.orders.widgets.OrderWidget
import cc.pe3epwithyou.trident.interfaces.shared.TridentDialog
import cc.pe3epwithyou.trident.interfaces.themes.DialogTitle
import cc.pe3epwithyou.trident.interfaces.themes.TridentThemed
import cc.pe3epwithyou.trident.state.FontCollection
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
    }

    private fun getWidgetTitle(): DialogTitleWidget {
        val icon = FontCollection.get("_fonts/icon/quest_log.png").withoutShadow()
        val text = Component.literal(" EVENT ORDERS".uppercase()).mccFont().offset(y = -0.5f)

        val baseTitle = icon.append(text)

        return DialogTitle(this, baseTitle, TITLE_COLOR)
    }

    override var title = getWidgetTitle()

    override fun layout(): GridLayout = grid {
        val mcFont = minecraft().font
        val eventOrders = playerState().eventOrders

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

        val orders = eventOrders.orders.sortedBy { it.isComplete }
        orders.forEachIndexed { index, order ->
            OrderWidget(order, this@EventOrdersDialog).atBottom(0, settings = LayoutConstants.LEFT)
            if (index != orders.lastIndex) {
                StringWidget(Component.empty(), mcFont).atBottom(0, settings = LayoutConstants.LEFT)
            }
        }
    }

    override fun refresh() {
        title = getWidgetTitle()
        super.refresh()
    }
}

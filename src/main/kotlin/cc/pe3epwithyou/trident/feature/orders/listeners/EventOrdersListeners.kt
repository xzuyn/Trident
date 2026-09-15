package cc.pe3epwithyou.trident.feature.orders.listeners

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.events.container.ContainerContext
import cc.pe3epwithyou.trident.events.container.ContainerEvents
import cc.pe3epwithyou.trident.feature.orders.OrderParser
import cc.pe3epwithyou.trident.feature.orders.OrderStorage
import cc.pe3epwithyou.trident.state.MCCIState
import cc.pe3epwithyou.trident.state.Order
import cc.pe3epwithyou.trident.utils.Logger

object EventOrdersListeners {
    private const val MAX_ORDERS = 3

    fun register() {
        ContainerEvents.onOpen(::find)
        ContainerEvents.onClose(::find)
    }

    fun find(ctx: ContainerContext) = with(ctx) {
        requireTitle("EVENT ORDERS")
        if (!Config.Fishing.eventOrdersModule) return@with
        if (!MCCIState.isOnSeaMonstersIsland()) return@with

        val orders = mutableListOf<Order>()
        handledScreen.menu.slots.forEach { slot ->
            if (orders.size >= MAX_ORDERS) return@forEach
            val order = OrderParser.parse(slot.item, slot.index) ?: return@forEach
            orders.add(order)
        }

        Logger.debugLog("Parsed ${orders.size} event orders")
        OrderStorage.loadOrders(orders)
    }
}

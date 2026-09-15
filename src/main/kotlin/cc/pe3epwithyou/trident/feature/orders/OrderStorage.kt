package cc.pe3epwithyou.trident.feature.orders

import cc.pe3epwithyou.trident.interfaces.DialogCollection
import cc.pe3epwithyou.trident.state.Order
import cc.pe3epwithyou.trident.utils.playerState

object OrderStorage {
    const val DIALOG_KEY = "event_orders"

    fun loadOrders(orders: List<Order>) {
        playerState().eventOrders.apply {
            this.orders = orders.toMutableList()
            needsUpdating = false
        }
        DialogCollection.refreshDialog(DIALOG_KEY)
    }

    /**
     * Applies a caught fish towards any active order requirements matching [fishName].
     * Returns true if any requirement's progress was updated.
     */
    fun applyCatch(fishName: String): Boolean {
        val orders = playerState().eventOrders.orders
        if (orders.isEmpty()) return false

        var updated = false
        orders.forEach { order ->
            order.requirements.forEach { requirement ->
                if (requirement.current < requirement.total &&
                    requirement.fishName.equals(fishName, ignoreCase = true)
                ) {
                    requirement.current = (requirement.current + 1).coerceAtMost(requirement.total)
                    updated = true
                }
            }
        }

        if (updated) DialogCollection.refreshDialog(DIALOG_KEY)
        return updated
    }
}

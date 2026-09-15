package cc.pe3epwithyou.trident.feature.orders

import cc.pe3epwithyou.trident.interfaces.DialogCollection
import cc.pe3epwithyou.trident.state.Order
import cc.pe3epwithyou.trident.utils.Logger
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
     * Applies a caught fish towards any active order requirements matching [fishName],
     * incrementing by [quantity] (e.g. > 1 when a fishing magnet-style perk catches multiple
     * fish at once). Returns true if any requirement's progress was updated.
     */
    fun applyCatch(fishName: String, quantity: Int = 1): Boolean {
        val orders = playerState().eventOrders.orders
        if (orders.isEmpty()) return false

        var updated = false
        orders.forEach { order ->
            order.requirements.forEach { requirement ->
                if (requirement.current < requirement.total &&
                    requirement.fishName.equals(fishName, ignoreCase = true)
                ) {
                    val newCurrent = (requirement.current + quantity).coerceAtMost(requirement.total)
                    if (newCurrent != requirement.current) {
                        requirement.current = newCurrent
                        updated = true
                    }
                }
            }
        }

        Logger.debugLog("Applied event order catch: $fishName x$quantity (updated=$updated)")
        if (updated) DialogCollection.refreshDialog(DIALOG_KEY)
        return updated
    }
}

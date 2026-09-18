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
                if (requirement.fishName.equals(fishName, ignoreCase = true)) {
                    requirement.current += quantity
                    updated = true
                }
            }
        }

        Logger.debugLog("Applied event order catch: $fishName x$quantity (updated=$updated)")
        if (updated) DialogCollection.refreshDialog(DIALOG_KEY)
        return updated
    }

    /**
     * The [FishingLocation] with the most fish still needed across every incomplete
     * requirement in the active orders - i.e. the island that would clear the most progress.
     * Null if there are no orders loaded, or every requirement is already fulfilled.
     */
    fun suggestedLocation(): FishingLocation? {
        return playerState().eventOrders.orders
            .flatMap { it.requirements }
            .filter { it.current < it.total }
            .mapNotNull { req -> OrderFishData.find(req.fishName)?.location?.let { it to (req.total - req.current) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, needed) -> needed.sum() }
            .maxByOrNull { it.value }
            ?.key
    }
}

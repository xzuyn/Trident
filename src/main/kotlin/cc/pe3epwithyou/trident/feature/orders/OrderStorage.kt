package cc.pe3epwithyou.trident.feature.orders

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.interfaces.DialogCollection
import cc.pe3epwithyou.trident.state.Order
import cc.pe3epwithyou.trident.state.Rarity
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
     * Applies a caught fish towards the first active order requirement matching [fishName]
     * that still needs it (i.e. `current < total`), incrementing it by [quantity] (e.g. > 1
     * when a fishing magnet-style perk catches multiple fish at once). Orders are matched in
     * their display order, so a completed requirement in an earlier order is skipped in favor
     * of the same fish's requirement in the next order that still needs it. Returns true if a
     * requirement's progress was updated.
     */
    fun applyCatch(fishName: String, quantity: Int = 1): Boolean {
        val orders = playerState().eventOrders.orders
        if (orders.isEmpty()) return false

        val requirement = orders.firstNotNullOfOrNull { order ->
            order.requirements.firstOrNull {
                it.fishName.equals(fishName, ignoreCase = true) && it.current < it.total
            }
        }
        val updated = requirement != null
        requirement?.let { it.current += quantity }

        Logger.debugLog("Applied event order catch: $fishName x$quantity (updated=$updated)")
        if (updated) DialogCollection.refreshDialog(DIALOG_KEY)
        return updated
    }

    /**
     * The suggested [FishingLocation], per [cc.pe3epwithyou.trident.config.Config.Fishing.eventOrdersSuggestionMode]:
     * - [OrderSuggestionMode.MOST_NEEDED]: the location with the most fish still needed across
     *   every incomplete requirement (falls back to nothing further, this is the base mode).
     * - [OrderSuggestionMode.SOONEST_COMPLETION]: the location that would fully finish an order
     *   in the fewest catches, considering only orders whose remaining requirements are all
     *   from a single location (an order split across locations can't be finished by visiting
     *   just one). Falls back to [OrderSuggestionMode.MOST_NEEDED] if no such order exists.
     *
     * Both modes break ties by preferring the higher-[Rarity] order, then whichever order
     * displays first. Null if there are no orders loaded, or every requirement is fulfilled.
     */
    fun suggestedLocation(): FishingLocation? {
        val orders = playerState().eventOrders.orders
        return when (Config.Fishing.eventOrdersSuggestionMode) {
            OrderSuggestionMode.MOST_NEEDED -> suggestedByMostNeeded(orders)
            OrderSuggestionMode.SOONEST_COMPLETION -> suggestedBySoonestCompletion(orders) ?: suggestedByMostNeeded(orders)
        }
    }

    private data class LocationTally(val location: FishingLocation, val amount: Int, val rarity: Rarity, val orderIndex: Int)

    private fun suggestedByMostNeeded(orders: List<Order>): FishingLocation? {
        val contributions = orders.withIndex().flatMap { (index, order) ->
            order.requirements
                .filter { it.current < it.total }
                .mapNotNull { req ->
                    OrderFishData.find(req.fishName)?.location?.let { location ->
                        LocationTally(location, req.total - req.current, order.rarity, index)
                    }
                }
        }

        return contributions.groupBy { it.location }
            .map { (location, items) ->
                val total = items.sumOf { it.amount }
                // Representative order for tie-breaking: whichever contributing order has the
                // highest rarity, then whichever of those displays first.
                val representative = items.minWith(
                    compareByDescending<LocationTally> { it.rarity.ordinal }.thenBy { it.orderIndex }
                )
                LocationTally(location, total, representative.rarity, representative.orderIndex)
            }
            .minWithOrNull(
                compareByDescending<LocationTally> { it.amount }
                    .thenByDescending { it.rarity.ordinal }
                    .thenBy { it.orderIndex }
            )
            ?.location
    }

    private fun suggestedBySoonestCompletion(orders: List<Order>): FishingLocation? {
        return orders.withIndex().mapNotNull { (index, order) ->
            val incomplete = order.requirements.filter { it.current < it.total }
            if (incomplete.isEmpty()) return@mapNotNull null

            val locations = incomplete.mapNotNull { OrderFishData.find(it.fishName)?.location }.toSet()
            if (locations.size != 1) return@mapNotNull null

            LocationTally(locations.first(), incomplete.sumOf { it.total - it.current }, order.rarity, index)
        }.minWithOrNull(
            compareBy<LocationTally> { it.amount }
                .thenByDescending { it.rarity.ordinal }
                .thenBy { it.orderIndex }
        )?.location
    }
}
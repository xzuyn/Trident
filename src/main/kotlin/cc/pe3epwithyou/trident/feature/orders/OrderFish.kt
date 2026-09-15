package cc.pe3epwithyou.trident.feature.orders

import cc.pe3epwithyou.trident.state.Rarity

enum class FishingLocation(
    val displayName: String
) {
    FROSTED_FOREST("Frosted Forest"),
    GLACIAL_SETTLEMENT("Glacial Settlement"),
    CRYSTAL_CLIFFS("Crystal Cliffs"),
    ARCTIC_CAVERNS("Arctic Caverns"),
    BAMBOO_HILLS("Bamboo Hills"),
    MAGMATIC_SPRINGS("Magmatic Springs"),
    BLOOMING_SWAMP("Blooming Swamp"),
    BASIC_BEACH("Basic Beach");
}

data class OrderFish(
    val name: String,
    val location: FishingLocation,
    val rarity: Rarity,
)

/**
 * Static catalog of every fish species obtainable during the Sea Monsters Event, used to
 * look up the location and rarity of a fish referenced in an Event Order.
 */
object OrderFishData {
    val fish: List<OrderFish> = listOf(
        OrderFish("Fern Flounder", FishingLocation.FROSTED_FOREST, Rarity.COMMON),
        OrderFish("Coral Cod", FishingLocation.FROSTED_FOREST, Rarity.UNCOMMON),
        OrderFish("Glass Pike", FishingLocation.FROSTED_FOREST, Rarity.RARE),
        OrderFish("Mosaic Guppy", FishingLocation.FROSTED_FOREST, Rarity.EPIC),
        OrderFish("Queenfish", FishingLocation.FROSTED_FOREST, Rarity.LEGENDARY),
        OrderFish("Mirrored Mahi", FishingLocation.FROSTED_FOREST, Rarity.MYTHIC),

        OrderFish("Bluegill", FishingLocation.GLACIAL_SETTLEMENT, Rarity.COMMON),
        OrderFish("Midnight Gourami", FishingLocation.GLACIAL_SETTLEMENT, Rarity.UNCOMMON),
        OrderFish("Painted Discus", FishingLocation.GLACIAL_SETTLEMENT, Rarity.RARE),
        OrderFish("Pearlescent Betta", FishingLocation.GLACIAL_SETTLEMENT, Rarity.EPIC),
        OrderFish("Disco Discus", FishingLocation.GLACIAL_SETTLEMENT, Rarity.LEGENDARY),
        OrderFish("Wreckfish", FishingLocation.GLACIAL_SETTLEMENT, Rarity.MYTHIC),

        OrderFish("Reef Anchovy", FishingLocation.CRYSTAL_CLIFFS, Rarity.COMMON),
        OrderFish("Crystalline Cod", FishingLocation.CRYSTAL_CLIFFS, Rarity.UNCOMMON),
        OrderFish("Shardine", FishingLocation.CRYSTAL_CLIFFS, Rarity.RARE),
        OrderFish("Ocean Moonfish", FishingLocation.CRYSTAL_CLIFFS, Rarity.EPIC),
        OrderFish("Nightmare Marlin", FishingLocation.CRYSTAL_CLIFFS, Rarity.LEGENDARY),
        OrderFish("Floodfish", FishingLocation.CRYSTAL_CLIFFS, Rarity.MYTHIC),

        OrderFish("Silver Snook", FishingLocation.ARCTIC_CAVERNS, Rarity.COMMON),
        OrderFish("Ancient Snapper", FishingLocation.ARCTIC_CAVERNS, Rarity.UNCOMMON),
        OrderFish("Sunken Koi", FishingLocation.ARCTIC_CAVERNS, Rarity.RARE),
        OrderFish("Blossom Betta", FishingLocation.ARCTIC_CAVERNS, Rarity.EPIC),
        OrderFish("Sapphire Salmon", FishingLocation.ARCTIC_CAVERNS, Rarity.LEGENDARY),
        OrderFish("Cosmic Cod", FishingLocation.ARCTIC_CAVERNS, Rarity.MYTHIC),

        OrderFish("Neon Tetra", FishingLocation.BAMBOO_HILLS, Rarity.COMMON),
        OrderFish("Sinharaja Salmon", FishingLocation.BAMBOO_HILLS, Rarity.UNCOMMON),
        OrderFish("Daintree Guppy", FishingLocation.BAMBOO_HILLS, Rarity.RARE),
        OrderFish("Viney Perch", FishingLocation.BAMBOO_HILLS, Rarity.EPIC),
        OrderFish("Green Terror Cichlid", FishingLocation.BAMBOO_HILLS, Rarity.LEGENDARY),
        OrderFish("Torch Tarpon", FishingLocation.BAMBOO_HILLS, Rarity.MYTHIC),

        OrderFish("Coal Cod", FishingLocation.MAGMATIC_SPRINGS, Rarity.COMMON),
        OrderFish("Emberpike", FishingLocation.MAGMATIC_SPRINGS, Rarity.UNCOMMON),
        OrderFish("Volcanic Surgeonfish", FishingLocation.MAGMATIC_SPRINGS, Rarity.RARE),
        OrderFish("Molten Goldfish", FishingLocation.MAGMATIC_SPRINGS, Rarity.EPIC),
        OrderFish("Ashen Tilapia", FishingLocation.MAGMATIC_SPRINGS, Rarity.LEGENDARY),
        OrderFish("Blazing Betta", FishingLocation.MAGMATIC_SPRINGS, Rarity.MYTHIC),

        OrderFish("Fungal Salmon", FishingLocation.BLOOMING_SWAMP, Rarity.COMMON),
        OrderFish("Frilled Mackerel", FishingLocation.BLOOMING_SWAMP, Rarity.UNCOMMON),
        OrderFish("Ancient Fangtooth", FishingLocation.BLOOMING_SWAMP, Rarity.RARE),
        OrderFish("Swampy Betta", FishingLocation.BLOOMING_SWAMP, Rarity.EPIC),
        OrderFish("Glowberry Guppy", FishingLocation.BLOOMING_SWAMP, Rarity.LEGENDARY),
        OrderFish("Troutarium", FishingLocation.BLOOMING_SWAMP, Rarity.MYTHIC),

        OrderFish("Coral Angelfish", FishingLocation.BASIC_BEACH, Rarity.COMMON),
        OrderFish("Butterfly Fish", FishingLocation.BASIC_BEACH, Rarity.UNCOMMON),
        OrderFish("Surgeonfish", FishingLocation.BASIC_BEACH, Rarity.RARE),
        OrderFish("Braincoral Betta", FishingLocation.BASIC_BEACH, Rarity.EPIC),
        OrderFish("Parrotfish", FishingLocation.BASIC_BEACH, Rarity.LEGENDARY),
        OrderFish("Diamond Oarfish", FishingLocation.BASIC_BEACH, Rarity.MYTHIC),
    )

    private val byName: Map<String, OrderFish> = fish.associateBy { it.name.lowercase() }

    fun find(name: String): OrderFish? = byName[name.trim().lowercase()]

    fun getRarity(name: String): Rarity = find(name)?.rarity ?: Rarity.COMMON
}

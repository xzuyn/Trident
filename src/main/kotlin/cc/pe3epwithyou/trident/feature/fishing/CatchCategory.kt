package cc.pe3epwithyou.trident.feature.fishing

/**
 * The kinds of loot the Fishing Record module counts. Item name lists were ported from
 * Compact Fishing Message.
 */
enum class CatchCategory {
    JUNK, NORMAL_FISH, ELUSIVE_FISH, PEARL, TREASURE, SPIRIT;

    companion object {
        private val JUNK_ITEMS = setOf(
            "Rusted Can", "Tangled Kelp", "Lost Shoe", "Royal Residue", "Forgotten Crown"
        )

        private val PEARL_ITEMS = setOf(
            "Rough Pearl", "Polished Pearl", "Pristine Pearl"
        )

        private val TREASURE_ITEMS = setOf(
            "Common Treasure", "Uncommon Treasure", "Rare Treasure", "Epic Treasure", "Legendary Treasure",
            "Mythic Treasure"
        )

        private val SPIRIT_ITEMS = setOf(
            "Strong Spirit", "Refined Strong Spirit", "Pure Strong Spirit", "Wise Spirit",
            "Refined Wise Spirit", "Pure Wise Spirit", "Glimmering Spirit", "Refined Glimmering Spirit",
            "Pure Glimmering Spirit", "Greedy Spirit", "Refined Greedy Spirit", "Pure Greedy Spirit",
            "Lucky Spirit", "Refined Lucky Spirit", "Pure Lucky Spirit"
        )

        /** Elusive fish across all twelve fishing islands. */
        private val ELUSIVE_FISH_ITEMS = setOf(
            "Ancient Trevally", "Clown Triggerfish", "Desert Mackerel", "Royal Gramma", "Sandy Salmon",
            "Wreckfish", "Ashen Gourami", "Boiling Boxfish", "Driftfish Raider", "Emberpike",
            "Volcanic Butterflyfish", "Volcanic Goldfish", "Barrockuda", "Burnt Basa", "Canyon Perch",
            "Congo Tetra", "Golden Trout", "Phoenix Angelfish", "Coral Angelfish", "Coral Beauty",
            "Gorgonian Grouper", "Mahi Mahi", "Pearlescent Gourami", "Staghorn Flounder",
            "Emperor Angelfish", "Fungal Salmon", "Queenfish", "Snaggletooth Gar", "Troutarium",
            "Agaric Tarpon", "Clover Koi", "Lavender Leatherneck", "Napoleonfish", "Poppy Clownfish",
            "Rainbow Trout", "Tulapia", "Coral Cod", "Cosmic Cod", "Glass Pike", "Pearlescent Betta",
            "Rose Goldfish", "Treasured Tilapia", "Gloopy Gar", "Glowberry Guppy", "Lemon Sole",
            "Mutated Marlin", "Overgrown Grouper", "Shardine", "Binding Jellyfish", "Butterfly Fish",
            "Emerald Jewelfish", "Jungle Clownfish", "Jungle Fangtooth", "Zebra Turkeyfish", "Alligator Gar",
            "Goliath Tigerfish", "Grave Gudgeon", "Green Terror Cichlid", "Mud Pike", "Mudfin Catfish",
            "Dorado", "Foxfish", "Mossy Mullet", "Stone Carp", "Stone Salmon", "Tarnished Goldfish",
            "Ashen Goliath", "Basalt Discus", "Flying Flashfish", "Heated Haddock", "Molten Goldfish",
            "Torch Tarpon"
        )

        /** Works out which category a caught item name (the text inside the `[...]` brackets) belongs to. */
        fun fromItemName(name: String): CatchCategory = when {
            name in JUNK_ITEMS -> JUNK
            name in PEARL_ITEMS -> PEARL
            name in TREASURE_ITEMS -> TREASURE
            name in SPIRIT_ITEMS -> SPIRIT
            ELUSIVE_FISH_ITEMS.any { it in name } -> ELUSIVE_FISH
            else -> NORMAL_FISH
        }
    }
}

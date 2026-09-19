package cc.pe3epwithyou.trident.feature.orders

import dev.isxander.yacl3.api.NameableEnum
import net.minecraft.network.chat.Component

enum class OrderSuggestionMode : NameableEnum {
    MOST_NEEDED {
        override fun getDisplayName(): Component = Component.literal("Most Fish Needed")
    },
    SOONEST_COMPLETION {
        override fun getDisplayName(): Component = Component.literal("Soonest Order Completion")
    }
}

package cc.pe3epwithyou.trident.feature.dojo

import dev.isxander.yacl3.api.NameableEnum
import net.minecraft.network.chat.Component

enum class DojoSplitType : NameableEnum {
    BEST {
        override fun getDisplayName(): Component = Component.literal("Best time")
    },
    AVG {
        override fun getDisplayName(): Component = Component.literal("Average time")
    }
}

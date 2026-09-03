package cc.pe3epwithyou.trident.feature.dojo

import dev.isxander.yacl3.api.NameableEnum
import net.minecraft.network.chat.Component

enum class DojoEnding : NameableEnum {
    EASY {
        override fun getDisplayName(): Component = Component.literal("Easy (1 medal)")
    },
    MEDIUM {
        override fun getDisplayName(): Component = Component.literal("Medium (2 medals)")
    },
    HARD {
        override fun getDisplayName(): Component = Component.literal("Hard (3 medals)")
    }
}

package cc.pe3epwithyou.trident.feature.fishing

import dev.isxander.yacl3.api.NameableEnum
import net.minecraft.network.chat.Component

/** Whether the Fishing Record shows an earn rate next to each stat, and per which unit. */
enum class RecordEarnRate : NameableEnum {
    OFF {
        override fun getDisplayName(): Component = Component.literal("Off")
    },
    MINUTE {
        override fun getDisplayName(): Component = Component.literal("Per minute")
    },
    HOUR {
        override fun getDisplayName(): Component = Component.literal("Per hour")
    }
}

/** How often the Fishing Record's time (and therefore live earn rates) advances. */
enum class RecordUpdateInterval(val seconds: Int, private val label: String) : NameableEnum {
    ONE_SECOND(1, "1 second"),
    FIVE_SECONDS(5, "5 seconds"),
    TEN_SECONDS(10, "10 seconds"),
    THIRTY_SECONDS(30, "30 seconds"),
    ONE_MINUTE(60, "1 minute"),

    /** Time ticks every second, but earn rates only refresh when a catch is recorded. */
    ON_CATCH(1, "On catch");

    override fun getDisplayName(): Component = Component.literal(label)
}

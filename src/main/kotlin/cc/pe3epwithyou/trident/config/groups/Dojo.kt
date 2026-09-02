package cc.pe3epwithyou.trident.config.groups

import cc.pe3epwithyou.trident.config.Config.Companion.handler
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitType
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionEventListener
import dev.isxander.yacl3.dsl.*
import net.minecraft.network.chat.Component

fun dojoCategory(categoryRegistrar: CategoryRegistrar) {
    categoryRegistrar.register("dojo") {
        name(Component.translatable("config.trident.dojo.name"))

        lateinit var dojoSendSplitTime: Option<Boolean>
        lateinit var dojoShowTimer: Option<Boolean>
        lateinit var dojoShowSplitImprovements: Option<Boolean>
        lateinit var dojoShowTimerImprovementAt: Option<Int>
        lateinit var dojoSaveMode: Option<DojoSplitType>

        rootOptions.register("dojo_enabled") {
            name(Component.translatable("config.trident.dojo.enabled.name"))
            description(OptionDescription.of(Component.translatable("config.trident.dojo.enabled.description")))
            binding(handler.instance()::dojoEnabled, true)
            controller(tickBox())
            addListener { option, event ->
                if (event == OptionEventListener.Event.STATE_CHANGE) {
                    dojoSendSplitTime.setAvailable(option.pendingValue())
                    dojoShowTimer.setAvailable(option.pendingValue())
                    dojoShowSplitImprovements.setAvailable(option.pendingValue())
                    dojoShowTimerImprovementAt.setAvailable(option.pendingValue())
                    dojoSaveMode.setAvailable(option.pendingValue())
                }
            }
        }

        dojoSendSplitTime = rootOptions.register("dojo_send_split_time") {
            name(Component.translatable("config.trident.dojo.send_split_time.name"))
            description(OptionDescription.of(Component.translatable("config.trident.dojo.send_split_time.description")))
            binding(handler.instance()::dojoSendSplitTime, true)
            controller(tickBox())
            available { handler.instance().dojoEnabled }
        }

        dojoShowTimer = rootOptions.register("dojo_show_timer") {
            name(Component.translatable("config.trident.dojo.show_timer.name"))
            description(OptionDescription.of(Component.translatable("config.trident.dojo.show_timer.description")))
            binding(handler.instance()::dojoShowTimer, true)
            controller(tickBox())
            available { handler.instance().dojoEnabled }
        }

        dojoShowSplitImprovements = rootOptions.register("dojo_show_split_improvements") {
            name(Component.translatable("config.trident.dojo.show_split_improvements.name"))
            description(OptionDescription.of(Component.translatable("config.trident.dojo.show_split_improvements.description")))
            binding(handler.instance()::dojoShowSplitImprovements, true)
            controller(tickBox())
            available { handler.instance().dojoEnabled }
        }

        dojoShowTimerImprovementAt = rootOptions.register("dojo_show_timer_improvement_at") {
            name(Component.translatable("config.trident.dojo.show_timer_improvement_at.name"))
            description(OptionDescription.of(Component.translatable("config.trident.dojo.show_timer_improvement_at.description")))
            binding(handler.instance()::dojoShowTimerImprovementAt, -3)
            controller(
                slider(
                    IntRange(-10, 0),
                    1
                ) { v -> Component.literal("${v}s") }
            )
            available { handler.instance().dojoEnabled }
        }

        dojoSaveMode = rootOptions.register("dojo_save_mode") {
            name(Component.translatable("config.trident.dojo.save_mode.name"))
            description(OptionDescription.of(Component.translatable("config.trident.dojo.save_mode.description")))
            binding(handler.instance()::dojoSaveMode, DojoSplitType.BEST)
            controller(enumSwitch<DojoSplitType> { v -> v.displayName })
            available { handler.instance().dojoEnabled }
        }
    }
}

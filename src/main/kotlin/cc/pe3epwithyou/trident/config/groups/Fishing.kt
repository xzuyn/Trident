package cc.pe3epwithyou.trident.config.groups

import cc.pe3epwithyou.trident.config.Config.Companion.handler
import cc.pe3epwithyou.trident.feature.fishing.RecordEarnRate
import cc.pe3epwithyou.trident.feature.fishing.RecordUpdateInterval
import cc.pe3epwithyou.trident.utils.Resources
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionEventListener
import dev.isxander.yacl3.dsl.CategoryRegistrar
import dev.isxander.yacl3.dsl.available
import dev.isxander.yacl3.dsl.binding
import dev.isxander.yacl3.dsl.enumSwitch
import dev.isxander.yacl3.dsl.tickBox
import net.minecraft.network.chat.Component

fun fishingCategory(categoryRegistrar: CategoryRegistrar) {
    categoryRegistrar.register("fishing") {
        name(Component.translatable("config.trident.fishing.name"))

        lateinit var suppliesModuleDurability: Option<Boolean>

        rootOptions.register("supplies_module") {
            name(Component.translatable("config.trident.fishing.supplies_module.name"))
            description(
                OptionDescription.createBuilder()
                    .text(Component.translatable("config.trident.fishing.supplies_module.description"))
                    .image(
                        Resources.trident("textures/config/supplies.png"), 224, 208
                    ).build()
            )
            binding(handler.instance()::fishingSuppliesModule, true)
            controller(tickBox())
            addListener { option, event ->
                if (event == OptionEventListener.Event.STATE_CHANGE) {
                    suppliesModuleDurability.setAvailable(option.pendingValue())
                }
            }
        }

        suppliesModuleDurability = rootOptions.register("supplies_module_durability") {
            name(Component.translatable("config.trident.fishing.supplies_module.durability.name"))
            description(OptionDescription.of(Component.translatable("config.trident.fishing.supplies_module.durability.description")))
            binding(
                handler.instance()::fishingSuppliesModuleShowAugmentDurability,
                false
            )
            controller(tickBox())
            available { handler.instance().fishingSuppliesModule }

        }

        rootOptions.register("show_augment_status_in_interface") {
            name(Component.translatable("config.trident.fishing.show_augment_status_in_interface.name"))
            description(OptionDescription.of(Component.translatable("config.trident.fishing.show_augment_status_in_interface.description")))
            binding(
                handler.instance()::fishingShowAugmentStatusInInterface,
                true
            )
            controller(tickBox())
        }

        rootOptions.register("flash_if_depleted") {
            name(Component.translatable("config.trident.fishing.flash_if_depleted.name"))
            description(OptionDescription.of(Component.translatable("config.trident.fishing.flash_if_depleted.description")))
            binding(handler.instance()::fishingFlashIfDepleted, true)
            controller(tickBox())
        }

        rootOptions.register("island_indicators") {
            name(Component.translatable("config.trident.fishing.island_indicators.name"))
            description(OptionDescription.of(Component.translatable("config.trident.fishing.island_indicators.description")))
            binding(handler.instance()::fishingIslandIndicators, true)
            controller(tickBox())
        }

        groups.register("wayfinder_group") {
            name(Component.translatable("config.trident.fishing.wayfinder.name"))
            description(
                OptionDescription.createBuilder()
                    .text(Component.translatable("config.trident.fishing.wayfinder.description"))
                    .image(
                        Resources.trident("textures/config/wayfinder.png"), 290, 200
                    ).build()
            )

            lateinit var compactMode: Option<Boolean>

            options.register("wayfinder_module") {
                name(Component.translatable("config.trident.fishing.wayfinder_module.name"))
                description(
                    OptionDescription.of(Component.translatable("config.trident.fishing.wayfinder_module.description"))
                )
                binding(handler.instance()::fishingWayfinderModule, true)
                controller(tickBox())
                addListener { option, event ->
                    if (event == OptionEventListener.Event.STATE_CHANGE) {
                        compactMode.setAvailable(option.pendingValue())
                    }
                }
            }

            compactMode = options.register("wayfinder_module_compact") {
                name(Component.translatable("config.trident.fishing.wayfinder_module_compact.name"))
                description(
                    OptionDescription.createBuilder()
                        .text(Component.translatable("config.trident.fishing.wayfinder_module_compact.description"))
                        .image(
                            Resources.trident("textures/config/wayfinder_compact.png"), 372, 150
                        ).build()
                )
                binding(handler.instance()::fishingWayfinderModuleCompact, false)
                controller(tickBox())
                available { handler.instance().fishingWayfinderModule }
            }
        }

        groups.register("record_group") {
            name(Component.translatable("config.trident.fishing.record.name"))
            description(OptionDescription.of(Component.translatable("config.trident.fishing.record.description")))

            lateinit var showIcons: Option<Boolean>
            lateinit var earnRate: Option<RecordEarnRate>
            lateinit var updateInterval: Option<RecordUpdateInterval>

            options.register("record_module") {
                name(Component.translatable("config.trident.fishing.record_module.name"))
                description(OptionDescription.of(Component.translatable("config.trident.fishing.record_module.description")))
                binding(handler.instance()::fishingRecordModule, true)
                controller(tickBox())
                addListener { option, event ->
                    if (event == OptionEventListener.Event.STATE_CHANGE) {
                        showIcons.setAvailable(option.pendingValue())
                        earnRate.setAvailable(option.pendingValue())
                        updateInterval.setAvailable(option.pendingValue())
                    }
                }
            }

            showIcons = options.register("record_show_icons") {
                name(Component.translatable("config.trident.fishing.record_show_icons.name"))
                description(OptionDescription.of(Component.translatable("config.trident.fishing.record_show_icons.description")))
                binding(handler.instance()::fishingRecordShowIcons, true)
                controller(tickBox())
                available { handler.instance().fishingRecordModule }
            }

            earnRate = options.register("record_earn_rate") {
                name(Component.translatable("config.trident.fishing.record_earn_rate.name"))
                description(OptionDescription.of(Component.translatable("config.trident.fishing.record_earn_rate.description")))
                binding(handler.instance()::fishingRecordEarnRate, RecordEarnRate.OFF)
                controller(enumSwitch<RecordEarnRate> { v -> v.displayName })
                available { handler.instance().fishingRecordModule }
            }

            updateInterval = options.register("record_update_interval") {
                name(Component.translatable("config.trident.fishing.record_update_interval.name"))
                description(OptionDescription.of(Component.translatable("config.trident.fishing.record_update_interval.description")))
                binding(handler.instance()::fishingRecordUpdateInterval, RecordUpdateInterval.ONE_SECOND)
                controller(enumSwitch<RecordUpdateInterval> { v -> v.displayName })
                available { handler.instance().fishingRecordModule }
            }
        }
    }
}
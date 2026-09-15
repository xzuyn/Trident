package cc.pe3epwithyou.trident.feature.orders

import cc.pe3epwithyou.trident.state.Order
import cc.pe3epwithyou.trident.state.OrderRequirement
import cc.pe3epwithyou.trident.state.OrderReward
import cc.pe3epwithyou.trident.state.Rarity
import cc.pe3epwithyou.trident.utils.extensions.ItemStackExtensions.getLore
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.world.item.ItemStack

object OrderParser {
    private val REQUIREMENT_REGEX = Regex("""(\d+)\s*/\s*(\d+)\s*\[(.+)]""")
    private val REWARD_REGEX = Regex("""(\d+)\s*[xX]\s*\[(.+)]""")

    private enum class Section { NONE, REQUIREMENTS, REWARDS }

    /**
     * Attempts to parse an Event Order item's lore into an [Order].
     * Returns null if [item] is not a valid Event Order item (e.g. an empty/locked slot).
     */
    fun parse(item: ItemStack, slot: Int): Order? {
        if (item.isEmpty) return null

        val loreComponents = item.getLore()
        val lore = loreComponents.map { it.string }
        if (lore.none { it.contains("Order Requirements:", ignoreCase = true) }) return null

        val rarity = detectRarity(loreComponents)

        val requirements = mutableListOf<OrderRequirement>()
        val rewards = mutableListOf<OrderReward>()
        var section = Section.NONE

        lore.forEach { line ->
            when {
                line.contains("Order Requirements:", ignoreCase = true) -> section = Section.REQUIREMENTS
                line.contains("Order Rewards:", ignoreCase = true) -> section = Section.REWARDS
                line.isBlank() -> Unit

                section == Section.REQUIREMENTS -> REQUIREMENT_REGEX.find(line)?.let {
                    val (current, total, name) = it.destructured
                    requirements.add(
                        OrderRequirement(
                            fishName = name.trim(),
                            current = current.toIntOrNull() ?: 0,
                            total = total.toIntOrNull() ?: 1
                        )
                    )
                }

                section == Section.REWARDS -> REWARD_REGEX.find(line)?.let {
                    val (amount, name) = it.destructured
                    rewards.add(
                        OrderReward(
                            name = name.trim(),
                            amount = amount.toIntOrNull() ?: 1
                        )
                    )
                }
            }
        }

        if (requirements.isEmpty()) return null

        return Order(
            rarity = rarity,
            requirements = requirements,
            rewards = rewards,
            slot = slot
        )
    }

    /**
     * Detects an order's rarity primarily by matching the text color of a lore line against
     * the known [Rarity] colors (mirrors [Rarity.getFromItem]), falling back to matching the
     * rarity's name as plain text (e.g. a lore line that just reads "UNCOMMON").
     */
    private fun detectRarity(lore: List<Component>): Rarity {
        lore.forEach { component ->
            val color = component.style.color ?: return@forEach
            Rarity.entries.find { TextColor.fromRgb(it.color) == color }?.let { return it }
        }

        lore.firstNotNullOfOrNull { component ->
            Rarity.entries.find { it.name.equals(component.string.trim(), ignoreCase = true) }
        }?.let { return it }

        return Rarity.COMMON
    }
}


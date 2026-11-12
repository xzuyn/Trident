package cc.pe3epwithyou.trident.feature.crate

import cc.pe3epwithyou.trident.events.click.ClickEvents
import cc.pe3epwithyou.trident.events.container.ContainerContext
import cc.pe3epwithyou.trident.events.container.ContainerEvents
import cc.pe3epwithyou.trident.feature.indicators.BlueprintIndicator
import cc.pe3epwithyou.trident.state.MCCIState
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.defaultFont
import cc.pe3epwithyou.trident.utils.extensions.ItemStackExtensions.findInLore
import cc.pe3epwithyou.trident.utils.minecraft
import cc.pe3epwithyou.trident.utils.playerState
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import java.util.*
import kotlin.math.roundToInt

/**
 * Shows, on each Cosmetic Crate in the Crate Emporium, how likely it is to give a blueprint the
 * player doesn't have yet.
 *
 * The odds are learned from the "Loot Preview" screen: when the player
 * right-clicks a crate in the Crate Emporium, the crate's name is remembered, and once the preview opens
 * the chances of every blueprint the player doesn't own yet (no "crafted" ownership) are added up.
 * Until a crate has been previewed its line reads "Chance: ???".
 *
 * The odds are only as fresh as the last preview of that crate, so previewing again updates them.
 */
object CrateChances {
    private const val EMPORIUM_TITLE = "CRATE EMPORIUM"
    private const val PREVIEW_TITLE = "LOOT PREVIEW"

    /** A right-click on a crate is only linked to a preview opening within this long afterwards. */
    private const val CLICK_TIMEOUT_MS = 10_000L

    private val CHANCE_PATTERN = Regex("""Chance:\s*([\d.,]+)\s*%""")

    private var lastPreviewedCrate: String? = null
    private var lastPreviewedAt = 0L

    fun register() {
        ClickEvents.onClick {
            requireTitle(EMPORIUM_TITLE)
            if (!right) return@onClick
            val stack = clickedItem() ?: return@onClick
            if (stack.isEmpty) return@onClick
            val name = stack.hoverName.string.trim()
            if (!isCrate(name)) return@onClick
            lastPreviewedCrate = name
            lastPreviewedAt = System.currentTimeMillis()
        }
        ContainerEvents.onOpen(::handlePreview)
    }

    private fun isCrate(name: String) = "Crate" in name

    /** Reads the open Loot Preview and stores the non-duplicate chance for the crate it belongs to. */
    private fun handlePreview(ctx: ContainerContext) {
        ctx.requireTitle(PREVIEW_TITLE)

        // Consume the crate so an unrelated preview later on can't be attributed to it
        val crate = lastPreviewedCrate ?: return
        val fresh = System.currentTimeMillis() - lastPreviewedAt <= CLICK_TIMEOUT_MS
        lastPreviewedCrate = null
        if (!fresh) return

        // Only the container's own slots, not the player's inventory slots that follow them
        val menu = ctx.handledScreen.menu
        val slots = menu.slots.take(menu.rowCount * 9)

        var blueprints = 0
        var unownedChance = 0.0
        for (slot in slots) {
            val stack = slot.item
            if (stack.isEmpty) continue
            val blueprint = BlueprintIndicator.readBlueprint(stack) ?: continue
            val chance = stack.findInLore(CHANCE_PATTERN)
                ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
                ?: return // a blueprint without a readable chance would make the total wrong
            blueprints++
            if (!blueprint.ownership) unownedChance += chance
        }
        if (blueprints == 0) return

        playerState().crateChances[crate] = unownedChance.coerceIn(0.0, 100.0)
    }

    /**
     * Adds the "Chance:" line right after the "Cost:" line of a crate's tooltip in the Crate Emporium.
     *
     * @param lines the tooltip lines of the item, name first
     * @return true if [lines] was changed
     */
    @JvmStatic
    fun modifyTooltip(lines: MutableList<Component>): Boolean {
        if (!MCCIState.isOnIsland()) return false
        val screen = minecraft().gui.screen() as? ContainerScreen ?: return false
        if (!screen.title.string.contains(EMPORIUM_TITLE, ignoreCase = true)) return false

        val name = lines.firstOrNull()?.string?.trim() ?: return false
        if (!isCrate(name)) return false

        val costIndex = lines.indexOfFirst { "Cost:" in it.string }
        if (costIndex < 0) return false

        val chance = playerState().crateChances[name]
        val value = if (chance == null) {
            Component.literal("???").withStyle(ChatFormatting.GRAY)
        } else {
            Component.literal(formatPercent(chance)).withStyle(ChatFormatting.YELLOW)
        }

        lines.add(
            costIndex + 1,
            Component.literal("Chance: ").withStyle(ChatFormatting.GRAY).defaultFont()
                .append(value.defaultFont())
        )
        return true
    }

    /** 85 -> "85%", 12.5 -> "12.5%", 3.25 -> "3.25%" */
    private fun formatPercent(percent: Double): String {
        val hundredths = (percent * 100).roundToInt()
        if (hundredths % 100 == 0) return "${hundredths / 100}%"
        return "%.2f".format(Locale.ROOT, hundredths / 100.0).trimEnd('0').trimEnd('.') + "%"
    }
}

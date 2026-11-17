package cc.pe3epwithyou.trident.feature.crate

import cc.pe3epwithyou.trident.state.MCCIState
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.defaultFont
import cc.pe3epwithyou.trident.utils.minecraft
import cc.pe3epwithyou.trident.utils.parseFormattedInt
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import java.util.*

/**
 * Adds a "Coins per Trophy" line right after the "Cost:" line of shop items that give trophies, so
 * the most trophy-efficient purchases are easy to spot (lower is better).
 *
 * The price is the second number of the cost line ("235,994/30,000" -> 30,000), the first being how
 * much the player currently has.
 */
object TrophyRatio {
    private val TROPHIES_PATTERN = Regex("""Trophies:\s*([\d,]+)""")
    private val COST_PATTERN = Regex("""Cost:\s*[\d,]+\s*/\s*([\d,]+)(.*)$""")

    /**
     * @param lines the tooltip lines of the item, name first
     * @return true if [lines] was changed
     */
    @JvmStatic
    fun modifyTooltip(lines: MutableList<Component>): Boolean {
        if (!MCCIState.isOnIsland()) return false
        if (minecraft().gui.screen() !is ContainerScreen) return false

        val trophies = lines.firstNotNullOfOrNull { TROPHIES_PATTERN.find(it.string) }
            ?.groupValues?.get(1)?.parseFormattedInt() ?: return false
        if (trophies <= 0) return false

        val costIndex = lines.indexOfFirst { COST_PATTERN.containsMatchIn(it.string) }
        if (costIndex < 0) return false
        val cost = COST_PATTERN.find(lines[costIndex].string) ?: return false

        // A cost paid in named items (e.g. "[Exclusive Cosmetic Core]") isn't a currency amount
        if (cost.groupValues[2].any { it.isLetter() }) return false
        val price = cost.groupValues[1].parseFormattedInt() ?: return false

        lines.add(
            costIndex + 1,
            Component.literal("Coins per Trophy: ").withStyle(ChatFormatting.GRAY).defaultFont()
                .append(Component.literal(format(price.toDouble() / trophies)).withStyle(ChatFormatting.YELLOW).defaultFont())
        )
        return true
    }

    /** 1500.0 -> "1,500", 428.571 -> "428.6" */
    private fun format(ratio: Double): String {
        val tenths = Math.round(ratio * 10)
        if (tenths % 10 == 0L) return "%,d".format(Locale.ROOT, tenths / 10)
        return "%,.1f".format(Locale.ROOT, tenths / 10.0)
    }
}

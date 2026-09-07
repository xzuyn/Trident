package cc.pe3epwithyou.trident.interfaces.dojo

import cc.pe3epwithyou.trident.feature.dojo.ALWAYS_SEPARATE_TRANSITIONS
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitManager
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitTimer
import cc.pe3epwithyou.trident.feature.dojo.buildCanonicalLevelNames
import cc.pe3epwithyou.trident.feature.dojo.classifyDojoSection
import cc.pe3epwithyou.trident.feature.dojo.group
import cc.pe3epwithyou.trident.interfaces.dojo.widgets.DojoSplitDividerWidget
import cc.pe3epwithyou.trident.interfaces.dojo.widgets.DojoSplitRowMode
import cc.pe3epwithyou.trident.interfaces.dojo.widgets.DojoSplitRowWidget
import cc.pe3epwithyou.trident.interfaces.dojo.widgets.DojoSplitSummaryWidget
import cc.pe3epwithyou.trident.interfaces.shared.TridentDialog
import cc.pe3epwithyou.trident.interfaces.themes.TridentThemed
import cc.pe3epwithyou.trident.state.FontCollection
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.mccFont
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.offset
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.LayoutConstants
import com.noxcrew.sheeplib.layout.grid
import com.noxcrew.sheeplib.theme.Themed
import com.noxcrew.sheeplib.util.opacity
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

class DojoSplitsDialog(x: Int, y: Int, key: String) : TridentDialog(x, y, key),
    Themed by TridentThemed {
    companion object {
        private const val CONTENT_WIDTH = 150
        private val TITLE_COLOR: Int = 0x038AFF opacity 127
    }

    /** One row to be drawn, with the identity it should be shown/matched under and the visual group it belongs to (for divider placement). */
    private data class RenderRow(val uid: String, val name: String, val mode: DojoSplitRowMode, val groupKey: Any)

    private fun getTitleWidget(): DojoDialogTitle {
        val title = FontCollection.get("_fonts/icon/quest_log.png").withStyle(
            Style.EMPTY.withoutShadow()
        ).append(Component.literal(" SPLITS").mccFont().offset(y = -0.5f))
        return DojoDialogTitle(this, title, TITLE_COLOR)
    }

    override var title = getTitleWidget()

    /**
     * Every Dojo course has the same fixed, interleaved layout: bonus branch 1, main group 1,
     * bonus branch 2, main group 2, bonus branch 3, main group 3, then one of the three
     * endings. Bonus branches not planned in config are left out entirely. Returns
     * (compositeUid, displayName) pairs — the uid is "origin_destination" (e.g. "START_M1-1",
     * "B1-3_M1-1"), since the unnamed transition into a level is folded into its own measured
     * time and two different transitions into the same level genuinely take different amounts
     * of time.
     */
    private fun buildCanonicalOrder(): List<Pair<String, String>> {
        var previous = "START"
        return buildCanonicalLevelNames().map { name ->
            val uid = "${previous}_$name"
            previous = name
            uid to name
        }
    }

    override fun layout(): GridLayout = grid {
        val font = minecraft().font
        val course = DojoSplitTimer.instance?.courseName ?: DojoSplitManager.lastCourseName

        if (course == null) {
            StringWidget(
                Component.literal("Start a run to see splits".uppercase()).mccFont()
                    .withStyle(ChatFormatting.GRAY), font
            ).atBottom(0, settings = LayoutConstants.LEFT)
            return@grid
        }

        val timer = DojoSplitTimer.instance

        val orderedRows = mutableListOf<Pair<String, String>>()
        val matchedUids = linkedSetOf<String>()
        buildCanonicalOrder().forEach { (uid, name) ->
            orderedRows.add(uid to name)
            matchedUids.add(uid)
        }

        // Safety net: anything actually reached this run always shows, even if it didn't
        // match a planned slot above (e.g. you deviated from the planned route).
        val seenThisRun = linkedSetOf<String>()
        timer?.completedSplits?.forEach { seenThisRun.add(it.levelUid) }
        if (timer != null && !timer.isBetween && timer.currentLevelUid.isNotEmpty()) seenThisRun.add(timer.currentLevelUid)
        seenThisRun.forEach { uid ->
            if (uid !in matchedUids) {
                val name = timer?.completedSplits?.firstOrNull { it.levelUid == uid }?.levelName
                    ?: timer?.takeIf { it.currentLevelUid == uid }?.levelName
                    ?: uid
                orderedRows.add(uid to name)
            }
        }

        // Transitions worth their own row get a unique group key (their own uid), so a divider
        // always forms on both sides of them — isolating them as their own single-row section
        // — regardless of whether the level before/after happens to share a visual group (e.g.
        // "M2-3 -> M3-1" would otherwise sit with no divider at all, since M2 and M3 both
        // classify as the same plain MAIN group).
        val renderRows = mutableListOf<RenderRow>()
        orderedRows.forEach { (uid, name) ->
            if (uid in ALWAYS_SEPARATE_TRANSITIONS) {
                renderRows.add(RenderRow(uid, name, DojoSplitRowMode.TRANSITION_ONLY, uid))
                renderRows.add(RenderRow(uid, name, DojoSplitRowMode.LEVEL_ONLY, classifyDojoSection(name).group()))
            } else {
                renderRows.add(RenderRow(uid, name, DojoSplitRowMode.COMBINED, classifyDojoSection(name).group()))
            }
        }

        var previousGroupKey: Any? = null
        renderRows.forEach { row ->
            if (previousGroupKey != null && row.groupKey != previousGroupKey) {
                DojoSplitDividerWidget(CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
            }
            previousGroupKey = row.groupKey

            DojoSplitRowWidget(row.uid, row.name, CONTENT_WIDTH, row.mode).atBottom(0, settings = LayoutConstants.LEFT)
        }

        DojoSplitDividerWidget(CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
        DojoSplitSummaryWidget(orderedRows, CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
    }

    override fun refresh() {
        title = getTitleWidget()
        super.refresh()
    }
}

package cc.pe3epwithyou.trident.interfaces.dojo

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.feature.dojo.DojoEnding
import cc.pe3epwithyou.trident.feature.dojo.DojoSectionGroup
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitManager
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitTimer
import cc.pe3epwithyou.trident.feature.dojo.classifyDojoSection
import cc.pe3epwithyou.trident.feature.dojo.group
import cc.pe3epwithyou.trident.interfaces.dojo.widgets.DojoSplitDividerWidget
import cc.pe3epwithyou.trident.interfaces.dojo.widgets.DojoSplitRowWidget
import cc.pe3epwithyou.trident.interfaces.dojo.widgets.DojoSplitSummaryWidget
import cc.pe3epwithyou.trident.interfaces.shared.TridentDialog
import cc.pe3epwithyou.trident.interfaces.themes.DialogTitle
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

    private fun getTitleWidget(): DialogTitle {
        val title = FontCollection.get("_fonts/icon/quest_log.png").withStyle(
            Style.EMPTY.withoutShadow()
        ).append(Component.literal(" SPLITS").mccFont().offset(y = -0.5f))
        return DialogTitle(this, title, TITLE_COLOR)
    }

    override var title = getTitleWidget()

    /**
     * Every Dojo course has the same fixed, interleaved layout: bonus branch 1, main group 1,
     * bonus branch 2, main group 2, bonus branch 3, main group 3, then one of the three
     * endings. Bonus branches not planned in config are left out entirely.
     */
    private fun buildCanonicalOrder(): List<String> = buildList {
        if (Config.Dojo.routeBonus1) addAll(listOf("B1-1", "B1-2", "B1-3"))
        addAll(listOf("M1-1", "M1-2", "M1-3"))
        if (Config.Dojo.routeBonus2) addAll(listOf("B2-1", "B2-2", "B2-3"))
        addAll(listOf("M2-1", "M2-2", "M2-3"))
        if (Config.Dojo.routeBonus3) addAll(listOf("B3-1", "B3-2", "B3-3"))
        addAll(listOf("M3-1", "M3-2", "M3-3"))
        add(
            when (Config.Dojo.routeEnding) {
                DojoEnding.EASY -> "B4-1"
                DojoEnding.MEDIUM -> "B4-2"
                DojoEnding.HARD -> "B4-3"
            }
        )
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
        val courseSplits = DojoSplitManager.getCourseSplits(course)
        val namesByUid = courseSplits.levelNames

        // A level's uid is stable across runs (it's derived from its name + styling), so once
        // we've seen a name before we can look its uid back up and pre-populate its row.
        fun findUid(name: String): String? {
            if (timer != null && !timer.isBetween && timer.levelName == name) return timer.currentLevelUid
            timer?.completedSplits?.firstOrNull { it.levelName == name }?.let { return it.levelUid }
            return namesByUid.entries.firstOrNull { it.value == name }?.key
        }

        val orderedUids = mutableListOf<String>()
        val matchedUids = linkedSetOf<String>()
        buildCanonicalOrder().forEach { name ->
            val uid = findUid(name) ?: return@forEach
            orderedUids.add(uid)
            matchedUids.add(uid)
        }

        // Safety net: anything actually reached this run always shows, even if it didn't
        // match a slot above (e.g. the course layout doesn't match what's hardcoded here).
        val seenThisRun = linkedSetOf<String>()
        timer?.completedSplits?.forEach { seenThisRun.add(it.levelUid) }
        if (timer != null && !timer.isBetween) seenThisRun.add(timer.currentLevelUid)
        seenThisRun.forEach { uid -> if (uid !in matchedUids) orderedUids.add(uid) }

        if (orderedUids.isEmpty()) {
            StringWidget(
                Component.literal("No splits recorded yet".uppercase()).mccFont()
                    .withStyle(ChatFormatting.GRAY), font
            ).atBottom(0, settings = LayoutConstants.LEFT)
            return@grid
        }

        fun nameOf(uid: String): String = namesByUid[uid]
            ?: timer?.completedSplits?.firstOrNull { it.levelUid == uid }?.levelName
            ?: timer?.takeIf { it.currentLevelUid == uid }?.levelName
            ?: "???"

        var previousGroup: DojoSectionGroup? = null
        orderedUids.forEach { uid ->
            val name = nameOf(uid)
            val currentGroup = classifyDojoSection(name).group()
            if (previousGroup != null && currentGroup != previousGroup) {
                DojoSplitDividerWidget(CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
            }
            previousGroup = currentGroup

            DojoSplitRowWidget(uid, name, CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
        }

        DojoSplitDividerWidget(CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
        DojoSplitSummaryWidget(orderedUids, CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
    }

    override fun refresh() {
        title = getTitleWidget()
        super.refresh()
    }
}

package cc.pe3epwithyou.trident.interfaces.dojo

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.feature.dojo.DojoEnding
import cc.pe3epwithyou.trident.feature.dojo.DojoSection
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

        fun nameOf(uid: String): String = courseSplits.levelNames[uid]
            ?: timer?.completedSplits?.firstOrNull { it.levelUid == uid }?.levelName
            ?: timer?.takeIf { it.currentLevelUid == uid }?.levelName
            ?: "???"

        // Order: levels reached this run first (in order), then any other known levels for
        // this course that haven't been reached yet — filtered to the planned route, so
        // branches/endings you're not attempting don't clutter the box before you get there.
        val seenThisRun = linkedSetOf<String>()
        timer?.completedSplits?.forEach { seenThisRun.add(it.levelUid) }
        if (timer != null && !timer.isBetween) seenThisRun.add(timer.currentLevelUid)

        val orderedUids = mutableListOf<String>()
        orderedUids.addAll(seenThisRun)
        courseSplits.levelNames.keys.forEach { uid ->
            if (uid in seenThisRun) return@forEach
            if (isPlannedSection(classifyDojoSection(nameOf(uid)))) orderedUids.add(uid)
        }

        if (orderedUids.isEmpty()) {
            StringWidget(
                Component.literal("No splits recorded yet".uppercase()).mccFont()
                    .withStyle(ChatFormatting.GRAY), font
            ).atBottom(0, settings = LayoutConstants.LEFT)
            return@grid
        }

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

    /**
     * Whether a not-yet-reached section belongs to the route currently planned in config.
     * Levels actually reached or completed this run are always shown regardless of this.
     */
    private fun isPlannedSection(section: DojoSection): Boolean = when (section) {
        DojoSection.MAIN -> true
        DojoSection.BONUS_1 -> Config.Dojo.routeBonus1
        DojoSection.BONUS_2 -> Config.Dojo.routeBonus2
        DojoSection.BONUS_3 -> Config.Dojo.routeBonus3
        DojoSection.ENDING_EASY -> Config.Dojo.routeEnding == DojoEnding.EASY
        DojoSection.ENDING_MEDIUM -> Config.Dojo.routeEnding == DojoEnding.MEDIUM
        DojoSection.ENDING_HARD -> Config.Dojo.routeEnding == DojoEnding.HARD
    }
}

package cc.pe3epwithyou.trident.interfaces.dojo

import cc.pe3epwithyou.trident.feature.dojo.DojoSplitManager
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitTimer
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

        // Order: levels reached this run first (in order), then any other known levels for
        // this course that haven't been reached yet, shown with their historical best time.
        val seenThisRun = linkedSetOf<String>()
        timer?.completedSplits?.forEach { seenThisRun.add(it.levelUid) }
        if (timer != null && !timer.isBetween) seenThisRun.add(timer.currentLevelUid)

        val orderedUids = mutableListOf<String>()
        orderedUids.addAll(seenThisRun)
        courseSplits.levelNames.keys.forEach { uid -> if (uid !in seenThisRun) orderedUids.add(uid) }

        if (orderedUids.isEmpty()) {
            StringWidget(
                Component.literal("No splits recorded yet".uppercase()).mccFont()
                    .withStyle(ChatFormatting.GRAY), font
            ).atBottom(0, settings = LayoutConstants.LEFT)
            return@grid
        }

        orderedUids.forEach { uid ->
            val fallbackName = courseSplits.levelNames[uid]
                ?: timer?.completedSplits?.firstOrNull { it.levelUid == uid }?.levelName
                ?: timer?.takeIf { it.currentLevelUid == uid }?.levelName
                ?: "???"
            DojoSplitRowWidget(uid, fallbackName, CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
        }

        DojoSplitSummaryWidget(orderedUids, CONTENT_WIDTH).atBottom(0, settings = LayoutConstants.LEFT)
    }

    override fun refresh() {
        title = getTitleWidget()
        super.refresh()
    }
}

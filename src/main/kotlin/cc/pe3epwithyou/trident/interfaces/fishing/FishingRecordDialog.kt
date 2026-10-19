package cc.pe3epwithyou.trident.interfaces.fishing

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.feature.fishing.FishingRecord
import cc.pe3epwithyou.trident.feature.fishing.RecordStat
import cc.pe3epwithyou.trident.interfaces.shared.TridentDialog
import cc.pe3epwithyou.trident.interfaces.themes.DialogTitle
import cc.pe3epwithyou.trident.interfaces.themes.TridentThemed
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.mccFont
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.offset
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.withTridentFont
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.LayoutConstants
import com.noxcrew.sheeplib.dialog.title.DialogTitleWidget
import com.noxcrew.sheeplib.layout.grid
import com.noxcrew.sheeplib.theme.Themed
import com.noxcrew.sheeplib.util.opacity
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

class FishingRecordDialog(x: Int, y: Int, key: String) : TridentDialog(x, y, key),
    Themed by TridentThemed {
    private companion object {
        private val TITLE_COLOR: Int = 0x219bf3 opacity 127
        private const val TIME_GLYPH = "\uE018"
    }

    private fun getWidgetTitle(): DialogTitleWidget {
        val title = Component.literal(TIME_GLYPH).withTridentFont()
            .append(Component.literal(" FISHING RECORD").mccFont().offset(y = -0.5f))
        return DialogTitle(this, title, TITLE_COLOR)
    }

    override var title = getWidgetTitle()

    private fun row(glyph: String, label: String, value: String, rate: String = ""): MutableComponent {
        val line = Component.empty()
        if (Config.Fishing.recordShowIcons) {
            line.append(Component.literal(glyph).withTridentFont()).append(Component.literal(" "))
        }
        line.append(Component.literal("${label.uppercase()}: ").mccFont())
        line.append(Component.literal(value).mccFont())
        if (rate.isNotEmpty()) {
            line.append(Component.literal(rate.uppercase()).mccFont().withStyle(ChatFormatting.GRAY))
        }
        return line
    }

    override fun layout(): GridLayout = grid {
        val font = minecraft().font

        StringWidget(
            row(TIME_GLYPH, "Time", FishingRecord.formatTime(FishingRecord.fishingTime)), font
        ).atBottom(0, settings = LayoutConstants.LEFT)

        RecordStat.entries.forEach { stat ->
            val amount = FishingRecord.amount(stat)
            val value = if (stat == RecordStat.XP || stat == RecordStat.TOKENS) "%,d".format(amount) else amount.toString()
            StringWidget(
                row(stat.glyph, stat.label, value, FishingRecord.earnRateSuffix(stat)), font
            ).atBottom(0, settings = LayoutConstants.LEFT)
        }
    }

    override fun refresh() {
        title = getWidgetTitle()
        super.refresh()
    }
}

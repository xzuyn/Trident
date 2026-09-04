package cc.pe3epwithyou.trident.interfaces.dojo

import cc.pe3epwithyou.trident.feature.dojo.DojoSplitTimer
import cc.pe3epwithyou.trident.utils.Resources
import cc.pe3epwithyou.trident.utils.extensions.GraphicsExtensions.fillRoundedAll
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.CompoundWidget
import com.noxcrew.sheeplib.dialog.Dialog
import com.noxcrew.sheeplib.dialog.title.DialogTitleWidget
import com.noxcrew.sheeplib.layout.CanvasLayout
import com.noxcrew.sheeplib.theme.Themed
import com.noxcrew.sheeplib.util.opacity
import com.noxcrew.sheeplib.util.Icon
import com.noxcrew.sheeplib.widget.IconButton
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.network.chat.Component

/**
 * Title bar for the Dojo splits dialog. Same structure as [cc.pe3epwithyou.trident.interfaces.themes.DialogTitle],
 * with an added reset button just left of close that clears the displayed run back to plain
 * historical bests (see [DojoSplitTimer.clearDisplay]) without touching saved history.
 */
class DojoDialogTitle(
    override val dialog: Dialog,
    private val component: Component,
    private val color: Int = 0x111111 opacity 127,
    private val tooltip: Tooltip? = null,
) :
    CompoundWidget(0, 0, dialog.width, FONT_HEIGHT + PADDING * 2),
    DialogTitleWidget,
    Themed by dialog {
    companion object {
        const val FONT_HEIGHT = 7
        const val PADDING = 4
        const val GAP = 1
    }

    override fun getWidth(): Int = layout.width
    override fun setWidth(i: Int) {
        layout.width = i
    }

    override fun getHeight(): Int = layout.height

    override val layout: CanvasLayout = CanvasLayout(
        100,
        FONT_HEIGHT + PADDING * 2,
    ).apply {
        val font = minecraft().font
        val w = StringWidget(
            component,
            font
        )
        if (tooltip != null) {
            w.setTooltip(tooltip)
        }
        w.at(top = PADDING, left = PADDING)

        IconButton(
            theme.icons.close,
            marginY = PADDING + 1,
            marginX = PADDING,
        ) { _, _ -> dialog.close() }
            .at(top = 0, right = 0)

        IconButton(
            Icon(Resources.trident("dojo/reset"), height = 16, width = 16),
            marginY = PADDING + 1,
            marginX = PADDING,
        ) { _, _ -> DojoSplitTimer.clearDisplay() }
            .at(top = 0, right = height + GAP)
    }

    override fun getRectangle(): ScreenRectangle = super<DialogTitleWidget>.getRectangle()

    init {
        layout.arrangeElements()
        layout.visitWidgets(this::addChild)
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, i: Int, j: Int, f: Float) {
        graphics.fillRoundedAll(
            x,
            y,
            getWidth(),
            getHeight(),
            color
        )
        super.extractWidgetRenderState(graphics, i, j, f)
    }

    override fun onDialogResize() {
        setWidth(dialog.width)
        layout.arrangeElements()
    }
}

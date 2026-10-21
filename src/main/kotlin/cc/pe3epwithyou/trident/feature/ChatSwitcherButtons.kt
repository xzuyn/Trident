package cc.pe3epwithyou.trident.feature

import cc.pe3epwithyou.trident.feature.chat.ChatControllerManager
import cc.pe3epwithyou.trident.feature.discord.ActivityManager
import cc.pe3epwithyou.trident.state.Game
import cc.pe3epwithyou.trident.state.MCCIState
import cc.pe3epwithyou.trident.utils.Resources
import cc.pe3epwithyou.trident.utils.Texture
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.mccFont
import cc.pe3epwithyou.trident.utils.extensions.GraphicsExtensions.fillRoundedAll
import cc.pe3epwithyou.trident.utils.minecraft
import cc.pe3epwithyou.trident.utils.withCooldown
import com.noxcrew.sheeplib.util.lighten
import com.noxcrew.sheeplib.util.opacity
import com.noxcrew.sheeplib.util.opaqueColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

object ChatSwitcherButtons {
    fun getChatModes(): List<ChatMode> = buildList {
        add(ChatMode.LOCAL)
        ActivityManager.Party.size?.let { if (it > 1) add(ChatMode.PARTY) }
        if (MCCIState.game.hasTeamChat) add(ChatMode.TEAM)
        if (MCCIState.isInPlobby()) add(ChatMode.PLOBBY)
    }

    fun getCurrentButtons(): List<Widget> = buildList {
        addAll(getChatModes().map(::Widget))
        if (MCCIState.game == Game.FISHING && islandShortName() != null) add(LocationWidget())
    }

    /** "i1" for Island 1 (Verdant Woods) up to "i9". Null for grottos, which aren't numbered islands. */
    private fun islandShortName(): String? = when (MCCIState.fishingState.island) {
        "temperate_1" -> "i1"
        "temperate_2" -> "i2"
        "temperate_3" -> "i3"
        "tropical_1" -> "i4"
        "tropical_2" -> "i5"
        "tropical_3" -> "i6"
        "barren_1" -> "i7"
        "barren_2" -> "i8"
        "barren_3" -> "i9"
        else -> null
    }

    open class Widget(val mode: ChatMode) : AbstractWidget(0, 0, 0, HEIGHT, Component.empty()) {
        companion object {
            const val HEIGHT = 9
            const val PADDING = 2

            const val CHAT_HEIGHT = 16
            private val ICON_SPRITE =
                Resources.trident("textures/interface/chat_channels/channel_icon.png")
        }

        init {
            val font = minecraft().font
            width = font.width(Component.literal(mode.displayName).withoutShadow().mccFont()) + 9 + PADDING * 2 + 4
        }

        open val backgroundColor: Int
            get() = when {
                isHovered -> mode.color.lighten(-0.5f) opacity 128
                else -> mode.color.lighten(-0.75f) opacity 128
            }

        open val textColor: Int
            get() = mode.color.opaqueColor()

        private var texture = Texture(ICON_SPRITE, 9, 7)

        override fun extractWidgetRenderState(
            graphics: GuiGraphicsExtractor, i: Int, j: Int, f: Float
        ) {
            graphics.fillRoundedAll(x, y, width, height, backgroundColor)
            texture.blit(graphics, x + PADDING, y + 1)
            graphics.text(
                minecraft().font,
                Component.literal(mode.displayName.uppercase()).mccFont().withoutShadow(),
                PADDING + x + 9 + 4,
                y,
                textColor
            )
        }

        override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
            withCooldown(mode, 1_000) {
                val connection = minecraft().connection ?: return@withCooldown
                connection.sendCommand(mode.commandName)
            }
            ChatControllerManager.clearController()
        }

        override fun updateWidgetNarration(narratigonElementOutput: NarrationElementOutput) = Unit
    }

    /** Copies "{island} @ {x} {z}" (e.g. "i5 @ -177 2566") to the clipboard instead of switching channel. */
    class LocationWidget : Widget(ChatMode.LOCATION) {
        override fun onClick(mouseButtonEvent: MouseButtonEvent, bl: Boolean) {
            val island = islandShortName() ?: return
            val pos = minecraft().player?.blockPosition() ?: return
            GLFW.glfwSetClipboardString(minecraft().window.handle(), "$island @ ${pos.x} ${pos.z}")
        }
    }

    data class ChatMode(
        val commandName: String,
        val displayName: String,
        val color: Int
    ) {
        companion object {
            val LOCAL =
                ChatMode("chat local", "LOCAL", 0xffffff)
            val PARTY =
                ChatMode("chat party", "PARTY", 0x7670e8)
            val TEAM =
                ChatMode("chat team", "TEAM", 0x1bff46)
            val PLOBBY =
                ChatMode("chat plobby", "PLOBBY", 0xffbc40)

            // Not a channel, so there's no command; see LocationWidget
            val LOCATION =
                ChatMode("", "LOCATION", 0x40d4ff)
        }
    }
}
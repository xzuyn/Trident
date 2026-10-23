package cc.pe3epwithyou.trident.feature

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.state.MCCIState
import cc.pe3epwithyou.trident.utils.Logger
import cc.pe3epwithyou.trident.utils.TridentFont
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.withSwatch
import cc.pe3epwithyou.trident.utils.extensions.GraphicsExtensions.fillRoundedAll
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.util.opacity
import com.noxcrew.sheeplib.util.opaqueColor
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastManager
import net.minecraft.network.chat.Component
import java.time.Instant

/**
 * Shows a notification at :00, :15, :30 and :45 of every hour (UTC, which is when the event games start)
 * reminding which event game is starting.
 */
object EventReminder {
    /** Halloween Event: minute of the hour -> the game starting then. Update or remove for the next event. */
    private val SCHEDULE = mapOf(
        0 to "Crypt Collectors",
        15 to "Tricky Tower",
        30 to "Creepy Critters",
        45 to "Scarecrow Sprint",
    )

    private const val GAME_COLOR = 0xff8c1a
    private const val QUARTER_SECONDS = 15L * 60
    private const val TOAST_DURATION_MS = 4000L

    // Starts at the current quarter hour, so it only fires when a new one begins, not when the game launches
    private var lastQuarter = Instant.now().epochSecond / QUARTER_SECONDS

    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { check() })
    }

    private fun check() {
        val quarter = Instant.now().epochSecond / QUARTER_SECONDS
        if (quarter == lastQuarter) return
        lastQuarter = quarter

        if (!Config.Global.eventReminders) return
        if (!MCCIState.isOnIsland()) return

        val minute = (quarter * 15 % 60).toInt()
        val game = SCHEDULE[minute] ?: return
        send(game)
    }

    private fun send(game: String) {
        minecraft().gui.toastManager().addToast(EventToast(game))

        Logger.sendMessage(
            Component.empty()
                .append(Component.literal(game).withColor(GAME_COLOR))
                .append(Component.literal(" is starting").withSwatch(TridentFont.TRIDENT_ACCENT))
        )
    }

    // Same look as the crafting toast, without an icon
    private class EventToast(private val game: String) : Toast {
        private var wantedVisibility: Toast.Visibility = Toast.Visibility.HIDE

        override fun height(): Int = 26

        override fun width(): Int = 180

        override fun getWantedVisibility(): Toast.Visibility = this.wantedVisibility

        override fun update(
            toastManager: ToastManager,
            l: Long
        ) {
            this.wantedVisibility =
                if (l > TOAST_DURATION_MS * toastManager.notificationDisplayTimeMultiplier) Toast.Visibility.HIDE else Toast.Visibility.SHOW
        }

        override fun extractRenderState(
            guiGraphics: GuiGraphicsExtractor,
            font: Font,
            l: Long
        ) {
            guiGraphics.fillRoundedAll(1, 2, this.width() - 3, this.height() - 2, 0x111111 opacity 192)
            guiGraphics.text(font, Component.literal(game).withColor(GAME_COLOR), 8, 6, 0xFFFFFF.opaqueColor())
            guiGraphics.text(
                font, Component.literal("is starting").withSwatch(
                    TridentFont.TRIDENT_COLOR
                ), 8, 15, 0xFFFFFF.opaqueColor()
            )
        }
    }
}

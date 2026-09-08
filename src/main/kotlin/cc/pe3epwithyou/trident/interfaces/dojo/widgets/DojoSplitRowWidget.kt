package cc.pe3epwithyou.trident.interfaces.dojo.widgets

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.feature.dojo.DojoSection
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitManager
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitTimer
import cc.pe3epwithyou.trident.feature.dojo.classifyDojoSection
import cc.pe3epwithyou.trident.utils.extensions.ComponentExtensions.mccFont
import cc.pe3epwithyou.trident.utils.extensions.GraphicsExtensions.fillRoundedAll
import cc.pe3epwithyou.trident.utils.minecraft
import com.noxcrew.sheeplib.util.opaqueColor
import com.noxcrew.sheeplib.util.opacity
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

/** What a [DojoSplitRowWidget] displays for a given (transitionUid, levelName) slot. */
enum class DojoSplitRowMode {
    /** Transition + level time summed into one row (the default, compact view). */
    COMBINED,

    /** Just the unnamed transition leading into the level, e.g. "B1-3 → M1-1". Only ever shows as a route-based prediction (see DojoSplitTimer.predictedNextTransitionUid) before it's confirmed — its identity isn't truly known until the level's own subtitle arrives. */
    TRANSITION_ONLY,

    /** Just the level's own obstacle time, e.g. "M1-1" — identical no matter which transition led here. */
    LEVEL_ONLY
}

/**
 * One row of the Dojo split list. Draws itself fresh every frame (rather than baking text at
 * construction time) so the active row's time can tick live without needing a dialog relayout.
 */
class DojoSplitRowWidget(
    private val levelUid: String,
    private val fallbackName: String,
    width: Int,
    private val mode: DojoSplitRowMode = DojoSplitRowMode.COMBINED
) : AbstractWidget(0, 0, width, HEIGHT, Component.empty()) {
    companion object {
        const val HEIGHT = 9
        private const val PADDING = 4
        private const val GAP = 3
    }

    override fun extractWidgetRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        val font = minecraft().font
        val timer = DojoSplitTimer.instance
        val completedRow = timer?.completedSplits?.firstOrNull { it.levelUid == levelUid }
        // A transition's identity isn't known until the level subtitle arrives, at which point
        // it's already finalized — so it's never shown as "live" in the usual sense, only
        // "reached" or "predicted" (see below) or not.
        val isActive = mode != DojoSplitRowMode.TRANSITION_ONLY &&
            timer != null && !timer.isBetween && timer.currentLevelUid == levelUid
        // The route is known ahead of time (see Config.Dojo's route options), so while still in
        // the unnamed gap before any level subtitle confirms anything, we can optimistically
        // guess this is the transition currently in progress if the player is following the
        // planned route. If a different level's subtitle ends up confirming a different
        // transition, this naturally reverts to the normal "not reached" placeholder below
        // on its own, since predictedNextTransitionUid() won't match this row anymore.
        val isPredicted = mode == DojoSplitRowMode.TRANSITION_ONLY &&
            timer != null && timer.isBetween && timer.predictedNextTransitionUid() == levelUid

        if (isActive || isPredicted) {
            graphics.fillRoundedAll(x, y, width, HEIGHT, 0xFFFFFF opacity 24)
        }

        val name: String
        val time: Double?
        val delta: Double?
        val live: Boolean
        val reached: Boolean

        when {
            completedRow != null -> {
                name = completedRow.levelName
                live = false
                reached = true
                when (mode) {
                    DojoSplitRowMode.COMBINED -> {
                        time = completedRow.timeSeconds
                        delta = completedRow.deltaSeconds
                    }

                    DojoSplitRowMode.TRANSITION_ONLY -> {
                        time = completedRow.transitionSeconds
                        delta = completedRow.transitionDelta
                    }

                    DojoSplitRowMode.LEVEL_ONLY -> {
                        time = completedRow.levelSeconds
                        delta = completedRow.levelDelta
                    }
                }
            }

            // The transition into this level has already finished (its subtitle arrived),
            // even though the level itself — and therefore the whole combined split — hasn't
            // finished yet, so there's no completedRow for it to read from above. The
            // transition's own time is fixed the moment the level starts, so show it now
            // rather than waiting for the level to also finish.
            mode == DojoSplitRowMode.TRANSITION_ONLY && timer != null && !timer.isBetween && timer.currentLevelUid == levelUid -> {
                name = fallbackName
                time = timer.resolvedTransitionSeconds()
                delta = timer.resolvedTransitionImprovement()
                    ?.takeIf { Config.Dojo.showSplitImprovements && it >= Config.Dojo.showTimerImprovementAt }
                live = false
                reached = true
            }

            isPredicted -> {
                name = fallbackName
                val elapsed = timer!!.currentSplitTimeSeconds()
                time = elapsed
                val best = timer.courseName?.let { DojoSplitManager.getSplitSeconds(it, levelUid) }
                delta = best?.let { elapsed - it }
                    ?.takeIf { Config.Dojo.showSplitImprovements && it >= Config.Dojo.showTimerImprovementAt }
                live = true
                reached = true
            }

            isActive -> {
                name = timer!!.levelName
                live = true
                reached = true
                when (mode) {
                    DojoSplitRowMode.LEVEL_ONLY -> {
                        time = timer.currentLevelPhaseSeconds() ?: 0.0
                        delta = timer.levelPhaseImprovement()
                            ?.takeIf { Config.Dojo.showSplitImprovements && it >= Config.Dojo.showTimerImprovementAt }
                    }

                    else -> {
                        time = timer.currentSplitTimeSeconds()
                        delta = timer.splitImprovement()
                            ?.takeIf { Config.Dojo.showSplitImprovements && it >= Config.Dojo.showTimerImprovementAt }
                    }
                }
            }

            else -> {
                val course = timer?.courseName ?: DojoSplitManager.lastCourseName
                name = fallbackName
                delta = null
                live = false
                reached = false
                val transitionBest = course?.let { DojoSplitManager.getSplitSeconds(it, levelUid) }
                val levelBest = course?.let { DojoSplitManager.getSplitSeconds(it, fallbackName) }
                time = when (mode) {
                    DojoSplitRowMode.COMBINED -> if (transitionBest != null && levelBest != null) transitionBest + levelBest else null
                    DojoSplitRowMode.TRANSITION_ONLY -> transitionBest
                    DojoSplitRowMode.LEVEL_ONLY -> levelBest
                }
            }
        }

        val nameColor = when {
            live -> ChatFormatting.WHITE
            reached -> ChatFormatting.GRAY
            else -> ChatFormatting.DARK_GRAY
        }
        val displayed = if (mode == DojoSplitRowMode.TRANSITION_ONLY) transitionDisplayName() else displayName(name)
        val nameComponent = Component.literal(displayed).withStyle(nameColor).mccFont()
        graphics.text(font, nameComponent, x + PADDING, y + 1, 0xFFFFFF.opaqueColor())

        val timeText = if (time != null) String.format("%.3f", time) else "--.---"
        val timeColor = if (reached) ChatFormatting.WHITE else ChatFormatting.DARK_GRAY
        val timeComponent = Component.literal(timeText).withStyle(timeColor).mccFont()
        val timeWidth = font.width(timeComponent)
        graphics.text(font, timeComponent, x + width - timeWidth - PADDING, y + 1, 0xFFFFFF.opaqueColor())

        if (delta == null) return
        val deltaColor = if (delta > 0) ChatFormatting.RED else ChatFormatting.GREEN
        val sign = if (delta > 0) "+" else ""
        val deltaComponent = Component.literal("$sign${String.format("%.2f", delta)}").withStyle(deltaColor).mccFont()
        val deltaWidth = font.width(deltaComponent)
        graphics.text(font, deltaComponent, x + width - timeWidth - deltaWidth - PADDING - GAP, y + 1, 0xFFFFFF.opaqueColor())
    }

    /** "START_M1-1" -> "START → M1-1" — the raw composite key is accurate but reads as noise. */
    private fun transitionDisplayName(): String {
        val parts = levelUid.split("_", limit = 2)
        return if (parts.size == 2) "${parts[0]} → ${parts[1]}".uppercase() else levelUid.uppercase()
    }

    private fun displayName(rawName: String): String = when (classifyDojoSection(rawName)) {
        DojoSection.ENDING_EASY -> "ENDING (EASY)"
        DojoSection.ENDING_MEDIUM -> "ENDING (MEDIUM)"
        DojoSection.ENDING_HARD -> "ENDING (HARD)"
        else -> rawName.uppercase()
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) = Unit
}

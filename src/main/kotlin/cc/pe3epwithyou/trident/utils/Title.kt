package cc.pe3epwithyou.trident.utils

import net.minecraft.network.chat.Component

object Title {
    fun sendTitle(
        title: Component,
        subtitle: Component,
        fadeIn: Int,
        stay: Int,
        fadeOut: Int,
        resetTime: Boolean = true
    ) {
        minecraft().gui.hud.setTimes(fadeIn, stay, fadeOut)
        minecraft().gui.hud.setSubtitle(subtitle)
        minecraft().gui.hud.setTitle(title)
        if (resetTime) minecraft().gui.hud.resetTitleTimes()
    }
}
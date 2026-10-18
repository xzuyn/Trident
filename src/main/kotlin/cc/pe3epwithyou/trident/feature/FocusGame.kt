package cc.pe3epwithyou.trident.feature

import cc.pe3epwithyou.trident.config.Config
import cc.pe3epwithyou.trident.events.container.ContainerEvents
import cc.pe3epwithyou.trident.state.Game
import cc.pe3epwithyou.trident.state.MCCIState.isOnIsland
import cc.pe3epwithyou.trident.utils.Logger
import cc.pe3epwithyou.trident.utils.extensions.WindowExtensions
import cc.pe3epwithyou.trident.utils.extensions.WindowExtensions.focusWindowIfInactive
import cc.pe3epwithyou.trident.utils.extensions.WindowExtensions.isMaximized
import cc.pe3epwithyou.trident.utils.minecraft
import org.lwjgl.glfw.GLFW

object FocusGame {
    /**
     * Called the moment the server tells us which game we've just been moved into.
     */
    fun handleGameJoin(joined: Game) {
        if (!Config.Games.autoFocus) return
        if (!isOnIsland()) return
        if (joined == Game.HUB || joined == Game.FISHING) return

        // Window calls (GLFW) must run on the client thread
        minecraft().execute {
            Logger.debugLog(
                """
                    isActive: ${WindowExtensions.isActive}
                    isMaximized: ${minecraft().window.isMaximized}

                    IconifiedValue: ${
                    GLFW.glfwGetWindowAttrib(
                        minecraft().window.handle(),
                        GLFW.GLFW_ICONIFIED
                    )
                }
                    MaximizedValue: ${
                    GLFW.glfwGetWindowAttrib(
                        minecraft().window.handle(),
                        GLFW.GLFW_MAXIMIZED
                    )
                }
                """.trimIndent()
            )

            minecraft().window.focusWindowIfInactive()
        }
    }

    fun register() {
        ContainerEvents.onOpen {
            requireTitle("MATCH FOUND! (0/")
            minecraft().window.focusWindowIfInactive()
        }
    }
}
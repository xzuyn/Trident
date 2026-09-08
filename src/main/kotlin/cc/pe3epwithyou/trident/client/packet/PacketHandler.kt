package cc.pe3epwithyou.trident.client.packet

import net.minecraft.network.protocol.Packet
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Simple interface for client bound packet handlers inside of Trident.
 * Handlers are added in [PacketManager] in the handlerList
 */
interface PacketHandler {
    /**
     * Checks whether the packet type is correct
     */
    fun check(packet: Packet<*>): Boolean

    /**
     * Handles the required packet
     */
    fun handle(packet: Packet<*>, ci: CallbackInfo)
}
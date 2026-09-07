package cc.pe3epwithyou.trident.client.packet

import cc.pe3epwithyou.trident.feature.friends.FriendsInServer
import cc.pe3epwithyou.trident.utils.Logger
import cc.pe3epwithyou.trident.utils.SuggestionPacket
import cc.pe3epwithyou.trident.utils.minecraft
import net.minecraft.network.protocol.Packet
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object PacketManager {
    private val handlerList = buildList {
        add(FriendsInServer.TabListPacketHandler())
        add(SuggestionPacket.CommandSuggestionsPacketHandler())
    }

    @JvmStatic
    fun processMinecraftPacket(packet: Packet<*>, ci: CallbackInfo) {
        minecraft().execute {
            handlerList.forEach {
                try {
                    if (it.check(packet)) it.handle(packet, ci)
                } catch (e: Throwable) {
                    Logger.error("Error processing minecraft packet.", e)
                }
            }
        }
    }
}
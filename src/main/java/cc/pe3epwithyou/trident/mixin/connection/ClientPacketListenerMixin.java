package cc.pe3epwithyou.trident.mixin.connection;

import cc.pe3epwithyou.trident.feature.FocusGame;
import cc.pe3epwithyou.trident.feature.dojo.DojoSplitTimer;
import cc.pe3epwithyou.trident.feature.fishing.WayfinderModule;
import cc.pe3epwithyou.trident.feature.questing.QuestListener;
import cc.pe3epwithyou.trident.state.Game;
import cc.pe3epwithyou.trident.state.MCCIState;
import cc.pe3epwithyou.trident.utils.ScreenManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "setSubtitleText", at = @At("TAIL"))
    private void trident$setSubtitleText(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        FocusGame.INSTANCE.handleSubtitle(packet.text().getString());
    }

    // Runs before the vanilla subtitle is actually displayed, so the Dojo split timer can
    // rewrite the subtitle (to append a split improvement indicator) and cancel the vanilla
    // display when needed.
    @Inject(method = "setSubtitleText", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void injectDojoSubtitle(ClientboundSetSubtitleTextPacket clientboundSetSubtitleTextPacket, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        if (MCCIState.INSTANCE.getGame() != Game.PARKOUR_WARRIOR_DOJO) return;
        DojoSplitTimer instance = DojoSplitTimer.Companion.getInstance();
        if (instance == null) return;
        instance.handleSubtitle(clientboundSetSubtitleTextPacket, ci);
    }

    @Inject(method = "handleSoundEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void injectHandleSoundEvent(ClientboundSoundPacket clientboundSoundPacket, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        if (MCCIState.INSTANCE.getGame() != Game.PARKOUR_WARRIOR_DOJO) return;
        DojoSplitTimer.onSound(clientboundSoundPacket);
    }

    // "Run Complete!" is delivered as a title (the big banner), not the subtitle used for
    // level names/medals, and unlike sound-based signals it can only appear once the final
    // medal has actually been processed, so it doesn't race with it.
    @Inject(method = "setTitleText", at = @At("TAIL"))
    private void injectSetTitleText(ClientboundSetTitleTextPacket clientboundSetTitleTextPacket, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        if (MCCIState.INSTANCE.getGame() != Game.PARKOUR_WARRIOR_DOJO) return;
        DojoSplitTimer instance = DojoSplitTimer.Companion.getInstance();
        if (instance == null) return;
        instance.handleTitle(clientboundSetTitleTextPacket.text().getString());
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void trident$handleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        QuestListener.INSTANCE.handleRefreshTasksItem(packet.getItem());
        ScreenManager.setWaiting(false);
    }

    @Inject(method = "handleBossUpdate", at = @At("TAIL"))
    private void trident$handleBossUpdate(ClientboundBossEventPacket packet, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        WayfinderModule.INSTANCE.handleBossbarEvent();
    }
}


package cc.pe3epwithyou.trident.mixin.screen;

import cc.pe3epwithyou.trident.config.Config;
import cc.pe3epwithyou.trident.events.click.ClickEvents;
import cc.pe3epwithyou.trident.events.click.ContainerClickContext;
import cc.pe3epwithyou.trident.events.container.ContainerContext;
import cc.pe3epwithyou.trident.events.container.ContainerEvents;
import cc.pe3epwithyou.trident.feature.EnhancedCompactInfinibag;
import cc.pe3epwithyou.trident.feature.chat.chatroom.Chatrooms;
import cc.pe3epwithyou.trident.feature.doll.Doll;
import cc.pe3epwithyou.trident.feature.exchange.ExchangeHandler;
import cc.pe3epwithyou.trident.feature.fishing.TideWindIndicator;
import cc.pe3epwithyou.trident.feature.indicators.BlueprintIndicator;
import cc.pe3epwithyou.trident.feature.indicators.CraftableIndicator;
import cc.pe3epwithyou.trident.feature.indicators.UpgradeIndicator;
import cc.pe3epwithyou.trident.feature.questing.lock.QuestLock;
import cc.pe3epwithyou.trident.feature.rarityslot.RaritySlot;
import cc.pe3epwithyou.trident.interfaces.exchange.ExchangeFilter;
import cc.pe3epwithyou.trident.interfaces.fishing.AugmentStatusInterface;
import cc.pe3epwithyou.trident.state.MCCIState;
import cc.pe3epwithyou.trident.utils.DebugDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin extends Screen {
    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    protected AbstractContainerScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "extractSlot", at = @At(value = "HEAD"))
    public void trident$extractSlotHead(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        RaritySlot.INSTANCE.render(graphics, slot);
        TideWindIndicator.INSTANCE.renderOutline(graphics, slot);
    }

    @Inject(method = "extractSlot", at = @At(value = "TAIL"))
    public void trident$extractSlotTail(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        if (Config.Global.INSTANCE.getBlueprintIndicators()) {
            BlueprintIndicator.checkItem(graphics, slot);
        }
        if (Config.Debug.INSTANCE.getDrawSlotNumber()) {
            DebugDraw.INSTANCE.renderSlotNumber(graphics, slot);
        }
        if (Config.Global.INSTANCE.getUpgradeIndicators()) {
            UpgradeIndicator.INSTANCE.render(graphics, slot);
        }
        TideWindIndicator.INSTANCE.render(graphics, slot);
        CraftableIndicator.INSTANCE.render(graphics, slot);
        if (Config.Global.INSTANCE.getExchangeImprovements()) {
            ExchangeHandler.INSTANCE.renderSlot(graphics, slot);
        }
        AugmentStatusInterface.render(graphics, slot);
        QuestLock.renderLock(graphics, slot);
        Doll.renderSlot(graphics, slot);
        Chatrooms.renderPinIcon(graphics, slot);
        EnhancedCompactInfinibag.render(graphics, slot);
    }

    @Inject(method = "onClose", at = @At(value = "HEAD"))
    public void trident$onClose(CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        if (minecraft.gui.screen() instanceof ContainerScreen s) {
            ContainerEvents.INSTANCE.getCLOSE().invoker().invoke(new ContainerContext(s));
        }
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    public void trident$extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            if (!ExchangeHandler.INSTANCE.shouldRenderTooltip(hoveredSlot)) ci.cancel();
        }
    }

    @Inject(method = "extractContents", at = @At(value = "TAIL"))
    public void trident$extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        if (minecraft.gui.screen() instanceof ContainerScreen s) {
            if (s.getTitle().getString().contains("ISLAND EXCHANGE")) {
                ExchangeHandler.INSTANCE.renderBackground(graphics, leftPos, topPos);
            }
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void trident$init(CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        String screenTitle = this.getTitle().getString();
        if (minecraft.gui.screen() instanceof ContainerScreen screen) {
            ContainerEvents.INSTANCE.getINIT().invoker().invoke(new ContainerContext(screen));
            if (screenTitle.contains("ISLAND EXCHANGE") && Config.Global.INSTANCE.getExchangeImprovements()) {
                int x = this.leftPos + 32;
                int y = this.topPos - 33;
                this.addRenderableWidget(new ExchangeFilter(x, y));
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void trident$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        ContainerScreen containerScreen = minecraft.gui.screen() instanceof ContainerScreen s ? s : null;
        if (containerScreen == null) return;
        ClickEvents.INSTANCE.getCLICK().invoker().invoke(new ContainerClickContext(doubleClick, containerScreen, event, cir));
    }

    @Inject(method = "extractContents", at = @At("HEAD"))
    public void trident$extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        Doll.render(graphics);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"))
    public void trident$mouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        Doll.rotateDoll((float) dy, (float) dx);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    public void trident$mouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        Doll.onReleased();
    }

}

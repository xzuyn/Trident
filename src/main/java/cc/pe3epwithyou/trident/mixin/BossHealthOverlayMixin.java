package cc.pe3epwithyou.trident.mixin;

import cc.pe3epwithyou.trident.feature.dojo.DojoSplitBar;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

/**
 * The Dojo split timer bar is drawn relative to the boss bars, so it's hooked in here
 * rather than through the regular HUD render pass.
 */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
    @Shadow
    @Final
    Map<UUID, LerpingBossEvent> events;

    @Inject(method = "render", at = @At("HEAD"))
    private void injectRender(GuiGraphicsExtractor guiGraphics, CallbackInfo ci) {
        DojoSplitBar.render(guiGraphics, events.size());
    }
}

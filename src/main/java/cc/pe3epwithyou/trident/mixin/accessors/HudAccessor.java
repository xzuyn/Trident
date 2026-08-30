package cc.pe3epwithyou.trident.mixin.accessors;

import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface HudAccessor {
    @Accessor
    @Nullable
    Component getOverlayMessageString();

    @Accessor
    @Nullable
    Component getTitle();
}

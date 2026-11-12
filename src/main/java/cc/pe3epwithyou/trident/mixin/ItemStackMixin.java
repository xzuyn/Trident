package cc.pe3epwithyou.trident.mixin;

import cc.pe3epwithyou.trident.feature.chat.chatroom.Chatrooms;
import cc.pe3epwithyou.trident.feature.crate.CrateChances;
import cc.pe3epwithyou.trident.feature.doll.Doll;
import cc.pe3epwithyou.trident.feature.questing.lock.QuestLock;
import cc.pe3epwithyou.trident.state.MCCIState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/TooltipFlag;isAdvanced()Z", shift = At.Shift.AFTER))
    void trident$addDetailsToTooltip(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> builder, CallbackInfo ci) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        Doll.modifyTooltip(builder);
        QuestLock.modifyTooltip(builder);
        Chatrooms.modifyTooltip(builder);
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    void trident$getTooltipLines(Item.TooltipContext context, @Nullable Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;
        List<Component> lines = new ArrayList<>(cir.getReturnValue());
        if (CrateChances.modifyTooltip(lines)) cir.setReturnValue(lines);
    }
}

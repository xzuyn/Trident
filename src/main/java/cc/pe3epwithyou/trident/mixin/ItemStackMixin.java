package cc.pe3epwithyou.trident.mixin;

import cc.pe3epwithyou.trident.feature.chat.chatroom.Chatrooms;
import cc.pe3epwithyou.trident.feature.doll.Doll;
import cc.pe3epwithyou.trident.feature.questing.lock.QuestLock;
import cc.pe3epwithyou.trident.state.MCCIState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "getTooltipLines", at = @At("TAIL"))
    void trident$getTooltipLines(
            Item.TooltipContext context,
            @Nullable Player player,
            TooltipFlag tooltipFlag,
            CallbackInfoReturnable<List<Component>> cir
    ) {
        if (!MCCIState.INSTANCE.isOnIsland()) return;

        List<Component> tooltip = cir.getReturnValue();

        Doll.modifyTooltip(tooltip::add);
        QuestLock.modifyTooltip(tooltip::add);
        Chatrooms.modifyTooltip(tooltip::add);
    }
}

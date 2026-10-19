package dev.rpgprogression.mixin;

import dev.rpgprogression.stamina.RangedStaminaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class CrossbowStaminaMixin {
    @Inject(method="tryLoadProjectiles",at=@At("HEAD"),cancellable=true)
    private static void rpg$checkLoading(LivingEntity entity,ItemStack crossbow,CallbackInfoReturnable<Boolean> ci){
        if(entity instanceof ServerPlayer player&&!RangedStaminaManager.can(player,"crossbow"))ci.setReturnValue(false);
    }
    @Inject(method="tryLoadProjectiles",at=@At("RETURN"))
    private static void rpg$payLoading(LivingEntity entity,ItemStack crossbow,CallbackInfoReturnable<Boolean> ci){
        if(ci.getReturnValueZ()&&entity instanceof ServerPlayer player)RangedStaminaManager.spend(player,"crossbow");
    }
}

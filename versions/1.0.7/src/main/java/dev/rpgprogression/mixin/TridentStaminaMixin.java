package dev.rpgprogression.mixin;

import dev.rpgprogression.stamina.RangedStaminaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentItem.class)
public abstract class TridentStaminaMixin {
    @Inject(method="releaseUsing",at=@At("HEAD"),cancellable=true)
    private void rpg$checkThrow(ItemStack stack,Level level,LivingEntity entity,int remaining,CallbackInfo ci){
        if(!(entity instanceof ServerPlayer player)||stack.getUseDuration()-remaining<10)return;
        int riptide=EnchantmentHelper.getRiptide(stack);if(riptide>0&&!player.isInWaterOrRain())return;
        if(!RangedStaminaManager.can(player,"spear")){ci.cancel();return;}
        // Riptide never creates a projectile; normal throws pay when that projectile is created.
        if(riptide>0)RangedStaminaManager.spend(player,"spear");
    }
}

package dev.rpgprogression.mixin;

import dev.rpgprogression.defense.EquipmentDefense;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep native, data-driven protection effects, but exclude ordinary armor from magic defense. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentArmorMixin {
    @Redirect(method="getDamageProtection",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;runIterationOnEquipment(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V"))
    private static void rpg$armorProtection(LivingEntity target,EnchantmentHelper.EnchantmentInSlotVisitor visitor,ServerLevel level,LivingEntity entity,DamageSource source){
        if(!(target instanceof Player)||!EquipmentDefense.magical(source)){EnchantmentHelper.runIterationOnEquipment(target,visitor);return;}
        for(EquipmentSlot slot:EquipmentSlot.values()){
            var stack=target.getItemBySlot(slot);
            if(slot.getType()==EquipmentSlot.Type.HUMANOID_ARMOR&&EquipmentDefense.ironsArmor(stack))EnchantmentHelper.runIterationOnItem(stack,slot,target,visitor);
        }
    }
}

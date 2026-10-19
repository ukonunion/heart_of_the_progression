package dev.rpgprogression.mixin;

import dev.rpgprogression.defense.EquipmentDefense;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ArmorDefenseMixin {
    @Inject(method="getDamageAfterArmorAbsorb",at=@At("HEAD"),cancellable=true)
    private void rpg$magicArmor(DamageSource source,float damage,CallbackInfoReturnable<Float> cir){
        if(!((Object)this instanceof Player p)||!EquipmentDefense.magical(source))return;
        float result=EquipmentDefense.afterArmor(p,source,damage);
        // Magic wears only armor that actually supplies magic protection. Ordinary armor is untouched.
        if(!source.is(DamageTypeTags.BYPASSES_EFFECTS)&&EquipmentDefense.armorAttribute(p,Attributes.ARMOR,true)>0){
            int wear=Math.max(1,(int)(damage/4));
            for(var stack:p.getArmorSlots())if(EquipmentDefense.ironsArmor(stack)&&(!source.is(DamageTypeTags.IS_FIRE)||!stack.getItem().isFireResistant())){
                var slot=((ArmorItem)stack.getItem()).getEquipmentSlot();stack.hurtAndBreak(wear,p,entity->entity.broadcastBreakEvent(slot));
            }
        }cir.setReturnValue(result);
    }
    @ModifyArg(method="getDamageAfterArmorAbsorb",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"),index=1)
    private float rpg$physicalArmor(float original){return (Object)this instanceof Player p?(float)Math.floor(EquipmentDefense.armorAttribute(p,Attributes.ARMOR,false)):original;}
    @ModifyArg(method="getDamageAfterArmorAbsorb",at=@At(value="INVOKE",target="Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"),index=2)
    private float rpg$physicalToughness(float original){return (Object)this instanceof Player p?(float)EquipmentDefense.armorAttribute(p,Attributes.ARMOR_TOUGHNESS,false):original;}
    @Redirect(method="getDamageAfterMagicAbsorb",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageProtection(Ljava/lang/Iterable;Lnet/minecraft/world/damagesource/DamageSource;)I"))
    private int rpg$enchantmentProtection(Iterable<ItemStack> stacks,DamageSource source){return (Object)this instanceof Player?EquipmentDefense.protection(stacks,source):EnchantmentHelper.getDamageProtection(stacks,source);}
}

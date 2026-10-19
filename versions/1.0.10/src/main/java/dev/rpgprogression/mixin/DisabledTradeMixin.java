package dev.rpgprogression.mixin;

import dev.rpgprogression.compat.DisabledIronsWeapons;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class DisabledTradeMixin {
    @Inject(method="getOffers",at=@At("RETURN"))
    private void rpg$offers(CallbackInfoReturnable<MerchantOffers> cir){cir.getReturnValue().removeIf(o->o.getResult().isEmpty()||o.getBaseCostA().isEmpty()||DisabledIronsWeapons.blocked(o.getResult())||DisabledIronsWeapons.blocked(o.getBaseCostA())||DisabledIronsWeapons.blocked(o.getCostB()));}
}

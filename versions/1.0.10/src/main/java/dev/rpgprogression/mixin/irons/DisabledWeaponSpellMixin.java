package dev.rpgprogression.mixin.irons;

import dev.rpgprogression.compat.DisabledIronsWeapons;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(value=AbstractSpell.class,remap=false)
public abstract class DisabledWeaponSpellMixin {
    @Inject(method="canBeCastedBy",at=@At("HEAD"),cancellable=true)
    private void rpg$blockedWeapon(int level,CastSource source,MagicData magic,Player caster,CallbackInfoReturnable<CastResult> cir){
        if(source==CastSource.SWORD&&(DisabledIronsWeapons.blocked(caster.getMainHandItem())||DisabledIronsWeapons.blocked(caster.getOffhandItem())))cir.setReturnValue(new CastResult(CastResult.Type.FAILURE,net.minecraft.network.chat.Component.translatable("rpgprogression.item.weapon_disabled")));
    }
}

package dev.rpgprogression.mixin.irons;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.resource.ResourceMath;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** One conversion is shared by checks, debits, channeling and native tooltips. */
@Pseudo @Mixin(value=AbstractSpell.class,remap=false)
public abstract class SpellCostMixin {
    @Inject(method="getManaCost",at=@At("RETURN"),cancellable=true)
    private void rpg$cost(int level,CallbackInfoReturnable<Integer> cir){
        if(IronsCompat.enabled())cir.setReturnValue(ResourceMath.spellCost(cir.getReturnValue()));
    }
}

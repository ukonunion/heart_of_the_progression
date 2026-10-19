package dev.rpgprogression.mixin.irons;

import dev.rpgprogression.compat.IronsRequirements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu",remap=false)
public abstract class ArcaneAnvilRequirementsMixin {
    @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
    private void rpg$useInputs(Player player,boolean hasStack,CallbackInfoReturnable<Boolean> cir){
        if(!IronsRequirements.menuAllowed(player,(AbstractContainerMenu)(Object)this,2))cir.setReturnValue(false);
    }
}

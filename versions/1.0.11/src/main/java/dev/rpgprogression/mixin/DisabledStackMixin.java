package dev.rpgprogression.mixin;

import dev.rpgprogression.compat.DisabledIronsWeapons;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Purge saved stacks in players, chests, ender chests, mobs and item entities on NBT load. */
@Mixin(ItemStack.class)
public abstract class DisabledStackMixin {
    @Inject(method="of",at=@At("RETURN"),cancellable=true)
    private static void rpg$read(CompoundTag nbt,CallbackInfoReturnable<ItemStack> cir){if(DisabledIronsWeapons.blocked(cir.getReturnValue()))cir.setReturnValue(ItemStack.EMPTY);}
}

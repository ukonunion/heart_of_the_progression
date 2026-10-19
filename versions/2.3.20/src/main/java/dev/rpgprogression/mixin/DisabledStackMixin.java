package dev.rpgprogression.mixin;

import dev.rpgprogression.compat.DisabledIronsWeapons;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Purge saved stacks in players, chests, ender chests, mobs and item entities on NBT load. */
@Mixin(ItemStack.class)
public abstract class DisabledStackMixin {
    @Inject(method="parse",at=@At("RETURN"),cancellable=true)
    private static void rpg$read(HolderLookup.Provider registries,Tag nbt,CallbackInfoReturnable<Optional<ItemStack>> cir){if(cir.getReturnValue().filter(DisabledIronsWeapons::blocked).isPresent())cir.setReturnValue(Optional.empty());}
    @Inject(method="parseOptional",at=@At("RETURN"),cancellable=true)
    private static void rpg$readOptional(HolderLookup.Provider registries,CompoundTag nbt,CallbackInfoReturnable<ItemStack> cir){if(DisabledIronsWeapons.blocked(cir.getReturnValue()))cir.setReturnValue(ItemStack.EMPTY);}
}

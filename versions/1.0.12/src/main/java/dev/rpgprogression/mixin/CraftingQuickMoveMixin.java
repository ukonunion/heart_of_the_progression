package dev.rpgprogression.mixin;

import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import dev.rpgprogression.recipe.CraftingResultAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shift-craft can be called directly and vanilla does not check ResultSlot.mayPickup. */
@Mixin({CraftingMenu.class,InventoryMenu.class})
public abstract class CraftingQuickMoveMixin {
    @Inject(method="quickMoveStack",at=@At("HEAD"),cancellable=true)
    private void rpg$quickCraft(Player p,int index,CallbackInfoReturnable<ItemStack> cir) {
        if(index==0&&((AbstractContainerMenu)(Object)this).getSlot(0) instanceof CraftingResultAccess access&&!access.rpg$allowed(p))cir.setReturnValue(ItemStack.EMPTY);
    }
}

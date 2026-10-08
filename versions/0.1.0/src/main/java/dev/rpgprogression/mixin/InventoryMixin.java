package dev.rpgprogression.mixin;

import dev.rpgprogression.inventory.InventorySlotRules;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Item pickups and /give insert directly into Inventory, bypassing menu Slot methods. */
@Mixin(Inventory.class)
public abstract class InventoryMixin implements net.minecraft.world.Container {
    @Shadow @Final public Player player;
    @Shadow public int selected;
    @Shadow public abstract ItemStack getItem(int index);
    @Shadow private boolean hasRemainingSpaceForItem(ItemStack target,ItemStack incoming) {throw new AssertionError();}
    @Override public boolean canPlaceItem(int index,ItemStack stack) {return !InventorySlotRules.locked(player,index);}
    @Inject(method="getFreeSlot",at=@At("HEAD"),cancellable=true)
    private void rpg$free(CallbackInfoReturnable<Integer> cir) {
        for(int i=0;i<9+InventorySlotRules.unlocked(player);i++)if(getItem(i).isEmpty()){cir.setReturnValue(i);return;}
        cir.setReturnValue(-1);
    }
    @Inject(method="getSlotWithRemainingSpace",at=@At("HEAD"),cancellable=true)
    private void rpg$merge(ItemStack stack,CallbackInfoReturnable<Integer> cir) {
        if(hasRemainingSpaceForItem(getItem(selected),stack)){cir.setReturnValue(selected);return;}
        if(hasRemainingSpaceForItem(getItem(40),stack)){cir.setReturnValue(40);return;}
        for(int i=0;i<9+InventorySlotRules.unlocked(player);i++)if(hasRemainingSpaceForItem(getItem(i),stack)){cir.setReturnValue(i);return;}
        cir.setReturnValue(-1);
    }
    @Inject(method="add(ILnet/minecraft/world/item/ItemStack;)Z",at=@At("HEAD"),cancellable=true)
    private void rpg$directAdd(int index,ItemStack stack,CallbackInfoReturnable<Boolean> cir) {
        if(InventorySlotRules.locked(player,index))cir.setReturnValue(false);
    }
    @Inject(method="pickSlot",at=@At("HEAD"),cancellable=true)
    private void rpg$pick(int index,CallbackInfo ci) {if(InventorySlotRules.locked(player,index))ci.cancel();}
    @Inject(method="findSlotMatchingUnusedItem",at=@At("HEAD"),cancellable=true)
    private void rpg$recipeIngredient(ItemStack wanted,CallbackInfoReturnable<Integer> cir) {
        for(int i=0;i<9+InventorySlotRules.unlocked(player);i++) {
            ItemStack item=getItem(i);
            if(!item.isEmpty()&&ItemStack.isSameItemSameTags(wanted,item)&&!item.isDamaged()&&!item.isEnchanted()&&!item.hasCustomHoverName()){cir.setReturnValue(i);return;}
        }
        cir.setReturnValue(-1);
    }
    @Inject(method="fillStackedContents",at=@At("HEAD"),cancellable=true)
    private void rpg$recipeContents(StackedContents contents,CallbackInfo ci) {
        for(int i=0;i<9+InventorySlotRules.unlocked(player);i++)contents.accountSimpleStack(getItem(i));
        ci.cancel();
    }
}

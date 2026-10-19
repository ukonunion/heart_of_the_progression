package dev.rpgprogression.mixin;

import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shared by the vanilla 2x2 and 3x3 grids; clears the result BEFORE any item can be taken. */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method="slotChangedCraftingGrid",at=@At("HEAD"),cancellable=true)
    private static void rpg$result(AbstractContainerMenu menu,Level level,Player player,CraftingContainer grid,ResultContainer result,CallbackInfo ci) {
        if(!level.isClientSide&&!IntelligenceRecipeManager.allowedCraft(player,grid)) {
            result.setItem(0,ItemStack.EMPTY); menu.setRemoteSlot(0,ItemStack.EMPTY);
            if(player instanceof ServerPlayer p)p.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId,menu.incrementStateId(),0,ItemStack.EMPTY));
            ci.cancel();
        }
    }
}

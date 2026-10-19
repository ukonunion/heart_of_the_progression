package dev.rpgprogression.mixin;

import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import dev.rpgprogression.recipe.CraftingResultAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;

/** Exposes only the crafting grid guard to the common slot/click checks. */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin implements CraftingResultAccess {
    @Shadow @Final private CraftingContainer craftSlots;
    @Override public boolean rpg$allowed(Player player) { return IntelligenceRecipeManager.allowedCraft(player,craftSlots); }
}

package dev.rpgprogression.mixin;

import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.Collection;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    /** Vanilla advancements otherwise rediscover locked recipes immediately after login. */
    @ModifyVariable(method="awardRecipes",at=@At("HEAD"),argsOnly=true)
    private Collection<Recipe<?>> rpg$recipes(Collection<Recipe<?>> recipes) {
        return IntelligenceRecipeManager.filterAllowed((ServerPlayer)(Object)this,recipes);
    }
}

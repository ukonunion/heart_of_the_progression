package dev.rpgprogression.mixin;

import com.google.gson.JsonElement;
import dev.rpgprogression.compat.DisabledIronsWeapons;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;
import java.util.stream.StreamSupport;

@Mixin(RecipeManager.class)
public abstract class DisabledRecipeMixin {
    @Inject(method="apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",at=@At("RETURN"))
    private void rpg$removeWeapons(Map<ResourceLocation,JsonElement> files,ResourceManager resources,ProfilerFiller profiler,CallbackInfo ci){
        var manager=(RecipeManager)(Object)this;
        if(manager.getRecipes().stream().anyMatch(DisabledIronsWeapons::recipe))manager.replaceRecipes(manager.getRecipes());
    }
    @ModifyVariable(method="replaceRecipes",at=@At("HEAD"),argsOnly=true)
    private Iterable<Recipe<?>> rpg$filterOutputs(Iterable<Recipe<?>> recipes){
        return StreamSupport.stream(recipes.spliterator(),false).filter(r->!DisabledIronsWeapons.recipe(r)).toList();
    }
}

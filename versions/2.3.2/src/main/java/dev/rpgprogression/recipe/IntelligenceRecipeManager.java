package dev.rpgprogression.recipe;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.inventory.CraftingContainer;
import java.util.*;

public final class IntelligenceRecipeManager extends SimpleJsonResourceReloadListener {
    public static final IntelligenceRecipeManager INSTANCE=new IntelligenceRecipeManager();
    private static volatile Map<ResourceLocation,Integer> requirements=Map.of();
    public static volatile boolean reloadPending=false;
    private IntelligenceRecipeManager() { super(new Gson(),"recipe_requirements"); }
    @Override protected void apply(Map<ResourceLocation,JsonElement> files,ResourceManager rm,ProfilerFiller profiler) {
        Map<ResourceLocation,Integer> next=new HashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try {
                JsonObject obj=entry.getValue().getAsJsonObject();
                for(var rule:obj.entrySet()) {
                    ResourceLocation id=ResourceLocation.parse(rule.getKey()); int value=rule.getValue().getAsInt();
                    if(value<0||value>10000)throw new IllegalArgumentException("Requirement outside 0..10000");
                    next.put(id,value);
                }
            } catch(RuntimeException e) { LogUtils.getLogger().error("Invalid RPG recipe requirements in {}",entry.getKey(),e); }
        });
        requirements=Map.copyOf(next); reloadPending=true;
    }
    public static boolean allowed(Player p,ResourceLocation id) {
        if(p.isCreative()||p.isSpectator())return true;
        int required=requirements.getOrDefault(id,0);
        return PlayerProgressionProvider.get(p).map(d->d.get(RPGAttribute.INTELLIGENCE)>=required).orElse(required==0);
    }
    public static boolean allowedCraft(Player p,CraftingContainer grid) {
        return p.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid.asCraftInput(),p.level()).map(r->allowed(p,r.id())).orElse(true);
    }
    public static void updateBook(ServerPlayer p,int oldInt,boolean notify) {
        List<RecipeHolder<?>> add=new ArrayList<>(),remove=new ArrayList<>();
        for(var entry:requirements.entrySet())p.server.getRecipeManager().byKey(entry.getKey()).ifPresent(r->{
            if(allowed(p,entry.getKey())) {
                add.add(r);
                if(notify&&oldInt<entry.getValue())ModNetworking.recipeUnlocked(p,r.value().getResultItem(p.level().registryAccess()).getHoverName());
            }else remove.add(r);
        });
        if(!remove.isEmpty())p.resetRecipes(remove);
        if(!add.isEmpty()) {
            if(notify)p.awardRecipes(add);
            else {
                // INIT refreshes book contents without ADD toasts for every recipe on login/reload.
                for(RecipeHolder<?> recipe:add){p.getRecipeBook().add(recipe);p.getRecipeBook().removeHighlight(recipe);}
                p.getRecipeBook().sendInitialRecipeBook(p);
            }
        }
    }
    private static boolean applies(RecipeHolder<?> r) { return requirements.containsKey(r.id()); }
    /** Prevent recipe-book auto-unlocks (advancements / pickups) from revealing gated recipes. */
    public static Collection<RecipeHolder<?>> filterAllowed(Player p,Collection<RecipeHolder<?>> recipes) {
        return recipes.stream().filter(r->!applies(r)||allowed(p,r.id())).toList();
    }
}

package dev.rpgprogression.food;

import dev.rpgprogression.attribute.AttributeManager;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;

/** Food benefits are server-owned timers, not removable potion effects. */
public final class FoodManager {
    public static final TagKey<Item> HEALING=tag("healing_foods"), SOUPS=tag("soups"), ENERGY=tag("energizing_foods");
    private static TagKey<Item> tag(String name){return TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("rpgprogression",name));}
    public static boolean golden(ItemStack stack){return stack.is(Items.GOLDEN_APPLE)||stack.is(Items.ENCHANTED_GOLDEN_APPLE);}
    public static String category(ItemStack stack){
        if(stack.is(Items.ENCHANTED_GOLDEN_APPLE))return "enchanted_apple";
        if(stack.is(Items.GOLDEN_APPLE))return "golden_apple";
        if(stack.is(Items.CHORUS_FRUIT))return "chorus";
        if(stack.is(SOUPS))return "soup";
        if(stack.is(ENERGY))return "energy";
        if(stack.is(HEALING))return "healing";
        return "none";
    }
    /** Called at actual Player.eat completion, before vanilla shrinks the stack. */
    public static void consume(ServerPlayer p,ItemStack stack){
        if(!p.isAlive())return;
        PlayerProgressionProvider.get(p).ifPresent(d->{
            switch(category(stack)){
                case "enchanted_apple" -> d.enchantedFoodTicks=ticks(RPGConfig.ENCHANTED_FOOD_SECONDS.get());
                case "golden_apple" -> d.goldenFoodTicks=ticks(RPGConfig.GOLDEN_FOOD_SECONDS.get());
                case "soup" -> d.soupFoodTicks=ticks(RPGConfig.SOUP_FOOD_SECONDS.get());
                case "energy" -> d.energyFoodTicks=ticks(RPGConfig.ENERGY_FOOD_SECONDS.get());
                case "chorus" -> {d.chorusFoodStacks=Math.min(4,d.chorusFoodStacks+1);d.chorusFoodTicks=ticks(RPGConfig.CHORUS_FOOD_SECONDS.get());}
                case "healing" -> {var food=stack.getFoodProperties(p);if(food!=null)p.heal(food.nutrition()*.5f);}
                default -> {}
            }
            d.dirty=true;AttributeManager.apply(p,d);
        });
    }
    public static void cake(ServerPlayer p){
        PlayerProgressionProvider.get(p).ifPresent(d->{d.energyFoodTicks=ticks(RPGConfig.ENERGY_FOOD_SECONDS.get());d.dirty=true;AttributeManager.apply(p,d);});
    }
    private static int ticks(double seconds){return Math.max(1,(int)Math.ceil(seconds*20));}
    public static double protection(PlayerProgression d){return d.enchantedFoodTicks>0?25:d.goldenFoodTicks>0?12.5:d.soupFoodTicks>0?6.25:0;}
    public static double energyMultiplier(PlayerProgression d){return d.energyFoodTicks>0?1+RPGConfig.ENERGY_FOOD_BONUS.get():1;}
    public static double sparkMultiplier(PlayerProgression d){return d.chorusFoodTicks>0?Math.pow(1.5,Math.min(4,d.chorusFoodStacks)):1;}
    public static void clear(PlayerProgression d){d.soupFoodTicks=d.goldenFoodTicks=d.enchantedFoodTicks=d.energyFoodTicks=d.chorusFoodTicks=d.chorusFoodStacks=0;d.dirty=true;}
    public static void tick(ServerPlayer p,PlayerProgression d){
        p.removeEffect(MobEffects.HUNGER);
        if(d.soupFoodTicks>0&&--d.soupFoodTicks==0)d.dirty=true;
        if(d.goldenFoodTicks>0&&--d.goldenFoodTicks==0)d.dirty=true;
        if(d.enchantedFoodTicks>0&&--d.enchantedFoodTicks==0)d.dirty=true;
        if(d.energyFoodTicks>0&&--d.energyFoodTicks==0){d.dirty=true;AttributeManager.apply(p,d);}
        if(d.chorusFoodTicks>0&&--d.chorusFoodTicks==0){d.chorusFoodStacks=0;d.dirty=true;}
        if(++d.healthRegenTicks>=20){d.healthRegenTicks=0;if(p.isAlive()&&!p.isSpectator()&&p.getHealth()<p.getMaxHealth())p.heal(RPGConfig.PASSIVE_HEALING.get().floatValue());}
    }
    private FoodManager(){}
}

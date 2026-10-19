package dev.rpgprogression.gametest.client;

import dev.rpgprogression.compat.EpicFightBridge;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.requirements.ItemCombatStats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/** Separate class so a standalone client never needs to resolve optional Epic Fight types. */
public final class ItemTooltipEpicCheck {
    public static void mode(ServerPlayer player,boolean battle){var patch=EpicFightCapabilities.getEntityPatch(player,PlayerPatch.class);if(battle)patch.toEpicFightMode(true);else patch.toVanillaMode(true);}
    public static void clientMode(Player player,boolean battle){var patch=EpicFightCapabilities.getEntityPatch(player,PlayerPatch.class);if(battle)patch.toEpicFightMode(false);else patch.toVanillaMode(false);}
    private static void near(double actual,double expected,String message){ItemTooltipCapture.check(Math.abs(actual-expected)<.001,message+": "+actual+" == "+expected);}
    public static void costs(ServerPlayer player){
        var patch=EpicFightCapabilities.getEntityPatch(player,PlayerPatch.class);var d=ProgressionManager.get(player);
        var original=player.getMainHandItem().copy();var second=player.getInventory().getItem(1).copy();
        var weight=player.getAttribute(yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes.WEIGHT.get());double originalWeight=weight.getBaseValue();
        var rules=java.util.List.copyOf(RPGConfig.EPICFIGHT_WEAPON_COSTS.get());mode(player,true);
        try{
            weight.setBaseValue(40);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
            var greatsword=player.getInventory().getItem(7);double heavy=ItemCombatStats.attackCost(player,greatsword);
            near(heavy,24,"hovered greatsword cost is independent of the held sword");
            ItemTooltipCapture.check(player.getMainHandItem().is(Items.IRON_SWORD),"preview does not equip the hovered item");
            for(double bodyWeight:new double[]{40,80}){
                weight.setBaseValue(bodyWeight);d.stamina=d.maxStamina();d.exhausted=false;
                double shown=ItemCombatStats.attackCost(player,player.getMainHandItem()),before=d.stamina;
                patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);
                near(before-d.stamina,shown,"tooltip matches native Epic strike at body weight "+bodyWeight);
                near(shown,bodyWeight==40?12:24,"native weight modifier changes the preview");
            }
            weight.setBaseValue(40);
            for(String rule:new String[]{"sword=7.25","sword=0"}){
                RPGConfig.EPICFIGHT_WEAPON_COSTS.set(java.util.List.of(rule));d.stamina=d.maxStamina();d.exhausted=false;
                double shown=ItemCombatStats.attackCost(player,player.getMainHandItem()),before=d.stamina;
                patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);
                near(before-d.stamina,shown,"native Epic strike follows edited cost rule "+rule);
            }
            RPGConfig.EPICFIGHT_WEAPON_COSTS.set(rules);d.stamina=d.maxStamina();d.exhausted=false;
            var bow=new ItemStack(Items.BOW);player.setItemInHand(InteractionHand.MAIN_HAND,bow);player.getInventory().setItem(1,new ItemStack(Items.ARROW,16));player.tickCount++;
            double before=d.stamina,shown=ItemCombatStats.rangedCost(player,bow);
            bow.getItem().releaseUsing(bow,player.level(),player,72000-20);
            near(before-d.stamina,shown,"tooltip shot cost equals a real bow shot");near(shown,8,"bow shot uses its own category");
            player.setItemInHand(InteractionHand.MAIN_HAND,original);player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);before=d.stamina;
            near(ItemCombatStats.attackCost(player,original),0,"creative Epic tooltip reports no stamina use");
            patch.onStrike(yesman.epicfight.gameasset.Animations.SWORD_AUTO1.get(),InteractionHand.MAIN_HAND);near(d.stamina,before,"native Epic creative attack is free");
        }finally{
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);weight.setBaseValue(originalWeight);RPGConfig.EPICFIGHT_WEAPON_COSTS.set(rules);
            player.setItemInHand(InteractionHand.MAIN_HAND,original);player.getInventory().setItem(1,second);d.stamina=d.maxStamina();d.exhausted=false;d.regenCooldown=20000;
            dev.rpgprogression.network.ModNetworking.rules(player);dev.rpgprogression.network.ModNetworking.sync(player);
        }
    }
    private ItemTooltipEpicCheck(){}
}

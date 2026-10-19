package dev.rpgprogression.gametest;

import dev.rpgprogression.compat.EpicFightCompat;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.RangedStaminaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import java.util.function.BiConsumer;

/** Actual vanilla item methods and projectile creation, exercised with and without Epic Fight. */
public final class RangedNativeChecks {
    private static void near(BiConsumer<Boolean,String> check,double value,double expected,String message){check.accept(Math.abs(value-expected)<.001,message+": "+value+" == "+expected);}
    public static void run(ServerPlayer player,BiConsumer<Boolean,String> check){
        var data=ProgressionManager.get(player);var saved=data.save();var inventory=player.getInventory().save(new net.minecraft.nbt.ListTag());int selected=player.getInventory().selected;
        try{
            player.setGameMode(GameType.SURVIVAL);player.getInventory().selected=0;
            if(EpicFightCompat.enabled())RangedEpicSetup.mode(player,false);
            actions(player,check,"normal");
            if(EpicFightCompat.enabled()){RangedEpicSetup.mode(player,true);actions(player,check,"epic");}
        }finally{
            player.setGameMode(GameType.SURVIVAL);if(EpicFightCompat.enabled())RangedEpicSetup.mode(player,false);
            player.getInventory().clearContent();player.getInventory().load(inventory);player.getInventory().selected=selected;data.load(saved);data.regenCooldown=20000;
        }
    }
    private static void ready(ServerPlayer player){var d=ProgressionManager.get(player);d.stamina=d.maxStamina();d.exhausted=false;player.tickCount++;}
    private static void load(ServerPlayer player,ItemStack crossbow,int charge){crossbow.getItem().releaseUsing(crossbow,player.level(),player,crossbow.getUseDuration()-charge);}
    private static void fire(ServerPlayer player,ItemStack crossbow){player.setItemInHand(InteractionHand.MAIN_HAND,crossbow);crossbow.getItem().use(player.level(),player,InteractionHand.MAIN_HAND);}
    private static void actions(ServerPlayer player,BiConsumer<Boolean,String> check,String mode){
        player.getInventory().clearContent();var data=ProgressionManager.get(player);
        var bow=new ItemStack(Items.BOW);player.setItemInHand(InteractionHand.MAIN_HAND,bow);player.getInventory().setItem(1,new ItemStack(Items.ARROW,32));ready(player);
        double before=data.stamina,price=RangedStaminaManager.cost(player,"bow");
        bow.getItem().releaseUsing(bow,player.level(),player,72000-20);near(check,before-data.stamina,price,mode+" bow shot pays once");
        check.accept(player.getInventory().getItem(1).getCount()==31,mode+" bow uses one arrow");
        data.stamina=1;data.exhausted=false;player.tickCount++;bow.getItem().releaseUsing(bow,player.level(),player,72000-20);
        near(check,data.stamina,1,mode+" insufficient bow shot is denied");check.accept(player.getInventory().getItem(1).getCount()==31,mode+" denied bow preserves ammo");
        ready(player);before=data.stamina;bow.getItem().releaseUsing(bow,player.level(),player,72000-1);near(check,data.stamina,before,mode+" too short bow draw is free");

        var crossbow=new ItemStack(Items.CROSSBOW);player.setItemInHand(InteractionHand.MAIN_HAND,crossbow);ready(player);before=data.stamina;
        load(player,crossbow,10);check.accept(!CrossbowItem.isCharged(crossbow),mode+" incomplete crossbow remains empty");near(check,data.stamina,before,mode+" incomplete loading is free");
        data.stamina=1;data.exhausted=false;int arrows=player.getInventory().getItem(1).getCount();load(player,crossbow,25);
        check.accept(!CrossbowItem.isCharged(crossbow)&&player.getInventory().getItem(1).getCount()==arrows,mode+" denied crossbow loading preserves ammo and NBT");near(check,data.stamina,1,mode+" denied loading consumes no stamina");
        ready(player);before=data.stamina;price=RangedStaminaManager.cost(player,"crossbow");load(player,crossbow,25);
        check.accept(CrossbowItem.isCharged(crossbow),mode+" full crossbow charges successfully");near(check,before-data.stamina,price,mode+" full crossbow loading pays once");
        before=data.stamina;load(player,crossbow,25);near(check,data.stamina,before,mode+" already charged crossbow cannot pay twice");
        data.stamina=0;data.exhausted=true;fire(player,crossbow);near(check,data.stamina,0,mode+" charged crossbow fires for free even with no stamina");check.accept(!CrossbowItem.isCharged(crossbow),mode+" free shot unloads the crossbow");

        crossbow=new ItemStack(Items.CROSSBOW);crossbow.enchant(Enchantments.MULTISHOT,1);player.setItemInHand(InteractionHand.MAIN_HAND,crossbow);ready(player);before=data.stamina;arrows=player.getInventory().getItem(1).getCount();load(player,crossbow,25);
        check.accept(crossbow.getTag().getList("ChargedProjectiles",10).size()==3&&player.getInventory().getItem(1).getCount()==arrows-1,mode+" native Multishot loads three projectiles from one arrow");
        near(check,before-data.stamina,price,mode+" Multishot pays only one loading cost");before=data.stamina;fire(player,crossbow);near(check,data.stamina,before,mode+" Multishot firing is free");

        player.getInventory().setItem(1,ItemStack.EMPTY);crossbow=new ItemStack(Items.CROSSBOW);player.setItemInHand(InteractionHand.MAIN_HAND,crossbow);ready(player);before=data.stamina;load(player,crossbow,25);
        check.accept(!CrossbowItem.isCharged(crossbow),mode+" missing ammo cannot charge crossbow");near(check,data.stamina,before,mode+" missing ammo has no stamina cost");
        player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.FIREWORK_ROCKET,3));ready(player);before=data.stamina;load(player,crossbow,25);near(check,before-data.stamina,price,mode+" firework loading pays once");before=data.stamina;fire(player,crossbow);near(check,data.stamina,before,mode+" firework launch does not charge again");

        player.getInventory().clearContent();var trident=new ItemStack(Items.TRIDENT);player.setItemInHand(InteractionHand.MAIN_HAND,trident);ready(player);before=data.stamina;
        trident.getItem().releaseUsing(trident,player.level(),player,72000-5);near(check,data.stamina,before,mode+" too short trident preparation is free");
        data.stamina=1;data.exhausted=false;trident.getItem().releaseUsing(trident,player.level(),player,72000-10);
        near(check,data.stamina,1,mode+" insufficient trident throw is denied");check.accept(player.getMainHandItem()==trident&&trident.getDamageValue()==0,mode+" denied throw preserves trident and durability");
        ready(player);before=data.stamina;price=RangedStaminaManager.cost(player,"spear");trident.getItem().releaseUsing(trident,player.level(),player,72000-10);
        near(check,before-data.stamina,price,mode+" real trident throw pays once");check.accept(player.getMainHandItem().isEmpty(),mode+" successful throw removes exactly its trident");

        player.setGameMode(GameType.CREATIVE);ready(player);before=data.stamina;crossbow=new ItemStack(Items.CROSSBOW);player.setItemInHand(InteractionHand.MAIN_HAND,crossbow);load(player,crossbow,25);
        near(check,data.stamina,before,mode+" creative loading is free");check.accept(CrossbowItem.isCharged(crossbow),mode+" creative charging needs no ammo");player.setGameMode(GameType.SURVIVAL);
    }
    private RangedNativeChecks(){}
}

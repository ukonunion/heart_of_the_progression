package dev.rpgprogression.verification.gametest;

import dev.rpgprogression.attribute.*;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.requirements.ItemCombatStats;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import java.util.function.BiConsumer;

/** Real players, item actions, native optional skills and discounted affordability. */
public final class AttributeBalanceChecks {
    private static void near(BiConsumer<Boolean,String> check,double actual,double expected,String what){check.accept(Math.abs(actual-expected)<.001,what+": "+actual+" == "+expected);}
    public static void run(ServerPlayer p,BiConsumer<Boolean,String> check){
        var d=ProgressionManager.get(p);var saved=d.save();var inventory=p.getInventory().save(new net.minecraft.nbt.ListTag());int selected=p.getInventory().selected;var mode=p.gameMode.getGameModeForPlayer();float health=p.getHealth();
        var pos=p.blockPosition().offset(2,0,0);var oldBlock=p.level().getBlockState(pos);
        try{
            p.setGameMode(GameType.SURVIVAL);if(EpicFightCompat.enabled())RangedEpicSetup.mode(p,false);
            for(var a:RPGAttribute.values())d.set(a,10);d.points=3;
            ProgressionManager.spend(p,RPGAttribute.VITALITY.ordinal());near(check,p.getMaxHealth(),21,"one Vitality purchase gives one HP");
            ProgressionManager.spend(p,RPGAttribute.SPARK_POTENTIAL.ordinal());near(check,d.maxSpark(),26,"one Spark purchase gives one reservoir unit");
            ProgressionManager.spend(p,RPGAttribute.ENDURANCE.ordinal());near(check,StaminaManager.cost(p,12),11.88,"one Endurance purchase reduces price by one percent");
            check.accept(d.points==0,"three purchases consume three points");
            d.set(RPGAttribute.VITALITY,20);AttributeManager.apply(p,d);near(check,p.getMaxHealth(),30,"Vitality 20 gives thirty HP");
            d.set(RPGAttribute.SPARK_POTENTIAL,20);near(check,d.maxSpark(),35,"Spark Potential 20 gives thirty-five capacity");near(check,d.baseMana(),2,"mana thresholds are preserved");
            d.set(RPGAttribute.ENDURANCE,40);d.stamina=100;d.exhausted=false;
            near(check,ItemCombatStats.attackCost(p,new ItemStack(Items.DIAMOND_SWORD)),8.4,"melee preview includes the thirty percent discount");
            near(check,ItemCombatStats.rangedCost(p,new ItemStack(Items.BOW)),5.6,"bow preview includes the discount");
            near(check,ItemCombatStats.rangedCost(p,new ItemStack(Items.CROSSBOW)),7,"crossbow preview includes the discount");
            near(check,ItemCombatStats.rangedCost(p,new ItemStack(Items.TRIDENT)),9.8,"trident preview includes the discount");
            var target=new net.minecraft.world.entity.animal.Cow(net.minecraft.world.entity.EntityType.COW,p.level());d.attackCooldown=0;
            NeoForge.EVENT_BUS.post(new AttackEntityEvent(p,target));near(check,d.stamina,91.6,"ordinary melee pays the discounted price once");
            d.stamina=100;p.setSprinting(true);for(int i=0;i<20;i++)StaminaManager.tick(p,d);p.setSprinting(false);near(check,d.stamina,97.2,"sprinting costs 2.8 units per second at Endurance forty");
            d.stamina=100;p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));p.level().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            check.accept(p.gameMode.destroyBlock(pos),"native stone mining succeeds");near(check,d.stamina,98.25,"native mining pays thirty percent less");
            RangedNativeChecks.run(p,check);
            d.stamina=7;d.exhausted=false;p.tickCount++;var crossbow=new ItemStack(Items.CROSSBOW);p.setItemInHand(InteractionHand.MAIN_HAND,crossbow);p.getInventory().setItem(1,new ItemStack(Items.ARROW,3));
            crossbow.getItem().releaseUsing(crossbow,p.level(),p,crossbow.getUseDuration(p)-25);check.accept(CrossbowItem.isCharged(crossbow),"seven stamina can afford a ten-unit crossbow charge at Endurance forty");near(check,d.stamina,0,"affordable discounted crossbow spends exactly seven");
            d.set(RPGAttribute.ENDURANCE,100);near(check,StaminaManager.cost(p,12),8.4,"discount never exceeds thirty percent");d.set(RPGAttribute.ENDURANCE,40);
            if(ParCoolCompat.enabled())AttributeBalanceParCoolChecks.run(p,check);
            if(EpicFightCompat.enabled())AttributeBalanceEpicChecks.run(p,check);
        }finally{
            p.level().setBlockAndUpdate(pos,oldBlock);p.setSprinting(false);p.setGameMode(mode);if(EpicFightCompat.enabled())RangedEpicSetup.mode(p,false);
            p.getInventory().clearContent();p.getInventory().load(inventory);p.getInventory().selected=selected;d.load(saved);AttributeManager.apply(p,d);p.setHealth(Math.min(health,p.getMaxHealth()));d.regenCooldown=20000;
        }
    }
    private AttributeBalanceChecks(){}
}

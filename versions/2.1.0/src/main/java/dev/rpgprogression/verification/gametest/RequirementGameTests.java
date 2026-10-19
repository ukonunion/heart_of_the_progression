package dev.rpgprogression.verification.gametest;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.requirements.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.*;
import java.util.*;

public final class RequirementGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var saved=new EnumMap<RPGAttribute,List<? extends String>>(RPGAttribute.class);
        RPGConfig.ITEM_REQUIREMENTS.forEach((a,c)->saved.put(a,List.copyOf(c.get())));
        try{
            var d=ProgressionManager.get(p);var sword=new ItemStack(Items.DIAMOND_SWORD);
            for(var a:RPGAttribute.values()){
                RPGConfig.ITEM_REQUIREMENTS.get(a).set(List.of("minecraft:diamond_sword=11"));
                h.assertFalse(ItemRequirements.allowed(p,sword),"each attribute gates use: "+a);
                d.set(a,11);h.assertTrue(ItemRequirements.allowed(p,sword),"exact threshold unlocks: "+a);
            }
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of("#minecraft:swords=12","minecraft:diamond_sword=15","minecraft:diamond_pickaxe=15"));
            h.assertTrue(ItemRequirements.required(sword).get(RPGAttribute.STRENGTH)==15,"strongest exact/tag threshold wins");
            p.setItemInHand(InteractionHand.MAIN_HAND,sword);
            var target=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE,1,2,1);
            double stamina=d.stamina;var attack=new AttackEntityEvent(p,target);NeoForge.EVENT_BUS.post(attack);
            h.assertTrue(attack.isCanceled()&&d.stamina==stamina,"denied attack does not consume stamina");
            var rightClick=new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);NeoForge.EVENT_BUS.post(rightClick);
            h.assertTrue(rightClick.isCanceled(),"right-click denied");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            p.gameMode.destroyBlock(pos);h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE),"server mining rejected");
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.VITALITY).set(List.of("minecraft:iron_chestplate=20"));
            p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
            RequirementEvents.tick(new PlayerTickEvent.Pre(p));
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).isEmpty()&&p.getInventory().contains(new ItemStack(Items.IRON_CHESTPLATE)),"restricted armor returned without loss");
            d.set(RPGAttribute.STRENGTH,15);h.assertTrue(ItemRequirements.allowed(p,sword),"multi-attribute weapon unlocked");
            h.assertTrue(ItemRequirements.allowed(p,new ItemStack(Items.DIRT)),"unlisted items unaffected");
        }finally{saved.forEach((a,rules)->RPGConfig.ITEM_REQUIREMENTS.get(a).set(rules));}
    }
    private RequirementGameTests(){}
}

package dev.rpgprogression.gametest;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.xp.XPManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import java.util.List;
public final class CookingGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);
        var mining=RPGConfig.MINING_OVERRIDES.get();var combat=RPGConfig.COMBAT_OVERRIDES.get();var fishing=RPGConfig.FISHING_OVERRIDES.get();var cooking=RPGConfig.COOKING_OVERRIDES.get();
        double mm=RPGConfig.MINING_XP_MULTIPLIER.get(),cm=RPGConfig.COMBAT_XP_MULTIPLIER.get(),fm=RPGConfig.FISHING_XP_MULTIPLIER.get(),km=RPGConfig.COOKING_XP_MULTIPLIER.get();
        try{
            RPGConfig.MINING_OVERRIDES.set(List.of("minecraft:stone=7"));RPGConfig.MINING_XP_MULTIPLIER.set(2.0);
            var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            XPManager.mined(p,pos,Blocks.STONE.defaultBlockState());h.assertTrue(d.xp==14,"mining override and multiplier");
            RPGConfig.COMBAT_OVERRIDES.set(List.of("minecraft:zombie=13"));RPGConfig.COMBAT_XP_MULTIPLIER.set(2.0);
            XPManager.killed(p,h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE,1,2,1));long combatEnd=d.xp;h.assertTrue(combatEnd>=34&&combatEnd<=47,"mob override and multiplier before variation");
            RPGConfig.FISHING_OVERRIDES.set(List.of("#minecraft:fishes=99","minecraft:cod=3"));RPGConfig.FISHING_XP_MULTIPLIER.set(2.0);
            XPManager.fished(p,List.of(new ItemStack(Items.COD,2)));h.assertTrue(d.xp==combatEnd+12,"exact item wins over tag");
            RPGConfig.COOKING_OVERRIDES.set(List.of("minecraft:cooked_beef=6"));RPGConfig.COOKING_XP_MULTIPLIER.set(2.0);
            var inventory=new SimpleContainer(new ItemStack(Items.COOKED_BEEF,4));var result=new FurnaceResultSlot(p,inventory,0,0,0);
            var taken=result.remove(2);result.onTake(p,taken);h.assertTrue(d.xp==combatEnd+36,"partial result awards per item");
            result.onTake(p,taken);h.assertTrue(d.xp==combatEnd+36,"duplicate result callback cannot award twice");
            d.xp=d.requiredXP()-24;
            result.onQuickCraft(ItemStack.EMPTY,new ItemStack(Items.COOKED_BEEF,2));result.onTake(p,new ItemStack(Items.COOKED_BEEF,2));
            h.assertTrue(d.level==2&&d.xp==0,"quick move awards once and crosses the new level threshold");
            h.getLevel().setBlockAndUpdate(pos,Blocks.CAMPFIRE.defaultBlockState());var fire=(CampfireBlockEntity)h.getLevel().getBlockEntity(pos);
            java.util.Map<java.util.UUID,ServerPlayer> players;
            try{var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);players=(java.util.Map<java.util.UUID,ServerPlayer>)field.get(p.server.getPlayerList());}
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            players.put(p.getUUID(),p);
            try{
                h.assertTrue(fire.placeFood(p,new ItemStack(Items.BEEF),1),"food placed on actual campfire");
                var saved=fire.saveWithFullMetadata();h.assertTrue(saved.hasUUID("rpgCookOwner0"),"campfire ownership persisted");fire.load(saved);
                CampfireBlockEntity.cookTick(h.getLevel(),pos,fire.getBlockState(),fire);
                h.assertTrue(d.level==2&&d.xp==12,"campfire completion rewards placing player");
                CampfireBlockEntity.cookTick(h.getLevel(),pos,fire.getBlockState(),fire);h.assertTrue(d.xp==12,"completed campfire cannot repeat reward");
            }finally{players.remove(p.getUUID());}
            RPGConfig.COOKING_OVERRIDES.set(List.of("minecraft:cooked_beef=0"));XPManager.cooked(p,new ItemStack(Items.COOKED_BEEF),false);
            h.assertTrue(d.xp==12,"zero disables selected reward");
        }finally{
            RPGConfig.MINING_OVERRIDES.set(mining);RPGConfig.COMBAT_OVERRIDES.set(combat);RPGConfig.FISHING_OVERRIDES.set(fishing);RPGConfig.COOKING_OVERRIDES.set(cooking);
            RPGConfig.MINING_XP_MULTIPLIER.set(mm);RPGConfig.COMBAT_XP_MULTIPLIER.set(cm);RPGConfig.FISHING_XP_MULTIPLIER.set(fm);RPGConfig.COOKING_XP_MULTIPLIER.set(km);
        }
    }
}

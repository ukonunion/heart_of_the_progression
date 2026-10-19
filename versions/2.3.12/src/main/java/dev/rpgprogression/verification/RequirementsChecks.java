package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.requirements.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

/** Actual registry, player interaction and threshold checks; excluded from the release JAR. */
public final class RequirementsChecks {
    private static void check(JsonArray checks,boolean ok,String name){SkintTravelChecks.check(checks,ok,name);}
    public static void run(ServerPlayer p,JsonArray checks){
        var d=ProgressionManager.get(p);var saved=d.save();var mode=p.gameMode.getGameModeForPlayer();var held=p.getMainHandItem();
        var rules=new EnumMap<RPGAttribute,List<? extends String>>(RPGAttribute.class);RPGConfig.ITEM_REQUIREMENTS.forEach((a,c)->rules.put(a,List.copyOf(c.get())));
        try{
            p.setGameMode(GameType.SURVIVAL);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            for(var a:RPGAttribute.values())d.set(a,10);
            for(String material:new String[]{"wooden","stone","golden","iron","diamond","netherite"}){
                int threshold=switch(material){case "iron"->15;case "diamond"->20;case "netherite"->25;default->0;};
                for(String tool:new String[]{"sword","pickaxe","axe","shovel","hoe"}){
                    var id=ResourceLocation.withDefaultNamespace(material+"_"+tool);var stack=new ItemStack(BuiltInRegistries.ITEM.get(id));
                    check(checks,ItemRequirements.required(stack).getOrDefault(RPGAttribute.ENDURANCE,0)==threshold,id+" has the expected material requirement "+threshold);
                    d.set(RPGAttribute.ENDURANCE,Math.max(0,threshold-1));
                    check(checks,threshold==0?ItemRequirements.allowed(p,stack):!ItemRequirements.allowed(p,stack),id+" respects the level below its threshold");
                    d.set(RPGAttribute.ENDURANCE,threshold);
                    check(checks,ItemRequirements.allowed(p,stack)&&ItemRequirements.unmet(p,stack).isEmpty(),id+" unlocks exactly at its threshold without remaining requirements");
                }
            }
            if(net.neoforged.fml.ModList.get().isLoaded("epicfight")){
                for(String material:new String[]{"wooden","stone","iron","diamond","netherite"})for(String tool:new String[]{"greatsword","longsword","spear","tachi","dagger"}){
                    var id=ResourceLocation.fromNamespaceAndPath("epicfight",material+"_"+tool);var stack=new ItemStack(BuiltInRegistries.ITEM.get(id));
                    int threshold=switch(material){case "iron"->15;case "diamond"->20;case "netherite"->25;default->0;};
                    check(checks,!stack.isEmpty()&&ItemRequirements.required(stack).getOrDefault(RPGAttribute.ENDURANCE,0)==threshold,id+" inherits the same material requirement");
                }
                RequirementsEpicChecks.run(p,checks);
            }
            d.set(RPGAttribute.ENDURANCE,14);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));
            var zombie=EntityType.ZOMBIE.create(p.serverLevel());double stamina=d.stamina;
            var attack=new AttackEntityEvent(p,zombie);NeoForge.EVENT_BUS.post(attack);
            check(checks,attack.isCanceled()&&d.stamina==stamina,"a restricted vanilla attack is cancelled before stamina is spent");
            var pos=p.blockPosition().offset(3,0,0);p.serverLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));p.gameMode.destroyBlock(pos);
            check(checks,p.serverLevel().getBlockState(pos).is(Blocks.STONE),"actual server mining refuses the iron pickaxe below Endurance fifteen");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.serverLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            for(var block:new net.minecraft.world.level.block.Block[]{Blocks.BREWING_STAND,Blocks.ENCHANTING_TABLE}){
                p.serverLevel().setBlockAndUpdate(pos,block.defaultBlockState());var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
                d.set(RPGAttribute.INTELLIGENCE,14);var denied=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,pos,hit);NeoForge.EVENT_BUS.post(denied);
                check(checks,denied.isCanceled(),BuiltInRegistries.BLOCK.getKey(block)+" rejects empty-hand interaction below Intelligence fifteen");
                d.set(RPGAttribute.INTELLIGENCE,15);var allowed=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,pos,hit);NeoForge.EVENT_BUS.post(allowed);
                check(checks,!allowed.isCanceled()&&ItemRequirements.unmet(p,new ItemStack(block)).isEmpty(),BuiltInRegistries.BLOCK.getKey(block)+" permits interaction exactly at Intelligence fifteen");
            }
            // Existing open interfaces also close if the attribute is reduced afterwards.
            d.set(RPGAttribute.INTELLIGENCE,14);p.containerMenu=new EnchantmentMenu(78,p.getInventory());RequirementEvents.tick(new PlayerTickEvent.Pre(p));
            check(checks,p.containerMenu==p.inventoryMenu,"an already-open enchanting interface closes after Intelligence becomes insufficient");
            p.containerMenu=new BrewingStandMenu(79,p.getInventory());RequirementEvents.tick(new PlayerTickEvent.Pre(p));
            check(checks,p.containerMenu==p.inventoryMenu,"an already-open brewing interface closes after Intelligence becomes insufficient");
            var diamond=new ItemStack(Items.DIAMOND_SWORD);RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of("minecraft:diamond_sword=15"));
            d.set(RPGAttribute.STRENGTH,14);d.set(RPGAttribute.ENDURANCE,19);
            check(checks,ItemRequirements.unmet(p,diamond).size()==2,"a custom second attribute coexists with the default material requirement");
            d.set(RPGAttribute.ENDURANCE,20);
            check(checks,ItemRequirements.unmet(p,diamond).equals(Map.of(RPGAttribute.STRENGTH,15)),"meeting one attribute removes only that requirement");
            d.set(RPGAttribute.STRENGTH,15);
            check(checks,ItemRequirements.unmet(p,diamond).isEmpty()&&ItemRequirements.allowed(p,diamond),"meeting every attribute removes the full requirement list and unlocks use");
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.ENDURANCE).set(List.of("minecraft:diamond_sword=0"));d.set(RPGAttribute.ENDURANCE,0);
            check(checks,!ItemRequirements.required(diamond).containsKey(RPGAttribute.ENDURANCE),"an exact zero override disables its built-in material requirement");
            if(dev.rpgprogression.compat.IronsCompat.present())IronsRequirementsChecks.run(p,checks);
        }finally{
            rules.forEach((a,list)->RPGConfig.ITEM_REQUIREMENTS.get(a).set(list));p.closeContainer();p.setItemInHand(InteractionHand.MAIN_HAND,held);d.load(saved);p.setGameMode(mode);
        }
    }
    private RequirementsChecks(){}
}

package dev.rpgprogression.verification;

import com.google.gson.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.requirements.*;
import dev.rpgprogression.skint.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.nio.file.*;
import java.util.*;

public final class AccessRulesChecks {
    public static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);if(!ok)throw new IllegalStateException(text);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));}
    private static PlayerInteractEvent.RightClickBlock click(ServerPlayer p,BlockPos pos){var e=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,pos,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));NeoForge.EVENT_BUS.post(e);return e;}
    public static void run(ServerPlayer p,JsonArray checks){
        var d=ProgressionManager.get(p);var saved=d.save();var mode=p.gameMode.getGameModeForPlayer();var position=p.position();var inventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i).copy());
        var rules=new EnumMap<RPGAttribute,List<? extends String>>(RPGAttribute.class);RPGConfig.ITEM_REQUIREMENTS.forEach((a,c)->rules.put(a,List.copyOf(c.get())));boolean defaults=RPGConfig.DEFAULT_ITEM_REQUIREMENTS.get();double open=RPGConfig.SKINT_OPEN_MINIMUM.get(),travel=RPGConfig.SKINT_TRAVEL_COST.get();int trade=RPGConfig.TRADE_INTELLIGENCE.get();
        try{
            p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();d.initialize();for(var a:RPGAttribute.values())d.set(a,100);RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(true);RPGConfig.ITEM_REQUIREMENTS.values().forEach(c->c.set(List.of()));RPGConfig.SKINT_OPEN_MINIMUM.set(25.0);RPGConfig.SKINT_TRAVEL_COST.set(5.0);RPGConfig.TRADE_INTELLIGENCE.set(20);
            var fixture=JsonParser.parseString(Files.readString(Path.of(System.getProperty("rpgprogression.redstoneFixture")))).getAsJsonObject();
            for(var value:fixture.getAsJsonArray("required")){String id=value.getAsString();var stack=item(id);check(checks,!stack.isEmpty()&&ItemRequirements.required(stack).equals(Map.of(RPGAttribute.INTELLIGENCE,15)),id+" has exactly Intelligence fifteen in the actual redstone tag");d.set(RPGAttribute.INTELLIGENCE,14);check(checks,!ItemRequirements.allowed(p,stack),id+" is blocked at Intelligence fourteen");d.set(RPGAttribute.INTELLIGENCE,15);check(checks,ItemRequirements.allowed(p,stack),id+" unlocks at Intelligence fifteen");}
            d.set(RPGAttribute.INTELLIGENCE,0);for(var value:fixture.getAsJsonArray("exempt")){String id=value.getAsString();check(checks,ItemRequirements.required(item(id)).isEmpty()&&ItemRequirements.allowed(p,item(id)),id+" remains exempt from the redstone requirement");}
            var pos=new BlockPos(420,190,10);p.serverLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),2);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            for(var block:List.of(Blocks.LEVER,Blocks.REPEATER,Blocks.COMPARATOR,Blocks.DISPENSER,Blocks.DROPPER,Blocks.CRAFTER,Blocks.HOPPER,Blocks.LECTERN,Blocks.REDSTONE_WIRE,Blocks.REDSTONE_WALL_TORCH)){
                p.serverLevel().setBlock(pos,block.defaultBlockState(),2);d.set(RPGAttribute.INTELLIGENCE,14);check(checks,click(p,pos).isCanceled(),BuiltInRegistries.BLOCK.getKey(block)+" rejects empty-hand use below Intelligence fifteen");d.set(RPGAttribute.INTELLIGENCE,15);check(checks,!click(p,pos).isCanceled(),BuiltInRegistries.BLOCK.getKey(block)+" permits empty-hand use at Intelligence fifteen");
            }
            p.serverLevel().setBlock(pos,Blocks.ENDER_CHEST.defaultBlockState(),2);d.set(RPGAttribute.INTELLIGENCE,0);d.set(RPGAttribute.SPARK_POTENTIAL,19);check(checks,click(p,pos).isCanceled(),"a placed ender chest rejects Spark Potential nineteen");d.set(RPGAttribute.SPARK_POTENTIAL,20);check(checks,!click(p,pos).isCanceled()&&ItemRequirements.required(new ItemStack(Items.ENDER_CHEST)).equals(Map.of(RPGAttribute.SPARK_POTENTIAL,20)),"a placed ender chest permits exactly Spark Potential twenty without an Intelligence condition");
            p.containerMenu=ChestMenu.threeRows(79,p.getInventory(),p.getEnderChestInventory());d.set(RPGAttribute.SPARK_POTENTIAL,19);RequirementEvents.tick(new PlayerTickEvent.Pre(p));check(checks,p.containerMenu==p.inventoryMenu,"an open ender chest closes when Spark Potential drops below twenty");
            var cart=EntityType.MINECART.create(p.serverLevel());d.set(RPGAttribute.INTELLIGENCE,14);var interact=new PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,cart);NeoForge.EVENT_BUS.post(interact);check(checks,interact.isCanceled(),"an already placed minecart cannot bypass the redstone requirement via empty-hand mounting");
            var a=new BlockPos(440,190,0);var b=a.offset(12,0,0);SkintTravelChecks.point(p.serverLevel(),a);SkintTravelChecks.point(p.serverLevel(),b);SkintManager.close(p,false);p.setPos(a.getX()+.5,a.getY(),a.getZ()+2.5);d.mana=0;d.manaCapacity=0;d.spark=24;var discovered=Set.copyOf(d.discoveredSkints);SkintManager.use(p,a);
            check(checks,SkintManager.session(p)==null&&!p.isPassenger()&&d.spark==24&&d.discoveredSkints.equals(discovered),"twenty-four spark cannot open, discover or seat at a skint and no resource is spent");
            d.spark=25;SkintManager.use(p,a);check(checks,SkintManager.canProgress(p)&&d.spark==25&&d.mana==0,"exactly twenty-five current spark opens the skint with empty mana, without spending spark");
            var target=SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),b);d.discoveredSkints.add(target.id());var token=SkintManager.session(p).token();check(checks,SkintManager.travel(p,target.id()),"an authorized route still starts with twenty-five spark");SkintManager.finishedStanding(p,(SkintSeat)p.getVehicle());SkintManager.action(p,4,token,new UUID(0,0));
            check(checks,SkintManager.canProgress(p)&&SkintManager.session(p).origin().id().equals(target.id())&&Math.abs(d.spark-20)<.000001,"arrival keeps the authorized session after paying only the existing five-spark route cost");SkintManager.close(p,false);SkintManager.use(p,b);check(checks,SkintManager.session(p)==null&&d.spark==20,"manual reopening below twenty-five spark is rejected without payment");RPGConfig.SKINT_OPEN_MINIMUM.set(0.0);SkintManager.use(p,b);check(checks,SkintManager.canProgress(p)&&d.spark==20,"the configured zero opening minimum disables only the opening gate");SkintManager.close(p,false);RPGConfig.SKINT_OPEN_MINIMUM.set(25.0);
            for(var aType:RPGAttribute.values())d.set(aType,100);var villager=EntityType.VILLAGER.create(p.serverLevel());d.set(RPGAttribute.INTELLIGENCE,19);interact=new PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,villager);NeoForge.EVENT_BUS.post(interact);check(checks,interact.isCanceled(),"villager interaction rejects trading below Intelligence twenty");d.set(RPGAttribute.INTELLIGENCE,20);interact=new PlayerInteractEvent.EntityInteract(p,InteractionHand.MAIN_HAND,villager);NeoForge.EVENT_BUS.post(interact);check(checks,!interact.isCanceled(),"villager interaction permits trading at Intelligence twenty");
            var offers=villager.getOffers();offers.clear();offers.add(new MerchantOffer(new ItemCost(Items.EMERALD),new ItemStack(Items.BREAD),10,1,0));villager.setTradingPlayer(p);var menu=new MerchantMenu(80,p.getInventory(),villager);p.containerMenu=menu;menu.getSlot(0).set(new ItemStack(Items.EMERALD));check(checks,menu.getSlot(2).hasItem(),"the native merchant fixture has a real payable trade result");d.set(RPGAttribute.INTELLIGENCE,19);
            check(checks,!menu.stillValid(p)&&!menu.getSlot(2).mayPickup(p)&&menu.quickMoveStack(p,2).isEmpty()&&menu.getSlot(0).getItem().getCount()==1,"lowered Intelligence blocks normal and shift-click trade output without consuming emeralds");d.set(RPGAttribute.INTELLIGENCE,20);check(checks,menu.stillValid(p)&&menu.getSlot(2).mayPickup(p)&&!menu.quickMoveStack(p,2).isEmpty()&&menu.getSlot(0).getItem().isEmpty(),"a real trade succeeds and consumes its emerald at Intelligence twenty");d.set(RPGAttribute.INTELLIGENCE,19);RequirementEvents.tick(new PlayerTickEvent.Pre(p));check(checks,p.containerMenu==p.inventoryMenu,"an already-open trading menu closes after Intelligence falls below twenty");RPGConfig.TRADE_INTELLIGENCE.set(0);check(checks,InteractionRequirements.canTrade(p),"the trading minimum remains configurable");RPGConfig.TRADE_INTELLIGENCE.set(20);
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.INTELLIGENCE).set(List.of("minecraft:lever=0"));check(checks,ItemRequirements.required(new ItemStack(Items.LEVER)).isEmpty(),"a configured zero overrides the new automatic redstone requirement");RPGConfig.ITEM_REQUIREMENTS.values().forEach(c->c.set(List.of()));RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(false);check(checks,ItemRequirements.required(new ItemStack(Items.LEVER)).isEmpty()&&ItemRequirements.required(new ItemStack(Items.ENDER_CHEST)).isEmpty(),"the existing default-item switch disables both new item requirement groups");RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(true);
            if(dev.rpgprogression.compat.IronsCompat.present())AccessIronsChecks.run(p,checks);
            if(net.neoforged.fml.ModList.get().isLoaded("relics"))AccessRelicsChecks.run(p,checks);
            AccessConfigTemplates.write(checks);
        }catch(Exception e){throw new IllegalStateException(e);}finally{
            SkintManager.close(p,false);p.closeContainer();RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(defaults);rules.forEach((a,r)->RPGConfig.ITEM_REQUIREMENTS.get(a).set(r));RPGConfig.SKINT_OPEN_MINIMUM.set(open);RPGConfig.SKINT_TRAVEL_COST.set(travel);RPGConfig.TRADE_INTELLIGENCE.set(trade);for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));d.load(saved);p.setPos(position);p.setGameMode(mode);
        }
    }
    private AccessRulesChecks(){}
}

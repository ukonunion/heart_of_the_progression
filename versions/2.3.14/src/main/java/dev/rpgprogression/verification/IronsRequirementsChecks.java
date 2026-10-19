package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.requirements.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.item.SpellBook;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.*;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import java.util.*;

/** Uses the installed 3.16.3 registry and actual casting/menu/equipment paths. Not shipped. */
public final class IronsRequirementsChecks {
    private static void check(JsonArray checks,boolean ok,String name){SkintTravelChecks.check(checks,ok,name);if(!ok)throw new IllegalStateException(name);}
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks",path)));}
    public static void run(ServerPlayer p,JsonArray checks){
        var d=ProgressionManager.get(p);var saved=d.save();var main=p.getMainHandItem();var off=p.getOffhandItem();var head=p.getItemBySlot(EquipmentSlot.HEAD);
        try{
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            int books=0,orbs=0,staffs=0,jewelry=0,wizard=0;
            for(var entry:BuiltInRegistries.ITEM.entrySet()){
                var id=entry.getKey().location();if(!id.getNamespace().equals("irons_spellbooks"))continue;
                var stack=new ItemStack(entry.getValue());String path=id.getPath();var required=ItemRequirements.required(stack);
                if(entry.getValue() instanceof SpellBook book){
                    books++;int n=book.getMaxSpellSlots();var expected=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);
                    if(n>=12){expected.put(RPGAttribute.INTELLIGENCE,25);expected.put(RPGAttribute.SPARK_POTENTIAL,20);}
                    else if(n>=10){expected.put(RPGAttribute.INTELLIGENCE,20);expected.put(RPGAttribute.SPARK_POTENTIAL,15);}
                    else if(n>=6)expected.put(RPGAttribute.INTELLIGENCE,15);
                    check(checks,required.equals(expected),id+" ("+n+" slots) has the requested book requirements");
                }
                if(path.equals("upgrade_orb")||path.endsWith("_upgrade_orb")){orbs++;check(checks,required.equals(Map.of(RPGAttribute.INTELLIGENCE,15)),id+" requires Intelligence 15");}
                if(entry.getValue() instanceof io.redspace.ironsspellbooks.item.weapons.StaffItem||path.equals("staff_of_the_nines")){staffs++;check(checks,required.equals(Map.of(RPGAttribute.INTELLIGENCE,20)),id+" requires Intelligence 20");}
                if(path.endsWith("_ring")||path.endsWith("_necklace")||path.endsWith("_amulet")){jewelry++;check(checks,required.equals(Map.of(RPGAttribute.INTELLIGENCE,12)),id+" requires Intelligence 12");}
                if(stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.fromNamespaceAndPath("rpgprogression","irons_wizard_equipment")))){
                    wizard++;check(checks,required.equals(Map.of(RPGAttribute.INTELLIGENCE,20,RPGAttribute.ENDURANCE,20)),id+" inherits both wizard requirements");
                }
                if(path.startsWith("wandering_magician_"))check(checks,required.isEmpty(),id+" stays unrestricted");
            }
            check(checks,books==16&&orbs==12&&staffs==7&&jewelry==17&&wizard==44,"the complete installed book/orb/staff/jewelry/wizard families were checked: "+books+"/"+orbs+"/"+staffs+"/"+jewelry+"/"+wizard);
            for(String path:List.of("lesser_spell_slot_upgrade","eldritch_manuscript"))check(checks,ItemRequirements.required(item(path)).equals(Map.of(RPGAttribute.INTELLIGENCE,20)),path+" requires Intelligence 20");
            var book=item("wimpy_spell_book");ISpellContainer.set(book,ISpellContainer.create(10,true,true));
            check(checks,ItemRequirements.required(book).equals(Map.of(RPGAttribute.INTELLIGENCE,20,RPGAttribute.SPARK_POTENTIAL,15)),"upgrading a five-slot book to ten cells raises its requirements");
            ISpellContainer.set(book,ISpellContainer.create(12,true,true));d.set(RPGAttribute.INTELLIGENCE,25);d.set(RPGAttribute.SPARK_POTENTIAL,19);
            check(checks,ItemRequirements.unmet(p,book).equals(Map.of(RPGAttribute.SPARK_POTENTIAL,20)),"twelve-slot books hide fulfilled Intelligence but retain unmet Spark Potential");
            d.set(RPGAttribute.SPARK_POTENTIAL,20);check(checks,ItemRequirements.allowed(p,book)&&ItemRequirements.unmet(p,book).isEmpty(),"both exact twelve-slot book thresholds unlock it completely");
            int spells=0;
            for(var spell:SpellRegistry.REGISTRY.stream().toList())if(!spell.equals(SpellRegistry.none())){
                spells++;
                for(int level:new int[]{spell.getMinLevel(),spell.getMaxLevel()}){
                    var scroll=item("scroll");ISpellContainer.createScrollContainer(spell,level,scroll);int expected=spell.getManaCost(level)*5;
                    check(checks,ItemRequirements.required(scroll).getOrDefault(RPGAttribute.INTELLIGENCE,0)==expected,spell.getSpellId()+" level "+level+" scroll requires five Intelligence per converted mana");
                    if(expected>0){d.set(RPGAttribute.INTELLIGENCE,expected-1);
                        check(checks,!IronsRequirements.spellAllowed(p,spell,level,CastSource.SPELLBOOK)&&!ItemRequirements.allowed(p,scroll),spell.getSpellId()+" level "+level+" is blocked below threshold both in books and scrolls");
                        check(checks,!spell.canBeCastedBy(level,CastSource.SPELLBOOK,MagicData.getPlayerMagicData(p),p).isSuccess(),spell.getSpellId()+" level "+level+" rejects the native casting authorization below threshold");
                    }
                    d.set(RPGAttribute.INTELLIGENCE,expected);check(checks,IronsRequirements.spellUnmet(p,spell,level).isEmpty()&&ItemRequirements.allowed(p,scroll),spell.getSpellId()+" level "+level+" reaches its exact requirement");
                }
            }
            check(checks,spells>80,"every registered Iron's spell was checked at minimum and maximum level ("+spells+")");
            var firebolt=SpellRegistry.FIREBOLT_SPELL.get();var scroll=item("scroll");ISpellContainer.createScrollContainer(firebolt,1,scroll);
            d.set(RPGAttribute.INTELLIGENCE,IronsRequirements.spellMinimum(firebolt,1)-1);double mana=d.mana;int count=scroll.getCount();
            p.setItemInHand(InteractionHand.MAIN_HAND,scroll);
            check(checks,!firebolt.attemptInitiateCast(scroll,1,p.level(),p,CastSource.SCROLL,true,"mainhand")&&!MagicData.getPlayerMagicData(p).isCasting()&&d.mana==mana&&scroll.getCount()==count,"a refused actual scroll cast starts no animation and consumes neither mana nor scroll");
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for(var row:List.of(Map.entry("scroll_forge",12),Map.entry("arcane_anvil",15))){
                var pos=p.blockPosition().offset(3,0,0);var block=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks",row.getKey()));p.serverLevel().setBlockAndUpdate(pos,block.defaultBlockState());
                var hit=new BlockHitResult(Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
                d.set(RPGAttribute.INTELLIGENCE,row.getValue()-1);var denied=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,pos,hit);NeoForge.EVENT_BUS.post(denied);
                d.set(RPGAttribute.INTELLIGENCE,row.getValue());var allowed=new PlayerInteractEvent.RightClickBlock(p,InteractionHand.MAIN_HAND,pos,hit);NeoForge.EVENT_BUS.post(allowed);
                check(checks,denied.isCanceled()&&!allowed.isCanceled(),row.getKey()+" checks placed-station interaction at its exact threshold");
                p.serverLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            d.set(RPGAttribute.INTELLIGENCE,19);var anvil=new io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu(92,p.getInventory(),ContainerLevelAccess.NULL);
            anvil.getSlot(0).set(item("wimpy_spell_book"));anvil.getSlot(1).set(item("lesser_spell_slot_upgrade"));
            check(checks,!anvil.getSlot(2).getItem().isEmpty()&&!anvil.getSlot(2).mayPickup(p),"the actual anvil result rejects a capacity upgrade at Intelligence 19");
            int before=anvil.getSlot(1).getItem().getCount();anvil.clicked(2,0,ClickType.QUICK_MOVE,p);
            check(checks,anvil.getSlot(1).getItem().getCount()==before,"shift-click cannot consume a locked capacity upgrade");
            d.set(RPGAttribute.INTELLIGENCE,20);check(checks,anvil.getSlot(2).mayPickup(p),"the actual anvil result permits its capacity upgrade at Intelligence 20");
            d.set(RPGAttribute.INTELLIGENCE,14);p.containerMenu=anvil;RequirementEvents.tick(new PlayerTickEvent.Pre(p));
            check(checks,p.containerMenu==p.inventoryMenu,"an open magical anvil closes if Intelligence falls below 15");
            var ring=item("mana_ring");d.set(RPGAttribute.INTELLIGENCE,11);var deniedEquip=new CurioCanEquipEvent(ring,new SlotContext("ring",p,0,false,true));NeoForge.EVENT_BUS.post(deniedEquip);
            d.set(RPGAttribute.INTELLIGENCE,12);var allowedEquip=new CurioCanEquipEvent(ring,new SlotContext("ring",p,0,false,true));NeoForge.EVENT_BUS.post(allowedEquip);
            check(checks,deniedEquip.getEquipResult()==net.neoforged.neoforge.common.util.TriState.FALSE&&allowedEquip.getEquipResult()!=net.neoforged.neoforge.common.util.TriState.FALSE,"native Curios equipment validation refuses rings at 11 and allows them at 12");
            var curio=CuriosApi.getCuriosInventory(p).orElseThrow();var handler=curio.getStacksHandler("ring").orElseThrow().getStacks();var prior=handler.getStackInSlot(0);
            handler.setStackInSlot(0,ring);d.set(RPGAttribute.INTELLIGENCE,11);CurioRequirementGuards.validate(p);
            check(checks,handler.getStackInSlot(0).isEmpty()&&p.getInventory().contains(item("mana_ring")),"a ring force-equipped below its threshold is returned to inventory without loss");handler.setStackInSlot(0,prior);
            var bookHandler=curio.getStacksHandler("spellbook").orElseThrow().getStacks();var oldBook=bookHandler.getStackInSlot(0);var lockedBook=item("netherite_spell_book");
            var container=ISpellContainer.create(12,true,true).mutableCopy();container.addSpellAtIndex(firebolt,1,0,false);ISpellContainer.set(lockedBook,container.toImmutable());bookHandler.setStackInSlot(0,lockedBook);
            d.set(RPGAttribute.INTELLIGENCE,24);d.set(RPGAttribute.SPARK_POTENTIAL,20);d.mana=8;
            check(checks,!io.redspace.ironsspellbooks.api.util.Utils.serverSideInitiateCast(p)&&!MagicData.getPlayerMagicData(p).isCasting(),"the actual key-cast path rejects a twelve-slot book at Intelligence 24 even when its spell is affordable");
            d.set(RPGAttribute.INTELLIGENCE,50);d.mana=8;
            check(checks,io.redspace.ironsspellbooks.api.util.Utils.serverSideInitiateCast(p)&&MagicData.getPlayerMagicData(p).isCasting(),"the actual key-cast path starts after both the book and spell requirements are fulfilled");
            d.set(RPGAttribute.INTELLIGENCE,0);IronsRequirements.validateCast(p);
            check(checks,!MagicData.getPlayerMagicData(p).isCasting()&&d.mana==8,"lowering Intelligence during a cast cancels it before payment");bookHandler.setStackInSlot(0,oldBook);
            d.set(RPGAttribute.ENDURANCE,19);d.set(RPGAttribute.INTELLIGENCE,20);p.setItemSlot(EquipmentSlot.HEAD,item("wizard_helmet"));RequirementEvents.tick(new PlayerTickEvent.Pre(p));
            check(checks,p.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"wizard armor force-equipped without Endurance 20 is removed by the server");
        }finally{p.closeContainer();p.setItemInHand(InteractionHand.MAIN_HAND,main);p.setItemInHand(InteractionHand.OFF_HAND,off);p.setItemSlot(EquipmentSlot.HEAD,head);d.load(saved);}
    }
    private IronsRequirementsChecks(){}
}

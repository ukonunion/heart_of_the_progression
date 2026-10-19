package dev.rpgprogression.compat;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.requirements.ItemRequirements;
import dev.rpgprogression.resource.ResourceMath;
import io.redspace.ironsspellbooks.api.item.IScroll;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.item.SpellBook;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import java.util.*;

/** Loaded only with Iron's 3.16. Requirements follow the actual spell level and upgraded book capacity. */
public final class IronsRequirements {
    private static final dev.rpgprogression.requirements.ConfiguredRequirements CONFIGURED=new dev.rpgprogression.requirements.ConfiguredRequirements();
    private static final TagKey<Item> WIZARD=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("rpgprogression","irons_wizard_equipment"));
    private static final TagKey<Item> RING=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("curios","ring"));
    private static final TagKey<Item> NECKLACE=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("curios","necklace"));
    public static void add(Player player,ItemStack stack,Map<RPGAttribute,Integer> result){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals("irons_spellbooks"))return;
        String path=id.getPath();
        if(stack.getItem() instanceof IScroll){
            var container=ISpellContainer.get(stack);
            if(container!=null)for(var slot:container.getActiveSpells())spellRequired(slot.getSpell(),player==null?slot.getLevel():slot.getSpell().getLevelFor(slot.getLevel(),player)).forEach((attribute,minimum)->result.merge(attribute,minimum,Math::max));
            result.values().removeIf(n->n<=0);
        }
        if(!dev.rpgprogression.config.RPGConfig.DEFAULT_ITEM_REQUIREMENTS.get())return;
        if(stack.getItem() instanceof SpellBook book){
            var container=ISpellContainer.get(stack);
            int slots=Math.max(book.getMaxSpellSlots(),container==null?0:container.getMaxSpellCount());
            if(slots>=12){result.put(RPGAttribute.INTELLIGENCE,25);result.put(RPGAttribute.SPARK_POTENTIAL,20);}
            else if(slots>=10){result.put(RPGAttribute.INTELLIGENCE,20);result.put(RPGAttribute.SPARK_POTENTIAL,15);}
            else if(slots>=6)result.put(RPGAttribute.INTELLIGENCE,15);
        }
        if(path.equals("upgrade_orb")||path.endsWith("_upgrade_orb"))result.put(RPGAttribute.INTELLIGENCE,15);
        if(stack.getItem() instanceof StaffItem||path.equals("staff_of_the_nines"))result.put(RPGAttribute.INTELLIGENCE,20);
        if(stack.is(RING)||stack.is(NECKLACE)||path.endsWith("_ring")||path.endsWith("_necklace")||path.endsWith("_amulet"))result.put(RPGAttribute.INTELLIGENCE,12);
        if(path.equals("lesser_spell_slot_upgrade")||path.equals("eldritch_manuscript"))result.put(RPGAttribute.INTELLIGENCE,20);
        if(path.equals("scroll_forge"))result.put(RPGAttribute.INTELLIGENCE,12);
        if(path.equals("arcane_anvil"))result.put(RPGAttribute.INTELLIGENCE,15);
        if(stack.is(WIZARD)){result.put(RPGAttribute.ENDURANCE,20);result.put(RPGAttribute.INTELLIGENCE,20);}
    }
    public static int spellMinimum(AbstractSpell spell,int level){
        int cost=spell.getManaCost(level);
        int units=IronsCompat.enabled()?cost:(int)ResourceMath.spellCost(cost);
        return dev.rpgprogression.requirements.SpellRequirementMath.intelligence(units,dev.rpgprogression.config.RPGConfig.SPELL_BASE_INTELLIGENCE.get(),dev.rpgprogression.config.RPGConfig.SPELL_INTELLIGENCE_PER_MANA.get());
    }
    public static Map<RPGAttribute,Integer> spellRequired(AbstractSpell spell,int level){
        var result=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);int minimum=spellMinimum(spell,level);
        if(minimum>0)result.put(RPGAttribute.INTELLIGENCE,minimum);
        CONFIGURED.apply(dev.rpgprogression.config.RPGConfig.SPELL_REQUIREMENTS,spell.getSpellId().toString(),ignored->false,result);return result;
    }
    public static Map<RPGAttribute,Integer> spellUnmet(Player player,AbstractSpell spell,int level){
        return ItemRequirements.unmet(player,spellRequired(spell,level));
    }
    public static boolean spellAllowed(Player player,AbstractSpell spell,int level,CastSource source){
        if(player.isCreative()||player.isSpectator()||source==CastSource.COMMAND||source==CastSource.MOB||source==CastSource.NONE)return true;
        for(var stack:List.of(player.getMainHandItem(),player.getOffhandItem()))
            if(stack.getItem() instanceof StaffItem&&!ItemRequirements.allowed(player,stack))return false;
        return spellUnmet(player,spell,level).isEmpty();
    }
    public static ItemStack station(AbstractContainerMenu menu){
        String name=menu.getClass().getName();
        String item=name.equals("io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu")?"arcane_anvil"
                :name.equals("io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu")?"scroll_forge":null;
        return item==null?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks",item)));
    }
    public static boolean menuAllowed(Player player,AbstractContainerMenu menu,int slot){
        if(!ItemRequirements.allowed(player,station(menu)))return false;
        // Taking an anvil result uses both inputs. Merely carrying or storing a locked item is allowed.
        if(menu instanceof io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu&&slot==2)
            return ItemRequirements.allowed(player,menu.getSlot(0).getItem())&&ItemRequirements.allowed(player,menu.getSlot(1).getItem());
        return true;
    }
    /** Iron's key-cast paths pass an empty ItemStack and identify the real source by its slot. */
    public static ItemStack castingItem(Player player,ItemStack supplied,String slot){
        if(supplied!=null&&!supplied.isEmpty())return supplied;
        if(slot==null)return ItemStack.EMPTY;
        if(slot.equals(io.redspace.ironsspellbooks.compat.Curios.SPELLBOOK_SLOT)){
            var book=io.redspace.ironsspellbooks.api.util.Utils.getPlayerSpellbookStack(player);return book==null?ItemStack.EMPTY:book;
        }
        for(var equipment:net.minecraft.world.entity.EquipmentSlot.values())if(equipment.getName().equals(slot))return player.getItemBySlot(equipment);
        int split=slot.lastIndexOf('_');
        if(split>0)try{
            int index=Integer.parseInt(slot.substring(split+1));String identifier=slot.substring(0,split);
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).flatMap(i->i.findCurio(identifier,index)).map(top.theillusivec4.curios.api.SlotResult::stack).orElse(ItemStack.EMPTY);
        }catch(NumberFormatException ignored){}
        return ItemStack.EMPTY;
    }
    public static void validateCast(net.minecraft.server.level.ServerPlayer player){
        var magic=io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player);
        if(magic.isCasting()&&(!spellAllowed(player,magic.getCastingSpell().getSpell(),magic.getCastingSpellLevel(),magic.getCastSource())
                ||!ItemRequirements.allowed(player,castingItem(player,magic.getPlayerCastingItem(),magic.getCastingEquipmentSlot()))))io.redspace.ironsspellbooks.api.util.Utils.serverSideCancelCast(player,false);
    }
    private IronsRequirements(){}
}

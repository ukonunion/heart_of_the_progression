package dev.rpgprogression.requirements;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import java.util.*;

public final class ItemRequirements {
    private static final ConfiguredRequirements CONFIGURED=new ConfiguredRequirements();
    public static Map<RPGAttribute,Integer> required(ItemStack stack){
        return required(null,stack);
    }
    public static Map<RPGAttribute,Integer> required(Player player,ItemStack stack){
        var result=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);if(stack.isEmpty())return result;
        // Vanilla material tiers are also used by Epic Fight and other tiered tools/weapons.
        if(RPGConfig.DEFAULT_ITEM_REQUIREMENTS.get()){
        if(stack.getItem() instanceof TieredItem tool){
            var tier=tool.getTier();int minimum=tier==Tiers.IRON?15:tier==Tiers.DIAMOND?20:tier==Tiers.NETHERITE?25:0;
            if(minimum>0)result.put(RPGAttribute.ENDURANCE,minimum);
        }
        if(stack.is(Items.BREWING_STAND)||stack.is(Items.ENCHANTING_TABLE))result.put(RPGAttribute.INTELLIGENCE,15);
        }
        if(dev.rpgprogression.compat.IronsCompat.present())dev.rpgprogression.compat.IronsRequirements.add(player,stack,result);
        String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        CONFIGURED.apply(RPGConfig.ITEM_REQUIREMENTS,item,id->stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(id))),result);
        return result;
    }
    /** The tooltip and server authorization use the same per-attribute comparison. */
    public static Map<RPGAttribute,Integer> unmet(Player player,ItemStack stack){
        return unmet(player,required(player,stack));
    }
    public static Map<RPGAttribute,Integer> unmet(Player player,Map<RPGAttribute,Integer> required){
        var missing=new EnumMap<RPGAttribute,Integer>(RPGAttribute.class);missing.putAll(required);
        if(player!=null)PlayerProgressionProvider.get(player).ifPresent(d->missing.entrySet().removeIf(e->d.get(e.getKey())>=e.getValue()));
        return missing;
    }
    public static boolean allowed(Player player,ItemStack stack){
        if(player.isCreative()||player.isSpectator()||stack==null||stack.isEmpty())return true;
        return unmet(player,stack).isEmpty();
    }
    private ItemRequirements(){}
}

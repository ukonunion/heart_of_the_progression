package dev.rpgprogression.gametest.client;

import com.google.gson.*;
import dev.rpgprogression.defense.EquipmentDefense;
import dev.rpgprogression.food.FoodManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.util.*;

/** Registry-derived catalogue of every installed armor item and food; never alters equipment. */
public final class ArmorFoodReportCapture {
    private static Map<Attribute,AttributeInstance> evaluate(List<ItemStack> stacks){
        Map<Attribute,AttributeInstance> result=new HashMap<>();
        for(var stack:stacks){var armor=(ArmorItem)stack.getItem();stack.getAttributeModifiers(armor.getEquipmentSlot()).forEach((attribute,modifier)->{
            var instance=result.computeIfAbsent(attribute,a->new AttributeInstance(a,x->{}));
            if(instance.getModifier(modifier.getId())!=null)throw new IllegalStateException("duplicate armor modifier: "+modifier.getId());
            instance.addTransientModifier(modifier);
        });}return result;
    }
    private static double value(Map<Attribute,AttributeInstance> values,Attribute attribute){return values.containsKey(attribute)?values.get(attribute).getValue():attribute.getDefaultValue();}
    private static JsonObject row(List<ItemStack> stacks){
        var attributes=evaluate(stacks);var row=new JsonObject();double armor=value(attributes,Attributes.ARMOR),tough=value(attributes,Attributes.ARMOR_TOUGHNESS);
        double physicalArmor=EquipmentDefense.armorAttribute(stacks,Attributes.ARMOR,false),physicalTough=EquipmentDefense.armorAttribute(stacks,Attributes.ARMOR_TOUGHNESS,false);
        double magicArmor=EquipmentDefense.armorAttribute(stacks,Attributes.ARMOR,true),magicTough=EquipmentDefense.armorAttribute(stacks,Attributes.ARMOR_TOUGHNESS,true);
        row.addProperty("armor",armor);row.addProperty("toughness",tough);row.addProperty("physicalArmor",physicalArmor);row.addProperty("physicalToughness",physicalTough);row.addProperty("magicArmor",magicArmor);row.addProperty("magicToughness",magicTough);row.addProperty("physicalPercent",EquipmentDefense.armorPercent(physicalArmor,physicalTough));
        var generic=ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation("irons_spellbooks","spell_resist"));
        double resist=generic==null?1:value(attributes,generic);
        row.addProperty("vanillaMagicalPercent",EquipmentDefense.armorPercent(magicArmor,magicTough));
        row.addProperty("magicalPercent",EquipmentDefense.spellPercent(magicArmor,magicTough,resist,true));
        var schools=new JsonObject();
        for(var attribute:ForgeRegistries.ATTRIBUTES){var id=ForgeRegistries.ATTRIBUTES.getKey(attribute);if(id.getNamespace().equals("irons_spellbooks")&&id.getPath().endsWith("_magic_resist"))schools.addProperty(id.getPath(),EquipmentDefense.spellPercent(magicArmor,magicTough,resist*value(attributes,attribute),true));}
        row.add("schoolPercent",schools);return row;
    }
    public static void write(ServerPlayer p){
        var source=EquipmentDefense.magicSource(p);
        var report=new JsonObject();report.addProperty("referenceDamageHP",10);report.addProperty("magicReference",source.typeHolder().unwrapKey().orElseThrow().location().toString());var items=new JsonArray();var foods=new JsonArray();
        Map<String,List<ItemStack>> groups=new TreeMap<>();
        for(var item:BuiltInRegistries.ITEM){var stack=new ItemStack(item);String id=BuiltInRegistries.ITEM.getKey(item).toString();
            if(item instanceof ArmorItem armor){var data=row(List.of(stack));data.addProperty("id",id);data.addProperty("slot",armor.getEquipmentSlot().getName());data.addProperty("material",armor.getMaterial().getName());items.add(data);
                String group=BuiltInRegistries.ITEM.getKey(item).getNamespace()+":"+armor.getMaterial().getName();groups.computeIfAbsent(group,k->new ArrayList<>()).add(stack);}
            var properties=stack.getFoodProperties(p);if(properties!=null){var data=new JsonObject();data.addProperty("id",id);data.addProperty("nutrition",properties.getNutrition());data.addProperty("category",FoodManager.category(stack));data.addProperty("healingHP",stack.is(FoodManager.HEALING)&&!stack.is(FoodManager.SOUPS)&&!stack.is(FoodManager.ENERGY)?properties.getNutrition()*.5:0);foods.add(data);}
        }
        var sets=new JsonArray();for(var entry:groups.entrySet()){
            // A material can contain alternative armor (e.g. a crown). Keep all pieces in
            // the item catalogue; compose one complete set with a single item per slot.
            Map<net.minecraft.world.entity.EquipmentSlot,ItemStack> slots=new EnumMap<>(net.minecraft.world.entity.EquipmentSlot.class);
            for(var stack:entry.getValue())slots.putIfAbsent(((ArmorItem)stack.getItem()).getEquipmentSlot(),stack);
            if(slots.size()!=4)continue;var data=row(new ArrayList<>(slots.values()));data.addProperty("material",entry.getKey());var ids=new JsonArray();slots.values().forEach(s->ids.add(BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));data.add("items",ids);sets.add(data);
        }
        report.add("armorItems",items);report.add("armorSets",sets);report.add("foods",foods);
        String profile=dev.rpgprogression.compat.IronsCompat.present()?"combined":"standalone";
        try{Files.writeString(Path.of("armor-food-report-0.9.2-"+profile+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
        catch(Exception error){throw new IllegalStateException("Cannot capture armor/food catalogue",error);}
    }
    private ArmorFoodReportCapture(){}
}

package dev.rpgprogression.verification.gametest;

import dev.rpgprogression.attribute.*;
import dev.rpgprogression.defense.*;
import dev.rpgprogression.food.FoodManager;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.resource.SparkManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class FoodGameTests {
    private static void near(GameTestHelper h,double actual,double expected,String message){h.assertTrue(Math.abs(actual-expected)<.002,message+": "+actual+" != "+expected);}
    private static void eat(ServerPlayer p,Item item){p.eat(p.level(),new ItemStack(item));}
    public static void healing(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);p.setHealth(10);p.getFoodData().setFoodLevel(0);p.getFoodData().setSaturation(20);
        p.getFoodData().addExhaustion(100);p.getFoodData().tick(p);
        near(h,p.getFoodData().getFoodLevel(),20,"food shell stays full");near(h,p.getFoodData().getSaturationLevel(),0,"saturation disabled");near(h,p.getHealth(),10,"no vanilla starvation/healing");
        h.assertTrue(p.canEat(false),"all food usable without hunger");
        h.assertFalse(p.addEffect(new MobEffectInstance(MobEffects.HUNGER,200)),"hunger effect rejected");
        for(int i=0;i<20;i++)FoodManager.tick(p,d);near(h,p.getHealth(),10.1,"weak constant regen independent of hunger");
        eat(p,Items.COOKED_BEEF);near(h,p.getHealth(),14.1,"steak restores half its eight nutrition points");
        eat(p,Items.BEEF);near(h,p.getHealth(),15.6,"raw beef heals fractional HP");
        eat(p,Items.COOKED_COD);near(h,p.getHealth(),18.1,"cooked fish heals 2.5 HP");
        eat(p,Items.CARROT);near(h,p.getHealth(),19.6,"vegetables heal");eat(p,Items.COOKED_BEEF);near(h,p.getHealth(),20,"food does not exceed max health");
        var starve=damage(p,p.damageSources().starve(),10);NeoForge.EVENT_BUS.post(starve);near(h,starve.getNewDamage(),0,"starvation cannot hurt");
    }
    public static void bonuses(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);p.setHealth(10);
        ItemStack result=Items.MUSHROOM_STEW.finishUsingItem(new ItemStack(Items.MUSHROOM_STEW),p.level(),p);
        h.assertTrue(result.is(Items.BOWL),"stew returns bowl normally");near(h,p.getHealth(),10,"soup grants defense instead of healing");
        for(var type:DefenseType.values())near(h,DefenseManager.percent(d,type),6.25,"soup applies to "+type);
        eat(p,Items.GOLDEN_APPLE);near(h,FoodManager.protection(d),12.5,"gold apple protection");h.assertTrue(p.getActiveEffects().isEmpty(),"gold apple replaces native effects");
        eat(p,Items.ENCHANTED_GOLDEN_APPLE);near(h,FoodManager.protection(d),25,"enchanted apple protection");
        d.enchantedFoodTicks=2;eat(p,Items.MUSHROOM_STEW);h.assertTrue(d.enchantedFoodTicks==2,"weaker soup never extends enchanted apple");
        Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET),p.level(),p);near(h,FoodManager.protection(d),25,"milk does not erase timer bonuses");
        var cakePos=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));var cake=net.minecraft.world.level.block.Blocks.CAKE;var state=cake.defaultBlockState();p.serverLevel().setBlockAndUpdate(cakePos,state);
        state.useWithoutItem(p.serverLevel(),p,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(cakePos),net.minecraft.core.Direction.UP,cakePos,false));
        h.assertTrue(p.serverLevel().getBlockState(cakePos).getValue(net.minecraft.world.level.block.CakeBlock.BITES)==1&&d.energyFoodTicks==1200,"real cake bite grants energy without hunger");
        Items.HONEY_BOTTLE.finishUsingItem(new ItemStack(Items.HONEY_BOTTLE),p.level(),p);AttributeManager.apply(p,d);near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),.11,"energy movement speed");near(h,p.getAttributeValue(Attributes.ATTACK_SPEED),4.4,"energy attack speed");
        d.stamina=50;d.regenCooldown=0;p.setSprinting(false);StaminaManager.tick(p,d);near(h,d.stamina,50.44,"food accelerates shared stamina regen");
        for(int i=0;i<5;i++)eat(p,Items.CHORUS_FRUIT);h.assertTrue(d.chorusFoodStacks==4,"chorus capped at four stacks");near(h,FoodManager.sparkMultiplier(d),5.0625,"chorus multiplies recovery per stack");
        d.spark=1;double before=d.spark;SparkManager.tick(p,d);near(h,d.spark-before,d.maxSpark()/d.sparkRecoveryTicks()*5.0625,"actual spark recovery accelerated");
        var copy=new PlayerProgression();copy.load(d.save());h.assertTrue(copy.energyFoodTicks==d.energyFoodTicks&&copy.chorusFoodStacks==4&&copy.enchantedFoodTicks==2,"food timers survive save/load without mana conversion");
        d.energyFoodTicks=d.chorusFoodTicks=d.enchantedFoodTicks=d.goldenFoodTicks=1;FoodManager.tick(p,d);
        near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),.1,"expired speed removed");near(h,p.getAttributeValue(Attributes.ATTACK_SPEED),4,"expired attack speed removed");near(h,FoodManager.sparkMultiplier(d),1,"expired chorus cleared");near(h,FoodManager.protection(d),6.25,"weaker unexpired soup resumes");
        FoodManager.clear(d);AttributeManager.apply(p,d);near(h,FoodManager.protection(d),0,"death cleanup clears bonuses");
    }
    public static void armor(GameTestHelper h,ServerPlayer p){
        double[] expected={8,24,28,40,70,72};var materials=java.util.List.of(ArmorMaterials.LEATHER,ArmorMaterials.GOLD,ArmorMaterials.CHAIN,ArmorMaterials.IRON,ArmorMaterials.DIAMOND,ArmorMaterials.NETHERITE);
        for(int i=0;i<materials.size();i++){var m=materials.get(i);int armor=0;for(var type:java.util.List.of(ArmorItem.Type.HELMET,ArmorItem.Type.CHESTPLATE,ArmorItem.Type.LEGGINGS,ArmorItem.Type.BOOTS))armor+=m.value().defense().getOrDefault(type,0);near(h,EquipmentDefense.armorPercent(armor,m.value().toughness()*4),expected[i],"vanilla armor "+m.unwrapKey().orElseThrow().location());}
        p.getAttribute(Attributes.ARMOR).setBaseValue(20);p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
        var d=ProgressionManager.get(p);d.set(RPGAttribute.VITALITY,20);eat(p,Items.MUSHROOM_STEW);
        near(h,EquipmentDefense.percent(p,DefenseType.PHYSICAL),70,"native attributes supply modded and vanilla armor");near(h,DefenseManager.displayedPercent(p,d,DefenseType.PHYSICAL),73.075,"UI composes equipment and RPG percentages");
        var mace=new ItemStack(Items.MACE);mace.enchant(p.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.BREACH),4);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,mace);
        near(h,EquipmentDefense.percent(p,DefenseType.PHYSICAL),70,"held Breach enchantment does not lower the player's armor report");
        near(h,absorb(p,"getDamageAfterArmorAbsorb",p.damageSources().mobAttack(p),10),9,"actual native damage retains Breach armor penetration");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var damage=damage(p,p.damageSources().mobAttack(p),3);NeoForge.EVENT_BUS.post(damage);near(h,damage.getNewDamage(),2.6925,"already reduced damage only receives RPG defense once");
        var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);armor.enchant(p.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION),4);p.setItemSlot(EquipmentSlot.CHEST,armor);
        boolean nativeSpell=EquipmentDefense.nativeSpell(EquipmentDefense.magicSource(p));
        near(h,EquipmentDefense.percent(p,DefenseType.MAGICAL),0,"vanilla armor and its enchantments do not supply magic protection");
        near(h,absorb(p,"getDamageAfterArmorAbsorb",EquipmentDefense.magicSource(p),10),10,"actual native spell ignores ordinary armor");
        near(h,absorb(p,"getDamageAfterMagicAbsorb",EquipmentDefense.magicSource(p),10),10,"actual native spell ignores vanilla armor enchantments");
        var attr=net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks","spell_resist"));
        if(attr.isPresent()&&p.getAttribute(attr.get())!=null){p.getAttribute(attr.get()).setBaseValue(1.4);near(h,EquipmentDefense.percent(p,DefenseType.MAGICAL),40,"separate native resistance survives without armor magic contribution");p.getAttribute(attr.get()).setBaseValue(1);}
        near(h,EquipmentDefense.spellRemaining(2),.25,"native spell resistance soft cap");
        for(var material:materials){
            var stacks=new java.util.ArrayList<ItemStack>();
            for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM)if(item instanceof ArmorItem a&&a.getEquipmentSlot().getType()==EquipmentSlot.Type.HUMANOID_ARMOR&&a.getMaterial()==material&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft"))stacks.add(new ItemStack(item));
            equip(p,stacks);near(h,EquipmentDefense.percent(p,DefenseType.MAGICAL),0,"full vanilla set has no magic defense: "+material.unwrapKey().orElseThrow().location());
            near(h,absorb(p,"getDamageAfterArmorAbsorb",EquipmentDefense.magicSource(p),10),10,"native magic damage unchanged by "+material.unwrapKey().orElseThrow().location());
            near(h,absorb(p,"getDamageAfterArmorAbsorb",p.damageSources().mobAttack(p),10),10*(1-EquipmentDefense.percent(p,DefenseType.PHYSICAL)/100),"real physical damage matches vanilla UI");
        }
        if(nativeSpell){
            var wizard=new java.util.ArrayList<ItemStack>();
            for(String piece:new String[]{"helmet","chestplate","leggings","boots"})wizard.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks","wizard_"+piece))));
            equip(p,wizard);
            near(h,EquipmentDefense.armorAttribute(p,Attributes.ARMOR,false),10,"Iron's armor points halved physically");
            near(h,EquipmentDefense.armorAttribute(p,Attributes.ARMOR,true),10,"Iron's armor points halved magically");
            double reduced=EquipmentDefense.armorPercent(10,0);near(h,EquipmentDefense.percent(p,DefenseType.PHYSICAL),reduced,"balanced Iron's physical defense");near(h,EquipmentDefense.percent(p,DefenseType.MAGICAL),reduced,"balanced Iron's magic defense");
            near(h,absorb(p,"getDamageAfterArmorAbsorb",EquipmentDefense.magicSource(p),10),10*(1-reduced/100),"native spell damage uses balanced Iron's defense");
            near(h,absorb(p,"getDamageAfterArmorAbsorb",p.damageSources().magic(),10),10*(1-reduced/100),"Iron's armor also protects against vanilla magic");
            wizard.set(2,new ItemStack(Items.DIAMOND_LEGGINGS));equip(p,wizard);
            near(h,EquipmentDefense.armorAttribute(p,Attributes.ARMOR,true),7,"mixed set magic excludes diamond leggings");
            near(h,EquipmentDefense.armorAttribute(p,Attributes.ARMOR,false),13,"mixed set physical keeps full vanilla armor");
            var magic=EquipmentDefense.magicSource(p);near(h,absorb(p,"getDamageAfterArmorAbsorb",magic,10),10*(1-EquipmentDefense.percent(p,DefenseType.MAGICAL)/100),"mixed native spell damage matches UI");
            var ironChest=wizard.get(1);ironChest.enchant(p.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION),4);wizard.get(2).enchant(p.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.PROTECTION),4);
            near(h,absorb(p,"getDamageAfterMagicAbsorb",magic,10),8.4,"only Iron's enchanted chest protects against magic");
            var voidSource=p.damageSources().fellOutOfWorld();near(h,absorb(p,"getDamageAfterArmorAbsorb",voidSource,10),10,"void armor bypass unchanged");
        }
    }
    public static LivingDamageEvent.Pre damage(ServerPlayer player,net.minecraft.world.damagesource.DamageSource source,float amount){return new LivingDamageEvent.Pre(player,new net.neoforged.neoforge.common.damagesource.DamageContainer(source,amount));}
    private static float absorb(ServerPlayer p,String method,net.minecraft.world.damagesource.DamageSource source,float damage){
        try{
            var field=net.minecraft.world.entity.LivingEntity.class.getDeclaredField("damageContainers");field.setAccessible(true);
            var containers=(java.util.Stack<net.neoforged.neoforge.common.damagesource.DamageContainer>)field.get(p);
            containers.push(new net.neoforged.neoforge.common.damagesource.DamageContainer(source,damage));
            try{var call=net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod(method,net.minecraft.world.damagesource.DamageSource.class,float.class);call.setAccessible(true);return (Float)call.invoke(p,source,damage);}finally{containers.pop();}
        }catch(Exception error){throw new IllegalStateException("Actual native damage method failed",error);}
    }
    private static void equip(ServerPlayer p,java.util.List<ItemStack> stacks){
        for(var attribute:java.util.List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS)){p.getAttribute(attribute).removeModifiers();p.getAttribute(attribute).setBaseValue(0);}
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})p.setItemSlot(slot,ItemStack.EMPTY);
        for(var stack:stacks){var slot=((ArmorItem)stack.getItem()).getEquipmentSlot();p.setItemSlot(slot,stack);stack.forEachModifier(slot,(attribute,modifier)->{var instance=p.getAttribute(attribute);if(instance!=null)instance.addOrUpdateTransientModifier(modifier);});}
    }
    private FoodGameTests(){}
}

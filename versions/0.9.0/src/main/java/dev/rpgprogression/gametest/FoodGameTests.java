package dev.rpgprogression.gametest;

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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

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
        var starve=new LivingDamageEvent(p,p.damageSources().starve(),10);MinecraftForge.EVENT_BUS.post(starve);near(h,starve.getAmount(),0,"starvation cannot hurt");
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
        cake.use(state,p.serverLevel(),cakePos,p,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(cakePos),net.minecraft.core.Direction.UP,cakePos,false));
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
        double[] expected={8,24,28,40,70,72};ArmorMaterials[] materials={ArmorMaterials.LEATHER,ArmorMaterials.GOLD,ArmorMaterials.CHAIN,ArmorMaterials.IRON,ArmorMaterials.DIAMOND,ArmorMaterials.NETHERITE};
        for(int i=0;i<materials.length;i++){var m=materials[i];int armor=0;for(var type:ArmorItem.Type.values())armor+=m.getDefenseForType(type);near(h,EquipmentDefense.armorPercent(armor,m.getToughness()*4),expected[i],"vanilla armor "+m.getName());}
        p.getAttribute(Attributes.ARMOR).setBaseValue(20);p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(8);
        var d=ProgressionManager.get(p);d.set(RPGAttribute.VITALITY,20);eat(p,Items.MUSHROOM_STEW);
        near(h,EquipmentDefense.percent(p,DefenseType.PHYSICAL),70,"native attributes supply modded and vanilla armor");near(h,DefenseManager.displayedPercent(p,d,DefenseType.PHYSICAL),73.075,"UI composes equipment and RPG percentages");
        var damage=new LivingDamageEvent(p,p.damageSources().mobAttack(p),3);MinecraftForge.EVENT_BUS.post(damage);near(h,damage.getAmount(),2.6925,"already reduced damage only receives RPG defense once");
        var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);armor.enchant(Enchantments.ALL_DAMAGE_PROTECTION,4);p.setItemSlot(EquipmentSlot.CHEST,armor);
        boolean nativeSpell=EquipmentDefense.nativeSpell(EquipmentDefense.magicSource(p));
        near(h,EquipmentDefense.percent(p,DefenseType.MAGICAL),nativeSpell?74.8:16,"UI respects armor bypass of the representative magic source");
        var attr=net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new net.minecraft.resources.ResourceLocation("irons_spellbooks","spell_resist"));
        if(attr!=null&&p.getAttribute(attr)!=null){p.getAttribute(attr).setBaseValue(1.4);near(h,EquipmentDefense.percent(p,DefenseType.MAGICAL),86.896,"Iron's resistance acts before armor and enchantments");}
        near(h,EquipmentDefense.spellRemaining(2),.25,"native spell resistance soft cap");
    }
    private FoodGameTests(){}
}

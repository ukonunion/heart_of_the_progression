package dev.rpgprogression.attribute;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

public final class AttributeManager {
    private static final ResourceLocation DAMAGE=id("strength_damage"),HEALTH=id("vitality_health"),SPEED=id("agility_speed"),FATIGUE=id("exhausted_attack"),FOOD_SPEED=id("food_speed"),FOOD_ATTACK=id("food_attack");
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("rpgprogression",path);}
    public static double bonus(PlayerProgression d,RPGAttribute a) { return Math.max(0,d.get(a)-RPGConfig.START.get()); }
    public static void apply(Player p,PlayerProgression d) {
        modifier(p,Attributes.ATTACK_DAMAGE,DAMAGE,bonus(d,RPGAttribute.STRENGTH)*RPGConfig.STRENGTH_DAMAGE.get(),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.MAX_HEALTH,HEALTH,bonus(d,RPGAttribute.VITALITY)*RPGConfig.VITALITY_HP.get(),AttributeModifier.Operation.ADD_VALUE);
        modifier(p,Attributes.MOVEMENT_SPEED,SPEED,bonus(d,RPGAttribute.AGILITY)*RPGConfig.AGILITY_SPEED.get(),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.ATTACK_SPEED,FATIGUE,d.exhausted?RPGConfig.EXHAUSTED_ATTACK.get()-1:0,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        double food=dev.rpgprogression.food.FoodManager.energyMultiplier(d)-1;
        modifier(p,Attributes.MOVEMENT_SPEED,FOOD_SPEED,food,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        modifier(p,Attributes.ATTACK_SPEED,FOOD_ATTACK,food,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if(p.getHealth()>p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        d.stamina=Math.min(d.stamina,d.maxStamina());
    }
    private static void modifier(Player p,Holder<Attribute> a,ResourceLocation id,double amount,AttributeModifier.Operation op) {
        AttributeInstance instance=p.getAttribute(a); if(instance==null)return;
        AttributeModifier old=instance.getModifier(id);
        if(old!=null && old.amount()==amount && old.operation()==op)return;
        instance.removeModifier(id);
        if(amount!=0)instance.addTransientModifier(new AttributeModifier(id,amount,op));
    }
    public static double drawRatio(Player p) {
        return PlayerProgressionProvider.get(p).map(d->ProgressionMath.drawRatio(d.get(RPGAttribute.AGILITY),RPGConfig.START.get(),RPGConfig.DRAW_FACTOR.get(),RPGConfig.MIN_DRAW.get())).orElse(1.0);
    }
    private AttributeManager() {}
}

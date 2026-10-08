package dev.rpgprogression.attribute;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

public final class AttributeManager {
    private static final UUID DAMAGE=UUID.fromString("8304ef75-8b43-4af8-a08a-a08a027b8b4f");
    private static final UUID HEALTH=UUID.fromString("304fb2b7-ad64-4f4d-947b-399f243c42c6");
    private static final UUID SPEED=UUID.fromString("ed4f0fa2-fb77-4a0a-87f7-ae0a51eeb98a");
    private static final UUID FATIGUE=UUID.fromString("b63e3a87-5c5d-45de-89b7-f6d859abfdd8");
    public static double bonus(PlayerProgression d,RPGAttribute a) { return Math.max(0,d.get(a)-RPGConfig.START.get()); }
    public static void apply(Player p,PlayerProgression d) {
        modifier(p,Attributes.ATTACK_DAMAGE,DAMAGE,bonus(d,RPGAttribute.STRENGTH)*RPGConfig.STRENGTH_DAMAGE.get(),AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p,Attributes.MAX_HEALTH,HEALTH,bonus(d,RPGAttribute.VITALITY)*RPGConfig.VITALITY_HP.get(),AttributeModifier.Operation.ADDITION);
        modifier(p,Attributes.MOVEMENT_SPEED,SPEED,bonus(d,RPGAttribute.AGILITY)*RPGConfig.AGILITY_SPEED.get(),AttributeModifier.Operation.MULTIPLY_TOTAL);
        modifier(p,Attributes.ATTACK_SPEED,FATIGUE,d.exhausted?RPGConfig.EXHAUSTED_ATTACK.get()-1:0,AttributeModifier.Operation.MULTIPLY_TOTAL);
        if(p.getHealth()>p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        d.stamina=Math.min(d.stamina,d.maxStamina());
    }
    private static void modifier(Player p,Attribute a,UUID id,double amount,AttributeModifier.Operation op) {
        AttributeInstance instance=p.getAttribute(a); if(instance==null)return;
        AttributeModifier old=instance.getModifier(id);
        if(old!=null && old.getAmount()==amount && old.getOperation()==op)return;
        instance.removeModifier(id);
        if(amount!=0)instance.addTransientModifier(new AttributeModifier(id,"RPG Progression",amount,op));
    }
    public static double drawRatio(Player p) {
        return p.getCapability(PlayerProgressionProvider.CAPABILITY).map(d->ProgressionMath.drawRatio(d.get(RPGAttribute.AGILITY),RPGConfig.START.get(),RPGConfig.DRAW_FACTOR.get(),RPGConfig.MIN_DRAW.get())).orElse(1.0);
    }
    private AttributeManager() {}
}

package dev.rpgprogression.verification.gametest;
import dev.rpgprogression.compat.ParCoolBridge;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.entity.KillEntityEvent;

/** Actual equipped native skills, native kill events and resource substitution. */
public final class EpicStaminaSkillGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);var cap=patch.getPlayerSkills();var d=ProgressionManager.get(p);
        var first=cap.getSkillContainerFor(SkillSlots.PASSIVE1);var second=cap.getSkillContainerFor(SkillSlots.PASSIVE2);
        d.set(RPGAttribute.ENDURANCE,11);d.stamina=80;
        h.assertTrue(first.setSkill(EpicFightSkills.ENDURANCE.get()),"native Endurance equipped");
        near(h,d.maxStamina(),130,"RPG Endurance110 + native Endurance20");near(h,patch.getMaxStamina(),26,"native maximum uses augmented shared pool");near(h,d.stamina,80,"equip does not refill stamina");
        if(ParCoolCompat.enabled())near(h,ParCoolBridge.maximum(p),130,"ParCool sees the passive capacity bonus");
        d.set(RPGAttribute.ENDURANCE,20);near(h,d.maxStamina(),220,"native additive bonus stays20 at higher RPG attribute");
        var maximum=p.getAttribute(yesman.epicfight.registry.entries.EpicFightAttributes.MAX_STAMINA);
        var multiply=new AttributeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rpgprogression","test_native_multiplier"),.25,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        maximum.addTransientModifier(multiply);near(h,d.maxStamina(),275,"native multiply applies after RPG base and native addition");maximum.removeModifier(multiply);
        first.setSkill(null);d.set(RPGAttribute.ENDURANCE,10);d.stamina=120;d.regenCooldown=200;StaminaManager.tick(p,d);near(h,d.stamina,100,"removing a capacity skill clamps excess without refilling");
        first.setSkill(EpicFightSkills.TECHNICIAN.get());second.setSkill(EpicFightSkills.SWORD_MASTER.get());
        near(h,d.maxStamina(),110,"Swordmaster capacity bonus");near(h,EpicFightCompat.regeneration(p),1.6,"Technician and Swordmaster regeneration modifiers combine");
        d.stamina=40;d.regenCooldown=0;StaminaManager.tick(p,d);near(h,d.stamina,40+RPGConfig.REGEN.get()*1.6/20,"single RPG regeneration honors equipped native bonuses");
        first.setSkill(null);second.setSkill(null);near(h,EpicFightCompat.regeneration(p),1,"removing passives restores ordinary regeneration");

        var target=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW,1,2,1);
        first.setSkill(EpicFightSkills.STAMINA_PILLAGER.get());d.stamina=40;double expected=40+Math.min((d.maxStamina()-40)*.3,2*RPGConfig.EPICFIGHT_UNIT.get());
        EpicFightEventHooks.Entity.KILL_ENTITY.postWithListener(new KillEntityEvent(patch,target,p.damageSources().playerAttack(p)),patch.getEventListener());
        near(h,d.stamina,expected,"native Stamina Pillager kill refund reaches the shared pool");
        d.stamina=d.maxStamina()-5;expected=d.stamina+1.5;
        EpicFightEventHooks.Entity.KILL_ENTITY.postWithListener(new KillEntityEvent(patch,target,p.damageSources().playerAttack(p)),patch.getEventListener());
        near(h,d.stamina,expected,"small native kill refund remains a percentage, without a free full refill");
        first.setSkill(EpicFightSkills.ADRENALINE_FIEND.get());p.tickCount=500;d.stamina=50;d.regenCooldown=30;patch.setStaminaRegenAwaitTicks(30);
        EpicFightEventHooks.Entity.KILL_ENTITY.postWithListener(new KillEntityEvent(patch,target,p.damageSources().playerAttack(p)),patch.getEventListener());
        h.assertTrue(d.regenCooldown==0,"native Adrenaline kill removes RPG regeneration delay");
        StaminaManager.spend(p,d,1);h.assertTrue(d.regenCooldown==0,"new spending retains native Adrenaline kill window");
        p.tickCount+=31;StaminaManager.spend(p,d,1);h.assertTrue(d.regenCooldown==30,"ordinary delay returns after the native kill window");

        first.setSkill(EpicFightSkills.FORBIDDEN_STRENGTH.get());cap.getSkillContainerFor(SkillSlots.DODGE).setSkill(EpicFightSkills.ROLL.get());d.stamina=5;d.exhausted=false;p.setHealth(20);float health=p.getHealth();
        h.assertTrue(patch.consumeForSkill(EpicFightSkills.ROLL.get(),Skill.Resource.STAMINA,2),"Forbidden Strength allows a native skill with insufficient shared stamina");
        near(h,d.stamina,0,"Forbidden Strength empties shared stamina once");near(h,p.getHealth(),health-1,"native health substitution keeps Epic Fight units");
        first.setSkill(EpicFightSkills.HYPERVITALITY.get());first.setStack(1);var weapon=cap.getSkillContainerFor(SkillSlots.WEAPON_INNATE);weapon.setSkill(EpicFightSkills.DANCING_EDGE.get());weapon.setStack(0);
        d.stamina=80;d.exhausted=false;double before=d.stamina;
        h.assertTrue(patch.consumeForSkill(EpicFightSkills.DANCING_EDGE.get(),Skill.Resource.WEAPON_CHARGE,1),"Hypervitality substitutes shared stamina for missing weapon charge");
        near(h,d.stamina,before-EpicFightSkills.DANCING_EDGE.get().getConsumption()*.1*RPGConfig.EPICFIGHT_UNIT.get(),"Hypervitality native resource event debits shared pool once");
        first.setSkill(null);weapon.setSkill(null);
    }
    private static void near(GameTestHelper h,double actual,double expected,String message){h.assertTrue(Math.abs(actual-expected)<.001,message+": "+actual+" != "+expected);}
    private EpicStaminaSkillGameTests(){}
}

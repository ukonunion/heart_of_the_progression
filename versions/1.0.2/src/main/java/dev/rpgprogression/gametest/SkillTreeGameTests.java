package dev.rpgprogression.gametest;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.defense.*;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.SkillTreeManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
public final class SkillTreeGameTests {
    public static void common(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);d.level=4;d.awardSkillPoints();h.assertTrue(d.skillPoints==0,"no skill point before level5");
        d.level=5;d.awardSkillPoints();h.assertTrue(d.skillPoints==1&&d.points==0,"level5 grants separate skill point");
        d.level=17;d.awardSkillPoints();h.assertTrue(d.skillPoints==3,"retroactive milestones5/10/15");
        d.skillPoints--;d.level=4;d.awardSkillPoints();d.level=17;d.awardSkillPoints();h.assertTrue(d.skillPoints==2,"lowering and raising level cannot duplicate awards");
        d.unlockedSkills.add("epicfight:guard");var copy=new PlayerProgression();copy.load(d.save());copy.awardSkillPoints();
        h.assertTrue(copy.skillPoints==2&&copy.skillMilestones==3&&copy.unlockedSkills.contains("epicfight:guard"),"skill progress persists without a repeated award");
        int before=d.skillPoints;h.assertFalse(SkillTreeManager.unlock(p,"rpgprogression:nonexistent"),"unknown ID rejected");h.assertTrue(d.skillPoints==before,"unknown skill cannot charge points");
        var legacy=d.save();legacy.putInt("version",3);legacy.remove("attributeBaseline");legacy.remove("sparkBaseline");legacy.remove("skillMilestones");legacy.remove("skillPoints");
        legacy.putInt("strength",14);legacy.putInt("endurance",19);legacy.putInt("spark_potential",3);legacy.putDouble("spark",8);legacy.putDouble("stamina",37);
        copy.load(legacy);copy.initialize();copy.awardSkillPoints();
        h.assertTrue(copy.get(RPGAttribute.STRENGTH)==15&&copy.get(RPGAttribute.ENDURANCE)==20&&copy.get(RPGAttribute.SPARK_POTENTIAL)==12,"old purchased attribute increments retained");
        h.assertTrue(copy.spark==8&&copy.stamina==37&&copy.skillPoints==3,"existing resources and retroactive skill awards retained");
        var again=new PlayerProgression();again.load(copy.save());again.initialize();again.awardSkillPoints();
        h.assertTrue(again.get(RPGAttribute.SPARK_POTENTIAL)==12&&again.skillPoints==3,"migration and awards happen once");
        legacy.putInt("points",0);legacy.putInt("strength",100);legacy.putInt("spark_potential",100);
        again.load(legacy);again.initialize();h.assertTrue(again.points==10&&again.get(RPGAttribute.STRENGTH)==100&&again.get(RPGAttribute.SPARK_POTENTIAL)==100,"investments above new cap refunded as attribute points");
        p.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        h.assertFalse(SkillTreeManager.unlock(p,"epicfight:guard"),"removed player tree requests ignored");SkillTreeManager.sync(p);
    }
    public static void magic(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);d.set(RPGAttribute.INTELLIGENCE,20);
        h.assertTrue(Math.abs(DefenseManager.percent(d,DefenseType.MAGICAL)-4)<.001,"magic defense scales with Intelligence");
        h.assertTrue(DefenseManager.classify(p.damageSources().magic())==DefenseType.MAGICAL,"unattributed magic has magic defense");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100));
        h.assertTrue(DefenseManager.classify(p.damageSources().magic(),p)==DefenseType.POISON,"vanilla poison still uses poison defense");p.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
        h.assertTrue(DefenseManager.classify(p.damageSources().indirectMagic(p,p))==DefenseType.MAGICAL,"attributed magic uses magical defense");
        h.assertTrue(DefenseManager.classify(p.damageSources().freeze())==DefenseType.ELEMENTAL,"elemental category remains separate");
        // Forge FakePlayer is always invulnerable; exercise the actual damage event pipeline.
        var hurt=new net.minecraftforge.event.entity.living.LivingDamageEvent(p,p.damageSources().indirectMagic(p,p),10);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(hurt);h.assertTrue(Math.abs(hurt.getAmount()-9.6)<.001,"native LivingDamage event reduces magic damage once");
        d.set(RPGAttribute.INTELLIGENCE,10000);h.assertTrue(DefenseManager.percent(d,DefenseType.MAGICAL)==75,"magic defense respects cap");
        if(dev.rpgprogression.compat.IronsCompat.present())IronsTreeGameTests.magic(h,p);
    }
    private SkillTreeGameTests(){}
}

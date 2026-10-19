package dev.rpgprogression.verification.gametest;

import com.alrex.parcool.common.Parkourability;
import com.alrex.parcool.common.action.ParCoolActions;
import com.alrex.parcool.common.stamina.ReadonlyStamina;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Executes real ParCool action methods and optional mixins, not a stand-in stamina model. */
public final class ParCoolGameTests {
    public static void check(GameTestHelper h,ServerPlayer p) {
        var d=ProgressionManager.get(p);var parkour=Parkourability.get(p);
        com.alrex.parcool.ParCool.getActionRegistry().getRegisteredActions().values().forEach(a->d.unlockedSkills.add(a.id().toString()));
        h.assertTrue(parkour!=null,"ParCool attached to player");
        near(h,parkour.getStamina().value(),100,"shared initial stamina");
        parkour.get(ParCoolActions.WALL_JUMP).start();
        near(h,d.stamina,95,"wall jump charges configured 50 x 0.1 on server");
        var run=parkour.get(ParCoolActions.FAST_RUN);run.start();p.setSprinting(true);
        for(int i=0;i<20;i++){StaminaManager.tick(p,d);run.tickOnWorking();}
        near(h,d.stamina,91,"fast run costs 4 per second with no duplicate vanilla sprint charge");
        run.finish();p.setSprinting(false);StaminaManager.spend(p,d,12);
        near(h,parkour.getStamina().value(),79,"RPG melee spending visible to ParCool");
        var swim=parkour.get(ParCoolActions.FAST_SWIM);p.setPose(net.minecraft.world.entity.Pose.SWIMMING);p.setSwimming(true);p.setSprinting(true);swim.start();
        h.assertTrue(swim.isDoing(),"learned native fast swim starts");
        for(int i=0;i<20;i++){StaminaManager.tick(p,d);swim.tickOnWorking();}
        near(h,d.stamina,75,"fast swim charges native shared stamina without duplicate RPG sprint charge");
        swim.finish();p.setSwimming(false);p.setPose(net.minecraft.world.entity.Pose.STANDING);p.setSprinting(false);d.stamina=79;
        d.set(RPGAttribute.ENDURANCE,11);near(h,parkour.getStamina().max(),110,"shared Endurance maximum");
        parkour.updateStaminaInRemote(new ReadonlyStamina(9999,9999,false,false));
        near(h,parkour.getStamina().value(),79,"client ParCool snapshot cannot overwrite server RPG stamina");
        d.set(RPGAttribute.ENDURANCE,10);run.start();StaminaManager.spend(p,d,1000);run.tickOnWorking();
        h.assertFalse(run.isDoing(),"exhaustion stops continuous parkour on server");
        run.start();h.assertFalse(run.isDoing(),"exhausted start blocked before doing flag changes");
        h.assertTrue(parkour.getStamina().isExhausted(),"shared exhaustion flag");
        d.stamina=14;StaminaManager.updateExhaustion(p,d);h.assertTrue(parkour.getStamina().isExhausted(),"shared recovery hysteresis");
        d.stamina=15;StaminaManager.updateExhaustion(p,d);h.assertFalse(parkour.getStamina().isExhausted(),"shared recovery threshold");
        run.start();h.assertTrue(run.isDoing(),"parkour usable after recovery");run.finish();
        double old=d.stamina;RPGConfig.PARCOOL_COMPAT.set(false);
        try {parkour.get(ParCoolActions.WALL_JUMP).start();near(h,d.stamina,old,"disabled bridge leaves RPG stamina alone");}
        finally {RPGConfig.PARCOOL_COMPAT.set(true);}
    }
    private static void near(GameTestHelper h,double value,double expected,String message){h.assertTrue(Math.abs(value-expected)<0.001,message+": "+value+" != "+expected);}
    private ParCoolGameTests() {}
}

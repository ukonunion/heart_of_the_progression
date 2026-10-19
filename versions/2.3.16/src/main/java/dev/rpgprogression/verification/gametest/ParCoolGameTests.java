package dev.rpgprogression.verification.gametest;
import com.alrex.parcool.common.action.*;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.attachment.common.*;
import com.alrex.parcool.common.network.payload.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.stamina.StaminaManager;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.nio.ByteBuffer;
import java.util.*;
public final class ParCoolGameTests {
    public static void check(GameTestHelper h,ServerPlayer p){
        var d=ProgressionManager.get(p);var parkour=Parkourability.get(p);var processor=new ActionProcessor();
        ParCoolSkills.catalog(p).forEach(n->d.unlockedSkills.add(n.id()));d.regenCooldown=20000;
        near(h,ParCoolBridge.value(p),100,"initial shared pool");
        var jump=parkour.get(WallJump.class);h.assertFalse(ParCoolBridge.blocked(p,jump),"jump precondition alive="+p.isAlive()+", removed="+p.isRemoved()+", cost="+ParCoolBridge.cost(p,jump)+", exhausted="+d.exhausted+", learned="+d.unlockedSkills.contains(ParCoolSkills.id(jump.getClass())));jump.start(p,parkour,ByteBuffer.allocate(128));near(h,d.stamina,95,"wall jump retains five-unit price");jump.finish(p);
        var run=parkour.get(FastRun.class);run.start(p,parkour,ByteBuffer.allocate(128));p.setSprinting(true);
        for(int i=0;i<20;i++){StaminaManager.tick(p,d);processor.onTick(new PlayerTickEvent.Post(p));}
        near(h,d.stamina,91,"native processor fast run costs four with no duplicate sprint debit");run.finish(p);p.setSprinting(false);StaminaManager.spend(p,d,12);near(h,ParCoolBridge.value(p),79,"melee spending visible in native mirror");
        var swim=parkour.get(FastSwim.class);p.setPose(net.minecraft.world.entity.Pose.SWIMMING);p.setSwimming(true);p.setSprinting(true);swim.start(p,parkour,ByteBuffer.allocate(128));
        for(int i=0;i<20;i++){StaminaManager.tick(p,d);processor.onTick(new PlayerTickEvent.Post(p));}
        near(h,d.stamina,75,"accelerated swimming retains four-unit price without duplicate debit");swim.finish(p);p.setSwimming(false);p.setPose(net.minecraft.world.entity.Pose.STANDING);p.setSprinting(false);
        d.stamina=79;d.set(RPGAttribute.ENDURANCE,11);near(h,ParCoolBridge.maximum(p),110,"native maximum follows Endurance");
        StaminaPayload.handleServer(new StaminaPayload(p.getUUID(),new ReadonlyStamina(false,9999999,9999999)),TestPayloadContext.of(p));near(h,d.stamina,79,"native client snapshot cannot refill or enlarge server pool");
        d.set(RPGAttribute.ENDURANCE,10);run.start(p,parkour,ByteBuffer.allocate(128));StaminaManager.spend(p,d,1000);processor.onTick(new PlayerTickEvent.Post(p));h.assertFalse(run.isDoing(),"exhaustion stops continuous action");
        run.start(p,parkour,ByteBuffer.allocate(128));h.assertFalse(run.isDoing(),"exhausted direct start rejected");
        d.stamina=14;StaminaManager.updateExhaustion(p,d);ParCoolBridge.mirror(p);h.assertTrue(p.getData(com.alrex.parcool.common.attachment.Attachments.STAMINA).isExhausted(),"native mirror preserves hysteresis");
        d.stamina=15;StaminaManager.updateExhaustion(p,d);run.start(p,parkour,ByteBuffer.allocate(128));h.assertTrue(run.isDoing(),"native action resumes at recovery threshold");run.finish(p);
        d.stamina=100;d.exhausted=false;
        if(net.neoforged.fml.ModList.get().isLoaded("epicparcool")){
            RangedEpicSetup.mode(p,true);var dodge=parkour.get(Dodge.class);double cost=ParCoolBridge.cost(p,dodge);
            ActionStatePayload.handleServer(new ActionStatePayload(p.getUUID(),List.of(new ActionStatePayload.Entry(Dodge.class,ActionStatePayload.Entry.Type.Start,new byte[128]))),TestPayloadContext.of(p));
            near(h,d.stamina,100-StaminaManager.cost(p,cost),"combined dodge charges native Epic price exactly once");double after=d.stamina;
            ActionStatePayload.handleServer(new ActionStatePayload(p.getUUID(),List.of(new ActionStatePayload.Entry(Dodge.class,ActionStatePayload.Entry.Type.Start,new byte[128]))),TestPayloadContext.of(p));near(h,d.stamina,after,"duplicate start cannot double charge");dodge.finish(p);RangedEpicSetup.mode(p,false);
        }
        double before=d.stamina;RPGConfig.PARCOOL_COMPAT.set(false);try{jump.start(p,parkour,ByteBuffer.allocate(128));near(h,d.stamina,before,"disabled sharing leaves RPG pool untouched");}finally{jump.finish(p);RPGConfig.PARCOOL_COMPAT.set(true);}
    }
    private static void near(GameTestHelper h,double got,double expected,String message){h.assertTrue(Math.abs(got-expected)<.0011,message+": "+got+" != "+expected);}
}

package dev.rpgprogression.verification.client;

import com.alrex.parcool.common.action.impl.Dodge;
import com.alrex.parcool.common.attachment.common.Parkourability;
import com.alrex.parcool.common.network.payload.ActionStatePayload;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import dev.rpgprogression.compat.*;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.progression.ProgressionManager;
import dev.rpgprogression.network.ModNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import java.nio.ByteBuffer;
import java.util.List;

/** Exercise the actual bridge animation event and the real client-to-server action packet. */
public final class BridgeClientChecks {
    public static void prepare(ServerPlayer player){
        var d=ProgressionManager.get(player);
        ParCoolSkills.catalog(player).forEach(n->d.unlockedSkills.add(n.id()));
        d.set(RPGAttribute.ENDURANCE,10);d.stamina=100;d.exhausted=false;d.regenCooldown=10000;
        EpicFightCapabilities.getEntityPatch(player,PlayerPatch.class).toEpicFightMode(true);
        ModNetworking.sync(player);
    }
    public static void begin(Minecraft mc){
        var patch=EpicFightCapabilities.getEntityPatch(mc.player,PlayerPatch.class);
        if(!patch.isEpicFightMode())throw new IllegalStateException("Native battle mode did not synchronize");
        var parkour=Parkourability.get(mc.player);var dodge=parkour.get(Dodge.class);
        var buffer=ByteBuffer.allocate(20).putInt(0).putDouble(0).putDouble(-1);buffer.flip();
        dodge.start(mc.player,parkour,buffer);
        if(!dodge.isDoing())throw new IllegalStateException("Learned client Dodge did not start");
        NeoForge.EVENT_BUS.post(new ParCoolActionEvent.StartEvent(mc.player,dodge));
        PacketDistributor.sendToServer(new ActionStatePayload(mc.player.getUUID(),List.of(new ActionStatePayload.Entry(Dodge.class,ActionStatePayload.Entry.Type.Start,buffer.array()))));
    }
    public static String animation(Minecraft mc){
        var patch=EpicFightCapabilities.getEntityPatch(mc.player,PlayerPatch.class);
        return patch.getClientAnimator().getAllLayers().stream().map(layer->String.valueOf(layer.animationPlayer.getRealAnimation().get().getRegistryName())).filter(name->name.startsWith("epicparcool:")).findFirst().orElse("none");
    }
    public static boolean serverPrice(ServerPlayer player){
        double expected=100-yesman.epicfight.registry.entries.EpicFightSkills.ROLL.get().getConsumption()*dev.rpgprogression.config.RPGConfig.EPICFIGHT_UNIT.get();
        var d=ProgressionManager.get(player);
        com.mojang.logging.LogUtils.getLogger().info("HEART_BRIDGE_PRICE actual={} expected={} battle={} endurance={} doing={}",d.stamina,expected,EpicFightCompat.battle(player),d.get(RPGAttribute.ENDURANCE),Parkourability.get(player).get(Dodge.class).isDoing());
        return Math.abs(d.stamina-expected)<.001;
    }
    public static void reset(ServerPlayer player){EpicFightCapabilities.getEntityPatch(player,PlayerPatch.class).toVanillaMode(true);}
    private BridgeClientChecks(){}
}

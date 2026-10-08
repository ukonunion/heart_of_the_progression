package dev.rpgprogression.gametest.client;

import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.compat.ParCoolCompat;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.client.Minecraft;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import java.util.function.BiConsumer;

/** Native Epic Fight skill consumer and native C2S packet, with authoritative S2C verification. */
public final class EpicFightClientSmoke {
    public static void tick(int tick,Minecraft mc,BiConsumer<Boolean,String> check){
        if(tick==320)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            var d=ProgressionManager.get(p);d.stamina=80;d.exhausted=false;d.regenCooldown=400;
            var patch=EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);
            check.accept(patch!=null,"Epic Fight server patch attached");
            Skill.Resource.STAMINA.consumer.consume(null,patch,3);d.regenCooldown=400;ModNetworking.stamina(p);
        });
        if(tick==340){
            var patch=EpicFightCapabilities.getEntityPatch(mc.player,LocalPlayerPatch.class);
            check.accept(patch!=null&&Math.abs(patch.getStamina()-13)<.001&&Math.abs(patch.getMaxStamina()-22)<.001,"Epic Fight local getters reflect server skill cost and Endurance");
            patch.setStamina(11);
        }
        if(tick==360){
            check.accept(Math.abs(ClientData.get().stamina-55)<.001,"Epic Fight native C2S debit arrives exactly once");
            var patch=EpicFightCapabilities.getEntityPatch(mc.player,LocalPlayerPatch.class);patch.setStamina(9999);patch.setStamina(Float.NaN);
            mc.getSingleplayerServer().execute(()->ProgressionManager.get(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())).regenCooldown=200);
        }
        if(tick==380){
            check.accept(Math.abs(ClientData.get().stamina-55)<.001,"Epic Fight native C2S cannot inject a refill or NaN");
            if(ParCoolCompat.enabled())check.accept(Math.abs(com.alrex.parcool.common.Parkourability.get(mc.player).getStamina().value()-55)<.001,"ParCool observes Epic Fight spending on the client");
        }
    }
    private EpicFightClientSmoke(){}
}

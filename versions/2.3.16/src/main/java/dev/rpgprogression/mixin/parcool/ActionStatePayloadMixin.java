package dev.rpgprogression.mixin.parcool;
import com.alrex.parcool.common.network.payload.ActionStatePayload;
import com.alrex.parcool.common.attachment.common.Parkourability;
import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import dev.rpgprogression.compat.*;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;
@Pseudo @Mixin(value=ActionStatePayload.class,remap=false)
public abstract class ActionStatePayloadMixin {
    @Inject(method="handleServer",at=@At("HEAD"),cancellable=true)
    private static void rpg$authorizedActions(ActionStatePayload msg,IPayloadContext ctx,CallbackInfo ci){
        if(!ParCoolCompat.present())return;
        ci.cancel();ctx.enqueueWork(()->{
            var p=ctx.player();if(!p.getUUID().equals(msg.playerID())||p.isRemoved()||!p.isAlive()||msg.states().size()>64)return;
            var parkour=Parkourability.get(p);var accepted=new ArrayList<ActionStatePayload.Entry>();
            for(var state:msg.states()){
                var action=parkour.get(state.action());if(action==null)continue;
                try {
                    switch(state.type()){
                        case Start -> {if(action.isDoing()||ParCoolBridge.blocked(p,action)||!(p instanceof net.minecraft.server.level.ServerPlayer server)||!com.alrex.parcool.common.info.ServerLimitation.get(server).isPermitted(state.action()))continue;
                            NeoForge.EVENT_BUS.post(new ParCoolActionEvent.Start.Pre(p,action));action.start(p,parkour,state.getDataAsBuffer());
                            if(!action.isDoing())continue;NeoForge.EVENT_BUS.post(new ParCoolActionEvent.StartEvent(p,action));NeoForge.EVENT_BUS.post(new ParCoolActionEvent.Start.Post(p,action));}
                        case Normal -> {if(!action.isDoing()||ParCoolBridge.blocked(p,action))continue;action.restoreSynchronizedState(state.getDataAsBuffer());}
                        case Finish -> {if(!action.isDoing())continue;NeoForge.EVENT_BUS.post(new ParCoolActionEvent.Finish.Pre(p,action));action.finish(p);NeoForge.EVENT_BUS.post(new ParCoolActionEvent.StopEvent(p,action));NeoForge.EVENT_BUS.post(new ParCoolActionEvent.Finish.Post(p,action));}
                    }
                    accepted.add(state);
                } catch(java.nio.BufferUnderflowException|IndexOutOfBoundsException ignored){
                    // A malformed start can set native doing=true before its callback fails.
                    if(action.isDoing())action.finish(p);
                }
            }
            if(!accepted.isEmpty())PacketDistributor.sendToAllPlayers(new ActionStatePayload(p.getUUID(),accepted));
            ParCoolBridge.mirror(p);
        });
    }
}

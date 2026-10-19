package dev.rpgprogression.compat;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Reject unavailable actions before ParCool and the animation bridge emit start events. */
public final class ParCoolActionGuards {
    public static void register(){
        NeoForge.EVENT_BUS.addListener((ParCoolActionEvent.TryToStartEvent event)->{
            if(ParCoolBridge.blocked(event.getPlayer(),event.getAction()))event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ParCoolActionEvent.TryToContinueEvent event)->{
            if(ParCoolBridge.blocked(event.getPlayer(),event.getAction()))event.setCanceled(true);
        });
    }
    private ParCoolActionGuards(){}
}

package dev.rpgprogression.gametest.client;

import com.mojang.logging.LogUtils;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in real connection/death/respawn regression; never included in the release JAR. */
@Mod.EventBusSubscriber(modid="rpgprogression",value=Dist.CLIENT)
public final class LifecycleClientSmoke {
    private static boolean loading;
    private static int ticks;
    private static volatile boolean failed,deadSaved;
    private static ServerPlayer oldPlayer;
    private static double savedSpark;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("rpgprogression.lifecycleSmoke")||event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(mc.screen,System.getProperty("rpgprogression.smokeWorld","RPGCrashRegression"));return;}
        if(loading&&mc.player==null){
            String label=mc.screen instanceof BackupConfirmScreen?net.minecraft.network.chat.Component.translatable("selectWorld.backupJoinConfirmButton").getString():mc.screen instanceof ConfirmScreen?net.minecraft.network.chat.CommonComponents.GUI_PROCEED.getString():null;
            if(label!=null)for(var child:mc.screen.children())if(child instanceof Button button&&button.getMessage().getString().equals(label)){button.onPress();break;}
            return;
        }
        if(mc.player==null||mc.getSingleplayerServer()==null)return;
        ticks++;
        boolean rejoin=Boolean.getBoolean("rpgprogression.lifecycleRelogin");
        if(rejoin){
            if(ticks==20)server(mc,()->{
                oldPlayer=player(mc);
                check(oldPlayer.isRemoved()&&!oldPlayer.isAlive(),"saved dead player removed during login tick");
                check(!oldPlayer.getCapability(PlayerProgressionProvider.CAPABILITY).isPresent(),"login survives unavailable old capability");
            });
            if(ticks==30)mc.player.respawn();
            if(ticks==70){checkClient();server(mc,()->checkReplacement(mc));}
            if(ticks==90)finish(mc,"RELOGIN");
            return;
        }
        if(ticks==20)server(mc,()->{
            var p=player(mc);p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.setSprinting(false);
            var d=ProgressionManager.get(p);d.level=12;d.xp=542;d.points=2;
            d.set(RPGAttribute.STRENGTH,19);d.set(RPGAttribute.ENDURANCE,17);d.set(RPGAttribute.VITALITY,15);d.set(RPGAttribute.SPARK_POTENTIAL,3);
            ProgressionManager.refresh(p,d.get(RPGAttribute.INTELLIGENCE),false);p.setHealth(p.getMaxHealth());
            d.stamina=37;d.regenCooldown=20000;d.spark=8;ModNetworking.sync(p);
        });
        if(ticks==45){checkClient();check(Math.abs(ClientData.get().stamina-37)<.001,"real client receives pre-death stamina");}
        if(ticks==50)server(mc,()->{
            oldPlayer=player(mc);savedSpark=ProgressionManager.get(oldPlayer).spark;oldPlayer.getAbilities().invulnerable=false;oldPlayer.kill();
        });
        if(ticks==85)server(mc,()->{
            check(oldPlayer.isRemoved()&&!oldPlayer.isAlive(),"real death reaches KILLED removal");
            check(!oldPlayer.getCapability(PlayerProgressionProvider.CAPABILITY).isPresent(),"real death invalidates old capability without a tick crash");
        });
        if(ticks==90)mc.player.respawn();
        if(ticks==125){checkClient();server(mc,()->checkReplacement(mc));}
        if(ticks==140)server(mc,()->{
            var p=player(mc);p.kill();p.deathTime=19;
            mc.getSingleplayerServer().getPlayerList().saveAll();deadSaved=true;
        });
        if(ticks==160){check(deadSaved,"dead player saved for next-launch regression");finish(mc,"DEATH");}
    }
    private static ServerPlayer player(Minecraft mc){return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());}
    private static void checkReplacement(Minecraft mc){
        var p=player(mc);var d=ProgressionManager.get(p);
        check(p!=oldPlayer&&p.isAlive()&&!p.isRemoved(),"respawn replaces the old entity");
        check(d.level==12&&d.xp==542&&d.points==2&&d.get(RPGAttribute.STRENGTH)==19,"server retains level, XP, points and attributes");
        check(Math.abs(d.stamina-d.maxStamina())<.001,"respawn restores stamina");
        check(d.spark>=savedSpark&&d.spark<10,"spark survives death and stays partially filled");
        // Other mods can revive the original capability in their later Clone listeners.
        // Its dead/removed entity must still be ignored even if that handle is available.
        check(!oldPlayer.isAlive()||oldPlayer.isRemoved(),"old player remains inactive after respawn");
        oldPlayer.getCapability(PlayerProgressionProvider.CAPABILITY).ifPresent(old->{
            double stamina=old.stamina;int delay=old.regenCooldown;
            dev.rpgprogression.event.CommonEvents.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,oldPlayer));
            check(old.stamina==stamina&&old.regenCooldown==delay,"revived handle of old player cannot run resource ticks");
        });
    }
    private static void checkClient(){
        var d=ClientData.get();check(d.initialized&&d.level==12&&d.xp==542&&d.points==2&&d.get(RPGAttribute.STRENGTH)==19,"client receives retained progression");
    }
    private static void server(Minecraft mc,Runnable action){mc.getSingleplayerServer().execute(()->{try{action.run();}catch(Throwable error){failed=true;LogUtils.getLogger().error("RPG_LIFECYCLE exception",error);}});}
    private static void check(boolean condition,String message){if(!condition)failed=true;LogUtils.getLogger().info("RPG_LIFECYCLE {}: {}",condition?"verified":"FAILED",message);}
    private static void finish(Minecraft mc,String stage){LogUtils.getLogger().info("RPG_LIFECYCLE_{} {}",stage,failed?"FAILED":"PASSED");mc.stop();}
    private LifecycleClientSmoke(){}
}

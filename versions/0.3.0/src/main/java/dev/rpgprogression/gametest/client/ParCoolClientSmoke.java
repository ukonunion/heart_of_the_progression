package dev.rpgprogression.gametest.client;

import com.alrex.parcool.api.stamina.AbstractLocalStamina;
import com.alrex.parcool.common.Parkourability;
import com.alrex.parcool.common.action.ParCoolActions;
import dev.rpgprogression.client.ClientData;
import dev.rpgprogression.compat.RPGLocalStamina;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.client.Minecraft;
import java.util.function.BiConsumer;

public final class ParCoolClientSmoke {
    public static void tick(int tick,Minecraft mc,BiConsumer<Boolean,String> check){
        if(tick==220)mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            var d=ProgressionManager.get(p);d.stamina=80;d.regenCooldown=200;ModNetworking.stamina(p);
        });
        if(tick==240){
            var view=Parkourability.get(mc.player).getStamina();
            check.accept(view instanceof RPGLocalStamina,"ParCool client uses RPG stamina adapter");
            check.accept(Math.abs(view.value()-80)<0.001&&Math.abs(view.max()-110)<0.001,"ParCool sees server stamina and Endurance maximum");
            check.accept(!((AbstractLocalStamina)view).showHud(),"duplicate ParCool stamina HUD disabled");
            Parkourability.get(mc.player).get(ParCoolActions.WALL_JUMP).startExplicitly();
            check.accept(Math.abs(ClientData.get().stamina-75)<0.001,"local parkour cost predicts shared stamina");
        }
        if(tick==255){
            check.accept(Math.abs(ClientData.get().stamina-75)<0.001,"ParCool action C2S and RPG stamina S2C agree");
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                check.accept(Math.abs(ProgressionManager.get(p).stamina-75)<0.001,"server charges native ParCool action packet exactly once");
            });
        }
    }
    private ParCoolClientSmoke(){}
}

package dev.rpgprogression.verification.gametest;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;
public final class AttributeBalanceParCoolChecks {
    public static void run(ServerPlayer p,BiConsumer<Boolean,String> check){
        var d=ProgressionManager.get(p);var parkour=com.alrex.parcool.common.Parkourability.get(p);
        com.alrex.parcool.ParCool.getActionRegistry().getRegisteredActions().values().forEach(a->d.unlockedSkills.add(a.id().toString()));
        d.stamina=100;d.exhausted=false;parkour.get(com.alrex.parcool.common.action.ParCoolActions.WALL_JUMP).start();
        check.accept(Math.abs(d.stamina-96.5)<.001,"native ParCool wall jump pays 3.5 rather than five");
    }
    private AttributeBalanceParCoolChecks(){}
}

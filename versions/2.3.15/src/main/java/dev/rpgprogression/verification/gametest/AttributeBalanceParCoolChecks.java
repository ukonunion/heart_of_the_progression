package dev.rpgprogression.verification.gametest;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BiConsumer;
public final class AttributeBalanceParCoolChecks {
    public static void run(ServerPlayer p,BiConsumer<Boolean,String> check){
        var d=ProgressionManager.get(p);var parkour=com.alrex.parcool.common.attachment.common.Parkourability.get(p);
        dev.rpgprogression.compat.ParCoolSkills.catalog(p).forEach(a->d.unlockedSkills.add(a.id()));d.stamina=100;d.exhausted=false;
        parkour.get(com.alrex.parcool.common.action.impl.WallJump.class).start(p,parkour,java.nio.ByteBuffer.allocate(128));
        check.accept(Math.abs(d.stamina-96.5)<.001,"native ParCool3 wall jump receives thirty-percent discount once");
    }
}

package dev.rpgprogression.mixin.parcool;

import com.alrex.parcool.common.action.AdditionalProperties;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.attachment.common.Parkourability;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** The independent starting skill can prepare a leap from ordinary sprinting. */
@Pseudo @Mixin(value=CatLeap.class,remap=false)
public abstract class IndependentLongJumpMixin {
    @Redirect(method="onClientTick",at=@At(value="INVOKE",target="Lcom/alrex/parcool/common/action/impl/FastRun;getNotDashTick(Lcom/alrex/parcool/common/action/AdditionalProperties;)I"))
    private int rpg$independentSprint(FastRun run,AdditionalProperties properties,Player player,Parkourability parkour){
        var skills=ProgressionManager.get(player).unlockedSkills;
        return skills.contains("parcool:long_jump")&&!skills.contains("parcool:fast_run")?properties.getNotSprintingTick():run.getNotDashTick(properties);
    }
}

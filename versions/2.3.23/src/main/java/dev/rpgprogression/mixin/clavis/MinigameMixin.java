package dev.rpgprogression.mixin.clavis;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.rpgprogression.compat.ClavisCompat;
import net.minecraft.client.Minecraft;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Keep Clavis's mistake penalty, rules and recovery animation when a life is saved. */
@Pseudo
@Mixin(targets="it.hurts.shatterbyte.clavis.common.minigame.Minigame",remap=false)
public abstract class MinigameMixin {
    @Unique private boolean rpgprogression$savedLife;

    @ModifyExpressionValue(method="hurt",at=@At(value="FIELD",target="Lit/hurts/shatterbyte/clavis/common/minigame/Minigame;health:I",opcode=Opcodes.GETFIELD,ordinal=0),remap=false)
    private int rpgprogression$saveLife(int health) {
        var player=Minecraft.getInstance().player;
        double chance=ClavisCompat.lifeSaveChance(player);
        rpgprogression$savedLife=health>0&&chance>0&&player!=null&&player.getRandom().nextDouble()<chance;
        // Clavis subtracts one immediately after this read. Its later loss check sees the real value.
        return health+(rpgprogression$savedLife?1:0);
    }

    @WrapWithCondition(method="hurt",at=@At(value="INVOKE",target="Lit/hurts/shatterbyte/clavis/common/client/screen/LockpickingScreen;animateHeart()V"),remap=false)
    private boolean rpgprogression$onlyBreakLostHearts(@Coerce Object screen) {
        return !rpgprogression$savedLife;
    }
}

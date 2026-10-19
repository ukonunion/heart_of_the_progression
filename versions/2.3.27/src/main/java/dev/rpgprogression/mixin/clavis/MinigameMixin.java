package dev.rpgprogression.mixin.clavis;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.rpgprogression.client.hud.ClavisHeartRewind;
import dev.rpgprogression.compat.ClavisCompat;
import net.minecraft.client.Minecraft;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** A single successful Agility roll protects both the life and loot quality. */
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

    @ModifyExpressionValue(method="hurt",at=@At(value="INVOKE",target="Lit/hurts/shatterbyte/clavis/common/config/Config;getQualityPenaltyPerHit()F"),remap=false)
    private float rpgprogression$keepLootQuality(float penalty) {
        return rpgprogression$savedLife?0:penalty;
    }

    @ModifyReturnValue(method="getHealth",at=@At("RETURN"),remap=false)
    private int rpgprogression$displayLostHeart(int actual){return ClavisHeartRewind.displayedHealth(this,actual);}

    @WrapOperation(method="hurt",at=@At(value="INVOKE",target="Lit/hurts/shatterbyte/clavis/common/client/screen/LockpickingScreen;animateHeart()V"),remap=false)
    private void rpgprogression$rewindSavedHeart(@Coerce Object screen,Operation<Void> original) {
        if(rpgprogression$savedLife&&screen instanceof net.minecraft.client.gui.screens.Screen owner){
            ClavisHeartRewind.begin(owner,this);var before=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>());before.addAll(ClavisHeartRewind.particles(owner));
            original.call(screen);ClavisHeartRewind.capture(owner,before);
        }else{
            if(screen instanceof net.minecraft.client.gui.screens.Screen owner)ClavisHeartRewind.clear(owner);
            original.call(screen);
        }
    }
}

package dev.rpgprogression.mixin;

import dev.rpgprogression.xp.XPManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** BreakEvent fires before removal; this hook awards XP only after a confirmed removal. */
@Mixin(ServerPlayerGameMode.class)
public abstract class MiningMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;
    @Unique private BlockState rpg$before;
    @Inject(method="removeBlock",at=@At("HEAD"),remap=false)
    private void rpg$capture(BlockPos pos,boolean harvest,CallbackInfoReturnable<Boolean> cir){rpg$before=level.getBlockState(pos);}
    @Inject(method="removeBlock",at=@At("RETURN"),remap=false)
    private void rpg$mined(BlockPos pos,boolean harvest,CallbackInfoReturnable<Boolean> cir){if(cir.getReturnValueZ()&&rpg$before!=null)XPManager.mined(player,pos,rpg$before);}
}

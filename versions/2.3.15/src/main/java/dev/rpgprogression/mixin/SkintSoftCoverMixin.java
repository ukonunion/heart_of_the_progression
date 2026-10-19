package dev.rpgprogression.mixin;

import dev.rpgprogression.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Carpets and snow bypass sturdy-face checks; the Skint must not support these either. */
@Mixin({CarpetBlock.class,SnowLayerBlock.class})
public abstract class SkintSoftCoverMixin {
    @Inject(method="canSurvive",at=@At("HEAD"),cancellable=true)
    private void skintNoCover(BlockState state,LevelReader level,BlockPos pos,CallbackInfoReturnable<Boolean> callback){
        if(level.getBlockState(pos.below()).is(ModBlocks.TRAVELSKINT.get()))callback.setReturnValue(false);
    }
}

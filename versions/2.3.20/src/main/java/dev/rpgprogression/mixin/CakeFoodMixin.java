package dev.rpgprogression.mixin;

import dev.rpgprogression.food.FoodManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CakeBlock.class)
public abstract class CakeFoodMixin {
    @Inject(method="eat",at=@At(value="INVOKE",target="Lnet/minecraft/world/food/FoodData;eat(IF)V",shift=At.Shift.AFTER))
    private static void rpg$cakeBite(LevelAccessor level,BlockPos pos,BlockState state,Player player,CallbackInfoReturnable<InteractionResult> cir){if(player instanceof ServerPlayer p)FoodManager.cake(p);}
}

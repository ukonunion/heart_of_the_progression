package dev.rpgprogression.mixin;

import dev.rpgprogression.xp.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FurnaceResultSlot.class)
public abstract class FurnaceResultMixin implements FurnaceXPAccess {
    @Shadow @Final private Player player;
    @Shadow private int removeCount;
    @Unique private boolean rpg$quickResult;
    /** Ordinary pickup uses the native counter. Shift-click uses the original stack before it is emptied. */
    @Inject(method="checkTakeAchievements",at=@At("HEAD"))
    private void rpg$cooked(ItemStack stack,CallbackInfo ci){if(!rpg$quickResult&&player instanceof ServerPlayer p&&removeCount>0&&!stack.isEmpty())XPManager.cooked(p,stack.copyWithCount(removeCount),false);}
    @Unique public void rpg$beginQuickResult(ItemStack original,int count){rpg$quickResult=true;if(count>0&&player instanceof ServerPlayer p)XPManager.cooked(p,original.copyWithCount(count),false);}
    @Unique public void rpg$endQuickResult(){rpg$quickResult=false;}
}

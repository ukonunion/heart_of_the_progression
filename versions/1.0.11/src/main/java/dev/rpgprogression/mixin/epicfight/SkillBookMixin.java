package dev.rpgprogression.mixin.epicfight;
import dev.rpgprogression.skill.SkillTreeManager;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="yesman.epicfight.world.item.SkillBookItem",remap=false)
public abstract class SkillBookMixin {
    @Inject(method={"use","m_7203_"},at=@At("HEAD"),cancellable=true)
    private void rpg$noBooks(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir){
        if(SkillTreeManager.epic()){
            if(!level.isClientSide)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("rpgprogression.skills.books_disabled"),true);
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }
}

package dev.rpgprogression.mixin.irons;

import dev.rpgprogression.compat.IronsRequirements;
import dev.rpgprogression.requirements.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo @Mixin(value=AbstractSpell.class,remap=false)
public abstract class SpellRequirementsMixin {
    @Inject(method="canBeCastedBy",at=@At("HEAD"),cancellable=true)
    private void rpg$spell(int level,CastSource source,MagicData magic,Player player,CallbackInfoReturnable<CastResult> cir){
        if(!IronsRequirements.spellAllowed(player,(AbstractSpell)(Object)this,level,source)){
            var missing=IronsRequirements.spellUnmet(player,(AbstractSpell)(Object)this,level);
            var message=Component.translatable("rpgprogression.requirements.blocked");
            if(!missing.isEmpty()){
                message=Component.translatable("rpgprogression.requirements.title").append(" ");boolean first=true;
                for(var entry:missing.entrySet()){if(!first)message.append(", ");first=false;message.append(Component.translatable(entry.getKey().key())).append(" "+entry.getValue());}
            }
            cir.setReturnValue(new CastResult(CastResult.Type.FAILURE,message));
        }
    }
    @Inject(method="attemptInitiateCast",at=@At("HEAD"),cancellable=true)
    private void rpg$item(ItemStack stack,int spellLevel,Level level,Player player,CastSource source,boolean cooldown,String slot,CallbackInfoReturnable<Boolean> cir){
        if(!ItemRequirements.allowed(player,IronsRequirements.castingItem(player,stack,slot))){
            player.displayClientMessage(Component.translatable("rpgprogression.requirements.blocked"),true);cir.setReturnValue(false);
        }
    }
    /** Revalidate before payment/effects, including channeling after attributes or equipment change. */
    @Inject(method="castSpell",at=@At("HEAD"),cancellable=true)
    private void rpg$finish(Level world,int level,ServerPlayer player,CastSource source,boolean cooldown,CallbackInfo ci){
        if(!IronsRequirements.spellAllowed(player,(AbstractSpell)(Object)this,level,source)
                ||!ItemRequirements.allowed(player,IronsRequirements.castingItem(player,MagicData.getPlayerMagicData(player).getPlayerCastingItem(),MagicData.getPlayerMagicData(player).getCastingEquipmentSlot())))ci.cancel();
    }
}

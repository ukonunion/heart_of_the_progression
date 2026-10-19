package dev.rpgprogression.mixin.irons;
import dev.rpgprogression.compat.*;
import io.redspace.ironsspellbooks.effect.InstantManaEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(value=InstantManaEffect.class,remap=false)
public abstract class ManaPotionMixin {
    @Inject(method="getDescriptionLine",at=@At("HEAD"),cancellable=true)
    private void rpg$description(MobEffectInstance effect,CallbackInfoReturnable<Component> cir){
        if(IronsCompat.enabled())cir.setReturnValue(Component.translatable("tooltip.irons_spellbooks.instant_mana_description",effect.getAmplifier()+1,(effect.getAmplifier()+1)*5).withStyle(net.minecraft.ChatFormatting.BLUE));
    }
    @Inject(method="applyInstantenousEffect",remap=true,at=@At("HEAD"),cancellable=true)
    private void rpg$potion(Entity direct,Entity source,LivingEntity target,int amplifier,double strength,CallbackInfo ci){
        if(target instanceof ServerPlayer p&&IronsCompat.shares(p)){IronsBridge.potion(p,amplifier,strength);ci.cancel();}
    }
}

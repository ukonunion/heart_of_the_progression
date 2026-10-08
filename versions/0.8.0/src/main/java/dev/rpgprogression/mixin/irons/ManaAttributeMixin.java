package dev.rpgprogression.mixin.irons;
import dev.rpgprogression.compat.IronsCompat;
import dev.rpgprogression.progression.ProgressionManager;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native percentage calculations see the same capacity; equipped modifiers are ignored. */
@Mixin(LivingEntity.class)
public abstract class ManaAttributeMixin {
    @Inject(method="getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D",at=@At("HEAD"),cancellable=true)
    private void rpg$capacity(Attribute attribute,CallbackInfoReturnable<Double> cir){
        if((Object)this instanceof ServerPlayer p&&IronsCompat.shares(p)&&attribute==AttributeRegistry.MAX_MANA.get())
            cir.setReturnValue(ProgressionManager.get(p).baseMana());
    }
}

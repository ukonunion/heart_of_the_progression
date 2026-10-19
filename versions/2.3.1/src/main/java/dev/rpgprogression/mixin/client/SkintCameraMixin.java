package dev.rpgprogression.mixin.client;

import dev.rpgprogression.client.SkintCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class SkintCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch,float roll);
    @Shadow private boolean detached;
    @Inject(method="setup",at=@At("TAIL"))
    private void rpg$skintView(BlockGetter level,Entity entity,boolean thirdPerson,boolean mirrored,float partial,CallbackInfo ci){
        var view=SkintCamera.view();if(view==null)return;
        detached=true;setPosition(view.position());setRotation(view.yaw(),view.pitch(),0);
    }
}

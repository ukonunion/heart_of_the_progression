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
    private boolean rpg$isMainCamera(){return (Object)this==net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();}
    @Inject(method="setup",at=@At("HEAD"))
    private void rpg$beginReturnTarget(BlockGetter level,Entity entity,boolean thirdPerson,boolean mirrored,float partial,CallbackInfo ci){
        if(rpg$isMainCamera())SkintCamera.beforeSetup();
    }
    // Keep the character's third-person rendering during the return while vanilla computes the saved view.
    @ModifyVariable(method="setup",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private boolean rpg$returnThirdPerson(boolean original){
        var type=rpg$isMainCamera()?SkintCamera.returnPerspective():null;return type==null?original:!type.isFirstPerson();
    }
    @ModifyVariable(method="setup",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private boolean rpg$returnMirrored(boolean original){
        var type=rpg$isMainCamera()?SkintCamera.returnPerspective():null;return type==null?original:type.isMirrored();
    }
    @Inject(method="setup",at=@At("TAIL"))
    private void rpg$skintView(BlockGetter level,Entity entity,boolean thirdPerson,boolean mirrored,float partial,CallbackInfo ci){
        if(!rpg$isMainCamera())return;
        var view=SkintCamera.view((Camera)(Object)this);SkintCamera.afterSetup();if(view==null)return;
        // Hide the local model while the camera is still inside it on a first-person entry.
        detached=!entity.getBoundingBox().inflate(.1).contains(view.position());
        setPosition(view.position());setRotation(view.yaw(),view.pitch(),view.roll());
    }
}

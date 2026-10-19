package dev.rpgprogression.mixin.client;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.rpgprogression.client.SkintPlayerAnimation;
import dev.rpgprogression.client.SkintCamera;
import dev.rpgprogression.skint.SkintSeat;
import net.minecraft.client.model.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class SkintPlayerRendererMixin<T extends LivingEntity,M extends EntityModel<T>> {
    @Shadow protected M model;
    @Unique private List<SkintPlayerAnimation.Part> rpgprogression$skintParts;
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"),cancellable=true)
    private void rpgprogression$nearCamera(T entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffer,int light,CallbackInfo ci){
        if(SkintCamera.hidePlayer(entity,partial))ci.cancel();
    }
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",shift=At.Shift.AFTER,ordinal=0))
    private void rpgprogression$ground(T player,float yaw,float partial,PoseStack stack,MultiBufferSource buffer,int light,CallbackInfo ci){
        if(SkintPlayerAnimation.active(player)){
            var seat=(SkintSeat)player.getVehicle();
            // Passenger attachment is below the standing feet; the authored model starts at ground level.
            stack.translate(0,seat.getY()-Mth.lerp(partial,player.yo,player.getY()),0);
        }
    }
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;"))
    private void rpgprogression$pose(T entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffer,int light,CallbackInfo ci){
        if(model instanceof PlayerModel<?> playerModel&&SkintPlayerAnimation.vanilla(entity))rpgprogression$skintParts=SkintPlayerAnimation.apply(playerModel,(AbstractClientPlayer)entity,partial);
    }
    @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("RETURN"))
    private void rpgprogression$restore(T entity,float yaw,float partial,PoseStack stack,MultiBufferSource buffer,int light,CallbackInfo ci){
        if(rpgprogression$skintParts!=null){SkintPlayerAnimation.restore(rpgprogression$skintParts);rpgprogression$skintParts=null;}
    }
}

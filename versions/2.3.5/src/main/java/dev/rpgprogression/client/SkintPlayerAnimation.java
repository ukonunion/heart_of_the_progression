package dev.rpgprogression.client;

import java.util.*;
import dev.rpgprogression.animation.SkintAnimation;
import dev.rpgprogression.skint.SkintSeat;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.player.AbstractClientPlayer;

/** Apply the user's six-part pose to Minecraft's own Steve/Alex model, skin and clothing layers. */
public final class SkintPlayerAnimation {
    public record Part(ModelPart part,PartPose pose){}
    public static boolean active(net.minecraft.world.entity.Entity player){return player instanceof AbstractClientPlayer&&player.getVehicle() instanceof SkintSeat;}
    public static List<Part> apply(PlayerModel<?> model,AbstractClientPlayer player,float partial){
        var seat=(SkintSeat)player.getVehicle();var pose=seat.animationPose(partial);
        var parts=List.of(model.body,model.head,model.rightArm,model.leftArm,model.rightLeg,model.leftLeg,model.hat,model.jacket,model.rightSleeve,model.leftSleeve,model.rightPants,model.leftPants);
        var backup=parts.stream().map(p->new Part(p,p.storePose())).toList();
        apply(model.body,pose.bone("torso"));apply(model.head,pose.bone("head"));
        apply(model.rightArm,pose.bone("right_arm"));apply(model.leftArm,pose.bone("left_arm"));
        apply(model.rightLeg,pose.bone("right_leg"));apply(model.leftLeg,pose.bone("left_leg"));
        model.hat.copyFrom(model.head);model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);model.leftPants.copyFrom(model.leftLeg);
        return backup;
    }
    private static void apply(ModelPart part,SkintAnimation.Transform value){
        part.loadPose(part.getInitialPose());part.x-=value.x();part.y-=value.y();part.z+=value.z();
        part.xRot=(float)Math.toRadians(-value.rx());part.yRot=(float)Math.toRadians(-value.ry());part.zRot=(float)Math.toRadians(value.rz());
    }
    public static void restore(List<Part> parts){for(var part:parts)part.part.loadPose(part.pose);}
    private SkintPlayerAnimation(){}
}

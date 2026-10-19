package dev.rpgprogression.animation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkintAnimationTest {
    private void same(SkintAnimation.Pose a,SkintAnimation.Pose b){
        for(int i=0;i<SkintAnimation.BONES.size();i++)assertArrayEquals(a.bones().get(i).values(),b.bones().get(i).values(),1e-9);
    }
    @Test void sourceKeyframesAndHoldArePreserved(){
        assertEquals(1.4,SkintAnimation.SIT.length());assertEquals(1.4,SkintAnimation.STAND.length());
        var finalArm=SkintAnimation.SIT.sample(1).bone("right_arm");
        assertEquals(-13,finalArm.y());assertEquals(-20.2503,finalArm.rx());
        assertEquals(-69.7802,SkintAnimation.SIT.sample(.33333).bone("head").ry(),1e-9);
        same(SkintAnimation.SIT.sample(1),SkintAnimation.SIT.sample(20));
    }
    @Test void authoredSitAndStandJoinExactly(){
        same(SkintAnimation.SIT.sample(1.4),SkintAnimation.STAND.sample(0));
        same(SkintAnimation.STAND.sample(1.4),SkintAnimation.SIT.sample(0));
    }
    @Test void interpolationActuallyMovesBetweenAuthoredFrames(){
        var first=SkintAnimation.SIT.sample(0).bone("torso");var middle=SkintAnimation.SIT.sample(.5).bone("torso");var last=SkintAnimation.SIT.sample(1).bone("torso");
        assertNotEquals(first.y(),middle.y());assertNotEquals(last.y(),middle.y());assertTrue(Double.isFinite(middle.rx()));
        double a=SkintAnimation.SIT.sample(.26667-1e-7).bone("torso").rx(),b=SkintAnimation.SIT.sample(.26667+1e-7).bone("torso").rx();assertEquals(a,b,.001);
    }
    @Test void earlyExitAndReopeningStartFromTheCurrentPose(){
        var halfway=SkintAnimation.SIT.sample(.3);same(halfway,SkintAnimation.play(true,0,halfway));
        var gettingUp=SkintAnimation.play(true,.2,halfway);same(gettingUp,SkintAnimation.play(false,0,gettingUp));
        same(SkintAnimation.STAND.sample(1.4),SkintAnimation.play(true,1.4,halfway));
        same(SkintAnimation.SIT.sample(1.4),SkintAnimation.play(false,1.4,gettingUp));
    }
}

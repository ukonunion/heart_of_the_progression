package dev.rpgprogression.client.hud;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudVisibilityAnimationTest {
    @Test void appearanceAndDisappearanceInterpolateAndSettle(){
        var motion=new HudVisibilityAnimation();assertFalse(motion.active());
        motion.tick(true);assertEquals(0,motion.value(0));assertTrue(motion.value(.5f)>0&&motion.value(.5f)<motion.value(1));
        for(int i=0;i<8;i++)motion.tick(true);assertEquals(1,motion.value(0));assertEquals(1,motion.value(1));
        motion.tick(false);assertEquals(1,motion.value(0));assertTrue(motion.value(1)>0&&motion.value(1)<1);
        for(int i=0;i<8;i++)motion.tick(false);assertFalse(motion.active());assertEquals(0,motion.value(.5f));
    }
    @Test void reversingAnUnfinishedFadeDoesNotJumpOrLeaveStaleVisibility(){
        var motion=new HudVisibilityAnimation();for(int i=0;i<3;i++)motion.tick(true);float before=motion.value(1);
        motion.tick(false);assertEquals(before,motion.value(0));assertTrue(motion.value(1)<before);
        before=motion.value(1);motion.tick(true);assertEquals(before,motion.value(0));assertTrue(motion.value(1)>before);
        for(int i=0;i<40;i++){motion.tick(i%2==0);assertTrue(motion.value(.4f)>=0&&motion.value(.4f)<=1);}
        motion.reset();assertFalse(motion.active());assertEquals(0,motion.value(1));
    }
}

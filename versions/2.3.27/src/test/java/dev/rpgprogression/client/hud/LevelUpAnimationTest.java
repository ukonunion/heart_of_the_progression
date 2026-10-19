package dev.rpgprogression.client.hud;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LevelUpAnimationTest {
    @Test void idleNeverAppearsAndOneGainFadesInHoldsAndFadesOut(){
        var banner=new LevelUpAnimation();
        for(int i=0;i<100;i++)banner.tick();
        assertFalse(banner.active());assertEquals(0,banner.opacity(.5f));
        banner.show();banner.tick();
        assertEquals(0,banner.opacity(0));assertTrue(banner.opacity(.5f)>0);
        for(int i=1;i<8;i++)banner.tick();
        assertEquals(1,banner.opacity(1));
        for(int i=8;i<LevelUpAnimation.DISPLAY_TICKS;i++)banner.tick();
        assertEquals(1,banner.opacity(1));
        banner.tick();assertEquals(1,banner.opacity(0));assertTrue(banner.opacity(1)<1);
        for(int i=0;i<8;i++)banner.tick();
        assertFalse(banner.active());assertEquals(0,banner.opacity(1));
    }
    @Test void anotherGainReversesFadeSmoothlyAndLogoutClearsIt(){
        var banner=new LevelUpAnimation();banner.show();
        for(int i=0;i<LevelUpAnimation.DISPLAY_TICKS+4;i++)banner.tick();
        float faded=banner.opacity(1);assertTrue(faded>0&&faded<1);
        banner.show();assertEquals(faded,banner.opacity(1));
        banner.tick();assertEquals(faded,banner.opacity(0));assertTrue(banner.opacity(1)>faded);
        banner.reset();assertFalse(banner.active());assertEquals(0,banner.opacity(1));
        banner.tick();assertEquals(0,banner.opacity(.5f));
    }
}

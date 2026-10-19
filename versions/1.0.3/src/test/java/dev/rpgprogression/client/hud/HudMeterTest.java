package dev.rpgprogression.client.hud;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudMeterTest {
    @Test void pixelPairsStayCenteredAtEveryValue(){
        assertEquals(0,CenteredFill.halfWidth(74,0));assertEquals(37,CenteredFill.halfWidth(74,1));
        assertEquals(19,CenteredFill.halfWidth(74,.5));
        int previous=0;
        for(int step=0;step<=100;step++){
            int half=CenteredFill.halfWidth(74,step/100.0);assertTrue(half>=previous&&half<=37);previous=half;
        }
        assertEquals(0,CenteredFill.halfWidth(74,-1));assertEquals(37,CenteredFill.halfWidth(74,2));
        assertEquals(0,CenteredFill.halfWidth(74,Double.NaN));
        assertEquals(0,CenteredFill.pixels(73,0));assertEquals(73,CenteredFill.pixels(73,1));
        assertEquals(37,CenteredFill.pixels(73,.5));assertEquals(0,CenteredFill.pixels(73,Double.NaN));
        assertEquals(0,CenteredFill.pixels(73,-1));assertEquals(73,CenteredFill.pixels(73,2));
        for(int step=1;step<=100;step++){
            int pixels=CenteredFill.pixels(73,step/100.0);
            assertEquals(1,pixels%2);assertTrue(pixels<=73);
        }
    }
    @Test void effectSkinIsExclusiveAndHardcoreAware(){
        assertSame(HealthStyle.NORMAL,HealthStyle.select(false,false,false,false));
        assertSame(HealthStyle.ABSORPTION,HealthStyle.select(false,false,false,true));
        assertSame(HealthStyle.FROZEN,HealthStyle.select(false,false,true,true));
        assertSame(HealthStyle.POISON,HealthStyle.select(false,true,true,true));
        assertSame(HealthStyle.WITHER,HealthStyle.select(true,true,true,true));
        assertEquals("hp/survival/empty",HealthStyle.NORMAL.texture(false));
        assertEquals("hp/hardcore/main_hc",HealthStyle.NORMAL.texture(true));
        for(HealthStyle style:HealthStyle.values())assertTrue(style.texture(true).endsWith("_hc"));
    }
}

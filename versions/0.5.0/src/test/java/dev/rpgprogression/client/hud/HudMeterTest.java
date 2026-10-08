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

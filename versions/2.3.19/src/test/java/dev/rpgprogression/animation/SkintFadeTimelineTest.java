package dev.rpgprogression.animation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class SkintFadeTimelineTest {
    private static final long START=9_000_000_000L,D=SkintFadeTimeline.DURATION_NANOS;
    @Test void transferCanOnlyStartAfterCompleteDarkness(){
        var f=new SkintFadeTimeline();assertFalse(f.active());f.darken(START);
        assertEquals(0,f.opacity(START));assertFalse(f.black(START+D-1));
        assertTrue(f.black(START+D));assertEquals(1,f.opacity(START+D));
    }
    @Test void darkScreenSurvivesArbitrarilySlowWorldLoading(){
        var f=new SkintFadeTimeline();f.darken(START);f.hold();
        f.advance(START+120_000_000_000L);assertTrue(f.active());assertEquals(1,f.opacity(START+120_000_000_000L));
    }
    @Test void bothDirectionsAreSmoothAndMonotonic(){
        var f=new SkintFadeTimeline();f.darken(START);double prior=0;
        for(int i=0;i<=100;i++){double a=f.opacity(START+D*i/100);assertTrue(a>=prior&&a>=0&&a<=1);prior=a;}
        assertEquals(.5,f.opacity(START+D/2),1e-12);f.hold();f.reveal(START+D);prior=1;
        for(int i=0;i<=100;i++){double a=f.opacity(START+D+D*i/100);assertTrue(a<=prior&&a>=0&&a<=1);prior=a;}
        f.advance(START+2*D);assertFalse(f.active());assertEquals(0,f.opacity(START+2*D));
    }
    @Test void cancelledTransferRevealsContinuouslyFromCurrentOpacity(){
        var f=new SkintFadeTimeline();f.darken(START);long cancel=START+D/3;double a=f.opacity(cancel);
        f.reveal(cancel);assertEquals(a,f.opacity(cancel));f.advance(cancel+D);assertFalse(f.active());
    }
    @Test void logoutCannotLeaveBlackOverlayForNextWorld(){
        var f=new SkintFadeTimeline();f.darken(START);f.hold();f.clear();assertFalse(f.active());assertEquals(0,f.opacity(START+3*D));
        f.darken(START+4*D);assertEquals(0,f.opacity(START+4*D));
    }
}

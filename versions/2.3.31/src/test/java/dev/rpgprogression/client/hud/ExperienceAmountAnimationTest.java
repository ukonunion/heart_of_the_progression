package dev.rpgprogression.client.hud;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceAmountAnimationTest {
    @Test void firstSnapshotRespawnAndResetAreImmediateBaselines(){
        var animation=new ExperienceAmountAnimation();var player=new Object();animation.update(player,5000);assertEquals(5000,animation.value(0));
        animation.update(player,6000);animation.update(new Object(),7000);assertEquals(7000,animation.value(.5f));
        animation.reset();assertEquals(0,animation.value(1));animation.update(player,9000);assertEquals(9000,animation.value(0));
        animation.update(null,10000);assertEquals(0,animation.value(1));
    }
    @Test void gainAdvancesAcrossTicksAndFramesThenReachesTheExactTarget(){
        var animation=new ExperienceAmountAnimation();var player=new Object();animation.update(player,0);animation.update(player,2477);
        assertEquals(0,animation.value(0));assertTrue(animation.value(.5f)>0&&animation.value(.5f)<2477);
        long previous=0;
        for(int i=0;i<ExperienceAmountAnimation.DURATION_TICKS;i++){
            for(float partial:new float[]{0,.25f,.5f,.75f,1}){long current=animation.value(partial);assertTrue(current>=previous&&current<=2477);previous=current;}
            animation.update(player,2477);
        }
        assertEquals(2477,animation.value(0));assertEquals(2477,animation.value(1));
    }
    @Test void anotherPickupContinuesFromTheCurrentCountInsteadOfRestartingAtZero(){
        var animation=new ExperienceAmountAnimation();var player=new Object();animation.update(player,0);animation.update(player,2477);
        for(int i=0;i<3;i++)animation.update(player,2477);long current=animation.value(1);assertTrue(current>0&&current<2477);
        animation.update(player,4954);assertEquals(current,animation.value(0));assertTrue(animation.value(.5f)>current);
        for(int i=0;i<12;i++)animation.update(player,4954);assertEquals(4954,animation.value(1));
    }
    @Test void decreasesAndLevelRolloverDoNotOvershootOrGoNegative(){
        var animation=new ExperienceAmountAnimation();var player=new Object();animation.update(player,10000);animation.update(player,0);long previous=10000;
        for(int i=0;i<12;i++){long current=animation.value(1);assertTrue(current<=previous&&current>=0);previous=current;animation.update(player,0);}
        assertEquals(0,animation.value(1));
    }
    @Test void adjacentLargeLongValuesSettleExactlyAndStayInsideTheirBounds(){
        var animation=new ExperienceAmountAnimation();var player=new Object();long maximum=9_000_000_000_000_000L;
        animation.update(player,maximum-3);animation.update(player,maximum);
        for(int i=0;i<12;i++){assertTrue(animation.value(.5f)>=maximum-3&&animation.value(.5f)<=maximum);animation.update(player,maximum);}
        assertEquals(maximum,animation.value(1));
        animation.update(player,Long.MAX_VALUE);for(int i=0;i<12;i++){assertTrue(animation.value(1)>=maximum);animation.update(player,Long.MAX_VALUE);}assertEquals(Long.MAX_VALUE,animation.value(1));
    }
}

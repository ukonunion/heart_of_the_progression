package dev.rpgprogression.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionMathTest {
    @Test void legacyExponentialCurveAndOverflow() {
        double rate=ProgressionMath.DEFAULT_MULTIPLIER;
        assertEquals(810,ProgressionMath.required(1,810,rate));
        assertEquals(819,ProgressionMath.required(2,810,rate));
        assertEquals(500,ProgressionMath.required(1,500,rate));
        assertEquals(506,ProgressionMath.required(2,500,rate));
        assertEquals(511,ProgressionMath.required(3,500,rate));
        assertEquals(517,ProgressionMath.required(4,500,rate));
        assertEquals(523,ProgressionMath.required(5,500,rate));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.required(Integer.MAX_VALUE,500,rate));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.add(ProgressionMath.XP_LIMIT,ProgressionMath.XP_LIMIT));
    }
    @Test void legacyCurveUsesUnroundedBaseAtEveryLevel() {
        double rate=ProgressionMath.DEFAULT_MULTIPLIER;
        // At level 10 repeated rounding would give 109; evaluating the formula gives 110.
        assertEquals(110,ProgressionMath.required(10,100,rate));
        assertEquals(299,ProgressionMath.required(100,100,rate));
        assertEquals(100,ProgressionMath.required(1,100.49,rate));
        assertEquals(101,ProgressionMath.required(1,100.5,rate));
    }
    @Test void legacyCumulativeCostsRetainExperienceAcrossLevels() {
        double rate=ProgressionMath.DEFAULT_MULTIPLIER;long total=0;
        for(int level=1;level<100;level++)total+=ProgressionMath.required(level,500,rate);
        assertEquals(89370,total);
        assertEquals(483,2000-ProgressionMath.required(1,500,rate)-ProgressionMath.required(2,500,rate)-ProgressionMath.required(3,500,rate));
    }
    @Test void stagedCurveMatchesTheThreePhases() {
        int[] levels={1,2,3,4,5,6,10,20,30,40,49,50,51,60,75,99,100,300};
        long[] costs={500,550,600,650,700,752,1000,1900,3200,4900,6772,7000,7239,10200,18375,39879,41000,627000};
        for(int i=0;i<levels.length;i++)assertEquals(costs[i],ProgressionMath.required(levels[i],500),"level "+levels[i]);
        long total=0;for(int l=1;l<100;l++)total+=ProgressionMath.required(l,500);
        assertEquals(1137615,total);
        assertEquals(350,2000-ProgressionMath.required(1,500)-ProgressionMath.required(2,500)-ProgressionMath.required(3,500));
    }
    @Test void stagedCostsRiseContinuouslyAcrossMilestones() {
        long previous=0,previousStep=0;
        for(int l=1;l<=300;l++){
            long cost=ProgressionMath.required(l,500),step=cost-previous;
            assertTrue(cost>previous,"strict growth at "+l);
            if(l>2)assertTrue(step>=previousStep,"growth slows at "+l);
            previous=cost;previousStep=step;
        }
        assertEquals(50,ProgressionMath.required(5,500)-ProgressionMath.required(4,500));
        assertEquals(52,ProgressionMath.required(6,500)-ProgressionMath.required(5,500));
        assertEquals(228,ProgressionMath.required(50,500)-ProgressionMath.required(49,500));
        assertEquals(239,ProgressionMath.required(51,500)-ProgressionMath.required(50,500));
    }
    @Test void stagedCurveScalesAndRoundsEachThresholdIndependently() {
        assertEquals(4100,ProgressionMath.required(100,50));
        assertEquals(82000,ProgressionMath.required(100,1000));
        assertEquals(150,ProgressionMath.required(6,100));
        assertEquals(100,ProgressionMath.required(1,100.49));
        assertEquals(101,ProgressionMath.required(1,100.5));
        assertEquals(500,ProgressionMath.required(-5,500));
        assertEquals(ProgressionMath.XP_LIMIT,ProgressionMath.required(Integer.MAX_VALUE,500));
        assertEquals(1,ProgressionMath.required(1,1));
    }
    @Test void curveSelectionKeepsLegacyCustomRatesSeparate() {
        assertEquals(41000,ProgressionMath.required(100,500,10,ProgressionMath.Curve.STAGED));
        assertEquals(1493,ProgressionMath.required(100,500,ProgressionMath.DEFAULT_MULTIPLIER,ProgressionMath.Curve.EXPONENTIAL));
        assertEquals(500,ProgressionMath.required(100,500,1,ProgressionMath.Curve.EXPONENTIAL));
    }
    @Test void staminaScaling() {
        assertEquals(100,ProgressionMath.stamina(9,9,100,10));
        assertEquals(170,ProgressionMath.stamina(16,9,100,10));
        assertEquals(100,ProgressionMath.stamina(1,9,100,10));
    }
    @Test void bowDiminishingReturns() {
        assertEquals(1,ProgressionMath.drawRatio(10,10,.025,.25));
        assertEquals(.8,ProgressionMath.drawRatio(20,10,.025,.25),1e-9);
        assertEquals(2.0/3,ProgressionMath.drawRatio(30,10,.025,.25),1e-9);
        assertEquals(.25,ProgressionMath.drawRatio(10000,10,.025,.25));
    }
    @Test void enduranceDiscountStartsAboveBaselineAndStopsAtThirtyPercent() {
        assertEquals(12,ProgressionMath.staminaCost(12,0,10),1e-9);
        assertEquals(12,ProgressionMath.staminaCost(12,10,10),1e-9);
        assertEquals(11.88,ProgressionMath.staminaCost(12,11,10),1e-9);
        assertEquals(10.8,ProgressionMath.staminaCost(12,20,10),1e-9);
        assertEquals(8.52,ProgressionMath.staminaCost(12,39,10),1e-9);
        assertEquals(8.4,ProgressionMath.staminaCost(12,40,10),1e-9);
        assertEquals(8.4,ProgressionMath.staminaCost(12,10000,10),1e-9);
        assertEquals(11.88,ProgressionMath.staminaCost(12,21,20),1e-9);
    }
    @Test void fractionalActionsKeepTheirPrecisionAndFreeActionsStayFree() {
        assertEquals(.14,ProgressionMath.staminaCost(.2,40,10),1e-9);
        assertEquals(5.6,ProgressionMath.staminaCost(8,40,10),1e-9);
        assertEquals(7,ProgressionMath.staminaCost(10,40,10),1e-9);
        assertEquals(9.8,ProgressionMath.staminaCost(14,40,10),1e-9);
        assertEquals(0,ProgressionMath.staminaCost(0,40,10));
    }
    @Test void allInventoryThresholds() {
        int[] thresholds=new int[27];for(int i=0;i<27;i++)thresholds[i]=10+i;
        for(int endurance=0;endurance<=100;endurance++)
            assertEquals(Math.min(27,Math.max(0,endurance-9)),ProgressionMath.unlocked(endurance,thresholds));
    }
}

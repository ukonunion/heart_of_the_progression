package dev.rpgprogression.progression;

/** Chance per complete group of five Agility levels above level 10. */
public final class ClavisMath {
    public static double lifeSaveChance(int agility,double perFiveLevels) {
        if(!Double.isFinite(perFiveLevels)||perFiveLevels<=0)return 0;
        return Math.min(1,Math.max(0L,(long)agility-10)/5*perFiveLevels);
    }
    private ClavisMath(){}
}

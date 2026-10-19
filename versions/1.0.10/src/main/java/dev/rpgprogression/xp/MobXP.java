package dev.rpgprogression.xp;

import dev.rpgprogression.progression.ProgressionMath;

/** Pure per-kill variation; use the previous rounded reward as the center. */
public final class MobXP {
    public static long reward(long base,double roll){
        double factor=.75+.5*Math.max(0,Math.min(1,roll));
        return Math.max(0,Math.min(ProgressionMath.XP_LIMIT,Math.round(Math.max(0,Math.min(ProgressionMath.XP_LIMIT,base))*factor)));
    }
    private MobXP(){}
}

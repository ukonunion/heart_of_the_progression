package dev.rpgprogression.progression;

/** Pure, bounded arithmetic shared by gameplay and tests. */
public final class ProgressionMath {
    public static final long XP_LIMIT = 9_000_000_000_000_000L;
    public static long required(int level, double base, double multiplier) {
        return Math.max(1, Math.min(XP_LIMIT, Math.round(base * Math.pow(multiplier, Math.max(0,level-1)))));
    }
    public static long add(long a, long b) { return Math.min(XP_LIMIT, Math.max(0,a) + Math.min(XP_LIMIT,Math.max(0,b))); }
    public static double drawRatio(int agility,int baseline,double factor,double minimum) {
        return Math.max(minimum,1.0/(1.0+Math.max(0,agility-baseline)*factor));
    }
    public static double stamina(int endurance,int baseline,double base,double perPoint) {
        return Math.max(1,base+Math.max(0,endurance-baseline)*perPoint);
    }
    public static int unlocked(int endurance,int[] thresholds) {
        int n=0; for (int t: thresholds) { if (endurance<t) break; n++; } return Math.min(27,n);
    }
    private ProgressionMath() {}
}

package dev.rpgprogression.progression;

/** Pure, bounded arithmetic shared by gameplay and tests. */
public final class ProgressionMath {
    public static final long XP_LIMIT = 9_000_000_000_000_000L;
    public static final double DEFAULT_MULTIPLIER = 91.0/90.0;
    public enum Curve { STAGED, EXPONENTIAL, CUSTOM }
    /** Cost of the next level. The three polynomial sections meet with matching slopes. */
    public static long required(int level, double base) {
        double l=Math.max(1,level),factor;
        if(l<=5)factor=1+.1*(l-1);
        else if(l<=50){double t=l-5;factor=1.4+.1*t+.004*t*t;}
        else{double t=l-50;factor=14+.46*t+.018*t*t;}
        return bounded(base*factor);
    }
    public static long required(int level,double base,double multiplier,Curve curve) {
        if(curve==Curve.CUSTOM)return required(level,base,multiplier,curve,LevelXPFormula.DEFAULT);
        return curve==Curve.EXPONENTIAL?required(level,base,multiplier):required(level,base);
    }
    public static long required(int level,double base,double multiplier,Curve curve,String formula){
        if(curve!=Curve.CUSTOM)return required(level,base,multiplier,curve);
        double value=LevelXPFormula.evaluate(formula,level,base,multiplier);
        return Double.isNaN(value)?required(level,base):bounded(value);
    }
    /** Legacy configurable exponential curve, retained for servers with custom rates. */
    public static long required(int level, double base, double multiplier) {
        return bounded(base * Math.pow(multiplier, Math.max(0,level-1)));
    }
    private static long bounded(double value){return Math.max(1,Math.min(XP_LIMIT,Math.round(value)));}
    public static long add(long a, long b) { return Math.min(XP_LIMIT, Math.max(0,a) + Math.min(XP_LIMIT,Math.max(0,b))); }
    public static double drawRatio(int agility,int baseline,double factor,double minimum) {
        return Math.max(minimum,1.0/(1.0+Math.max(0,agility-baseline)*factor));
    }
    public static double stamina(int endurance,int baseline,double base,double perPoint) {
        return Math.max(1,base+Math.max(0,endurance-baseline)*perPoint);
    }
    public static double staminaCost(double baseCost,int endurance,int baseline) {
        double reduction=Math.min(.30,Math.max(0L,(long)endurance-baseline)*.01);
        return Math.max(0,baseCost)*(1-reduction);
    }
    public static int unlocked(int endurance,int[] thresholds) {
        int n=0; for (int t: thresholds) { if (endurance<t) break; n++; } return Math.min(27,n);
    }
    private ProgressionMath() {}
}

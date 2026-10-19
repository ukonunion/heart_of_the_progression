package dev.rpgprogression.resource;

public final class ResourceMath {
    public static final int MAX_MANA=8, INTELLIGENCE_PER_MANA=5, NATIVE_MANA_PER_UNIT=25;
    public static int manaCapacity(int intelligence,int start){return (int)Math.min(MAX_MANA,Math.max(0L,(long)intelligence-start)/INTELLIGENCE_PER_MANA);}
    public static int spellCost(int nativeCost){return nativeCost<=0?0:(int)Math.min(MAX_MANA,((long)nativeCost+NATIVE_MANA_PER_UNIT-1)/NATIVE_MANA_PER_UNIT);}
    public static double sparkCapacity(int potential,int start,double base,double perLevel){return base+Math.max(0,potential-start)*perLevel;}
    public static double sparkRecoveryTicks(int potential,int start,double baseSeconds,double speedPerLevel){return Math.max(1,baseSeconds*20/(1+Math.max(0,potential-start)*speedPerLevel));}
    public static boolean sparkDepleted(double value,double maximum){return maximum>0&&value<=maximum*0.1;}
    public static double recover(double value,double maximum,double amount){return Math.min(maximum,Math.max(0,value)+Math.max(0,amount));}
    private ResourceMath(){}
}

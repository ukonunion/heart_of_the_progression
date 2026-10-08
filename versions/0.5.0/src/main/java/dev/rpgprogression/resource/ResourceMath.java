package dev.rpgprogression.resource;

public final class ResourceMath {
    public static double sparkCapacity(int potential,int start,double base,double perLevel){return base+Math.max(0,potential-start)*perLevel;}
    public static double sparkRecoveryTicks(int potential,int start,double baseSeconds,double speedPerLevel){return Math.max(1,baseSeconds*20/(1+Math.max(0,potential-start)*speedPerLevel));}
    public static double recover(double value,double maximum,double amount){return Math.min(maximum,Math.max(0,value)+Math.max(0,amount));}
    private ResourceMath(){}
}

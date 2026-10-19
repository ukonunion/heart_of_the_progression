package dev.rpgprogression.client.hud;

/** Compact current RPG experience without rounding up across a suffix boundary. */
public final class ProgressionExperienceFormat {
    public static String format(long amount,boolean russian){
        long value=Math.max(0,amount);
        if(value<10_000)return Long.toString(value);
        long divisor=value>=1_000_000?1_000_000:1_000;
        long whole=value/divisor,tenth=(value%divisor)/(divisor/10);
        return Long.toString(whole)+(tenth==0?"":(russian?",":".")+tenth)+" "+(divisor==1_000?(russian?"Т":"K"):(russian?"М":"M"));
    }
    private ProgressionExperienceFormat(){}
}

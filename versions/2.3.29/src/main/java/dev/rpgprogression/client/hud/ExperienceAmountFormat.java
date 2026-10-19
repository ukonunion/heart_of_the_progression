package dev.rpgprogression.client.hud;

import java.util.function.ToIntFunction;

/** Keep full integers while they fit; abbreviate overflow using the active font's real width. */
public final class ExperienceAmountFormat {
    private static final long[] UNITS={1_000L,1_000_000L,1_000_000_000L,1_000_000_000_000L,1_000_000_000_000_000L};
    private static final String[] EN={"K","M","B","T","Q"},RU={"Т","М","Млрд","Трлн","Квдр"};
    public static String format(long amount,boolean russian,int availableWidth,ToIntFunction<String> width){
        long value=Math.max(0,amount);String full=Long.toString(value);
        if(width.applyAsInt(full)<=availableWidth||value<UNITS[0])return full;
        int unit=0;while(unit+1<UNITS.length&&value>=UNITS[unit+1])unit++;
        long divisor=UNITS[unit],whole=value/divisor,tenth=(value%divisor)/(divisor/10);
        String suffix=" "+(russian?RU:EN)[unit];
        String compact=Long.toString(whole)+(tenth==0?"":(russian?",":".")+tenth)+suffix;
        return width.applyAsInt(compact)<=availableWidth?compact:Long.toString(whole)+suffix;
    }
    private ExperienceAmountFormat(){}
}

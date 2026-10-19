package dev.rpgprogression.xp;

import dev.rpgprogression.progression.ProgressionMath;
import java.util.*;

public final class ProgressionOrbMath {
    public static final int MAX_ORBS=12;
    public static final double ATTRACTION_RANGE=16;
    public static List<Long> split(long amount){
        long remaining=Math.max(0,Math.min(ProgressionMath.XP_LIMIT,amount));var result=new ArrayList<Long>();
        int[] sizes={2477,1237,617,307,149,73,37,17,7,3,1};
        while(remaining>0){
            long value=remaining;
            if(result.size()<MAX_ORBS-1)for(int size:sizes)if(remaining>=size){value=size;break;}
            result.add(value);remaining-=value;
        }
        return List.copyOf(result);
    }
    public static double attraction(double distance){double fraction=Math.max(0,1-Math.max(0,distance)/ATTRACTION_RANGE);return .018*fraction*fraction;}
    private ProgressionOrbMath(){}
}

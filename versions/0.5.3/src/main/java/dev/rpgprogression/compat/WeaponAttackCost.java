package dev.rpgprogression.compat;

import java.util.List;
import java.util.Locale;

public final class WeaponAttackCost {
    public static boolean valid(String rule){
        String[] parts=rule.split("=",-1);
        if(parts.length!=2||!parts[0].matches("[a-z0-9_:.\\-]+"))return false;
        try{double n=Double.parseDouble(parts[1]);return Double.isFinite(n)&&n>=0&&n<=10000;}
        catch(NumberFormatException error){return false;}
    }
    public static double get(String category,List<? extends String> rules,double fallback){
        String key=category.toLowerCase(Locale.ROOT);
        for(String rule:rules)if(valid(rule)&&rule.substring(0,rule.indexOf('=')).equals(key))return Double.parseDouble(rule.substring(rule.indexOf('=')+1));
        return fallback;
    }
    private WeaponAttackCost(){}
}

package dev.rpgprogression.requirements;

/** Converted mana units; zero mana or a disabled multiplier keeps the spell unrestricted. */
public final class SpellRequirementMath {
    public static int intelligence(int mana,int base,int perUnit){
        if(mana<=0||perUnit<=0)return 0;
        return (int)Math.min(10000,Math.max(0L,base)+(long)Math.min(8,mana)*perUnit);
    }
    private SpellRequirementMath(){}
}

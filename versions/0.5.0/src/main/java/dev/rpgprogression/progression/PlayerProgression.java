package dev.rpgprogression.progression;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.nbt.CompoundTag;
import java.util.Arrays;

public final class PlayerProgression {
    public int level=1, points=0;
    public long xp=0;
    private final int[] attributes={9,9,9,9,9,1};
    public double stamina=100, sprintDistance=0;
    public boolean exhausted=false, initialized=false;
    public double spark=25, mana=0, manaCapacity=100;
    public boolean manaInitialized=false, manaActive=false;
    public int manaRegenCooldown=0;
    public double lastSentSpark=-1,lastSentMana=-1,lastSentManaCapacity=-1;
    public boolean lastSentManaActive=false;
    // Runtime-only fields: timestamps are not persisted across a new server tick timeline.
    public int regenCooldown=0, attackCooldown=0, syncTicks=0, runWindowTicks=0, runWindowXP=0;
    public double lastX, lastY, lastZ, lastSentStamina=-1;
    public boolean sampledPosition=false, dirty=true;
    public int lastSpendTick=-100, lastRequestTick=-100;
    public double drawRemainder=0;
    public int get(RPGAttribute a) { return attributes[a.ordinal()]; }
    public void set(RPGAttribute a,int n) { attributes[a.ordinal()]=Math.max(0,Math.min(10000,n)); dirty=true; }
    public void initialize() {
        if (initialized) return;
        Arrays.fill(attributes,Math.min(RPGConfig.START.get(),RPGConfig.MAX_ATTRIBUTE.get()));
        attributes[RPGAttribute.SPARK_POTENTIAL.ordinal()]=Math.min(RPGConfig.SPARK_START.get(),RPGConfig.MAX_ATTRIBUTE.get());
        spark=maxSpark();
        stamina=maxStamina(); initialized=true; dirty=true;
    }
    public double maxStamina() { return ProgressionMath.stamina(get(RPGAttribute.ENDURANCE),RPGConfig.START.get(),RPGConfig.BASE_STAMINA.get(),RPGConfig.ENDURANCE_STAMINA.get()); }
    public long requiredXP() { return ProgressionMath.required(level,RPGConfig.BASE_XP.get(),RPGConfig.XP_MULTIPLIER.get()); }
    public double maxSpark(){return dev.rpgprogression.resource.ResourceMath.sparkCapacity(get(RPGAttribute.SPARK_POTENTIAL),RPGConfig.SPARK_START.get(),RPGConfig.SPARK_BASE.get(),RPGConfig.SPARK_PER_LEVEL.get());}
    public double sparkRecoveryTicks(){return dev.rpgprogression.resource.ResourceMath.sparkRecoveryTicks(get(RPGAttribute.SPARK_POTENTIAL),RPGConfig.SPARK_START.get(),RPGConfig.SPARK_RECOVERY_SECONDS.get(),RPGConfig.SPARK_RECOVERY_SPEED.get());}
    public double baseMana(){return RPGConfig.MANA_BASE.get()+Math.max(0,get(RPGAttribute.INTELLIGENCE)-RPGConfig.START.get())*RPGConfig.MANA_PER_INTELLIGENCE.get();}
    public CompoundTag save() {
        CompoundTag n=new CompoundTag(); n.putInt("version",2); n.putBoolean("initialized",initialized);
        n.putInt("level",level); n.putLong("xp",xp); n.putInt("points",points);
        for (RPGAttribute a:RPGAttribute.values()) n.putInt(a.id(),get(a));
        n.putDouble("stamina",stamina); n.putBoolean("exhausted",exhausted); n.putDouble("sprintDistance",sprintDistance);
        n.putDouble("spark",spark);n.putDouble("mana",mana);n.putDouble("manaCapacity",manaCapacity);
        n.putBoolean("manaInitialized",manaInitialized);n.putBoolean("manaActive",manaActive);
        return n;
    }
    public void load(CompoundTag n) {
        initialized=n.getBoolean("initialized"); level=Math.max(1,Math.min(1000,n.getInt("level")));
        xp=Math.max(0,Math.min(ProgressionMath.XP_LIMIT,n.getLong("xp"))); points=Math.max(0,n.getInt("points"));
        for(RPGAttribute a:RPGAttribute.values()) if(n.contains(a.id())) set(a,n.getInt(a.id()));
        if(!n.contains(RPGAttribute.SPARK_POTENTIAL.id()))set(RPGAttribute.SPARK_POTENTIAL,1);
        spark=n.contains("spark")?finite(n.getDouble("spark")):25;
        mana=finite(n.getDouble("mana"));manaCapacity=n.contains("manaCapacity")?finite(n.getDouble("manaCapacity")):100;
        manaInitialized=n.getBoolean("manaInitialized");manaActive=n.getBoolean("manaActive");
        stamina=finite(n.getDouble("stamina")); sprintDistance=finite(n.getDouble("sprintDistance"));
        exhausted=n.getBoolean("exhausted"); dirty=true;
    }
    private static double finite(double n) { return Double.isFinite(n)?Math.max(0,n):0; }
}

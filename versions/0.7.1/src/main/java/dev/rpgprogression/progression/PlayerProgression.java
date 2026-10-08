package dev.rpgprogression.progression;

import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import net.minecraft.nbt.CompoundTag;
import java.util.Arrays;

public final class PlayerProgression {
    // Kept outside NBT: a copied/respawned capability belongs to its new player.
    private final net.minecraft.world.entity.player.Player owner;
    public PlayerProgression(){this(null);}
    public PlayerProgression(net.minecraft.world.entity.player.Player owner){this.owner=owner;}
    public int level=1, points=0;
    public long xp=0;
    private final int[] attributes={10,10,10,10,10,10};
    private int attributeBaseline=10,sparkBaseline=10;
    public int skillPoints=0,skillMilestones=0;
    public final java.util.Set<String> unlockedSkills=new java.util.HashSet<>();
    public boolean parcoolSkillsImported=false,epicSkillsImported=false,legacySkills=false;
    public int lastSkillTick=-100,lastTreeRequestTick=-100;
    public double stamina=100, sprintDistance=0;
    public boolean exhausted=false, initialized=false;
    public double spark=25, mana=0, manaCapacity=100;
    public boolean sparkDepletionNotified=false;
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
        if (initialized) {migrateStartingValues();return;}
        Arrays.fill(attributes,Math.min(RPGConfig.START.get(),RPGConfig.MAX_ATTRIBUTE.get()));
        attributes[RPGAttribute.SPARK_POTENTIAL.ordinal()]=Math.min(RPGConfig.SPARK_START.get(),RPGConfig.MAX_ATTRIBUTE.get());
        attributeBaseline=RPGConfig.START.get();sparkBaseline=RPGConfig.SPARK_START.get();
        spark=maxSpark();
        sparkDepletionNotified=false;
        stamina=maxStamina(); initialized=true; dirty=true;
    }
    private void migrateStartingValues(){
        // Keep every purchased increment when upgrading the former 9/1 defaults to 10/10.
        if(attributeBaseline==9&&RPGConfig.START.get()==10){
            for(var a:RPGAttribute.values())if(a!=RPGAttribute.SPARK_POTENTIAL)shiftStartingValue(a,1);
            attributeBaseline=10;
        }
        if(sparkBaseline==1&&RPGConfig.SPARK_START.get()==10){shiftStartingValue(RPGAttribute.SPARK_POTENTIAL,9);sparkBaseline=10;}
    }
    private void shiftStartingValue(RPGAttribute attribute,int delta){
        int old=get(attribute),maximum=RPGConfig.MAX_ATTRIBUTE.get(),next=old+delta;
        if(old<=maximum&&next>maximum)points=(int)Math.min(Integer.MAX_VALUE,(long)points+next-maximum);
        set(attribute,Math.min(maximum,next));
    }
    public void awardSkillPoints(){
        int reached=level/RPGConfig.SKILL_LEVEL_INTERVAL.get();
        if(reached>skillMilestones){skillPoints=(int)Math.min(Integer.MAX_VALUE,(long)skillPoints+reached-skillMilestones);skillMilestones=reached;dirty=true;}
    }
    public double maxStamina() {
        double base=ProgressionMath.stamina(get(RPGAttribute.ENDURANCE),RPGConfig.START.get(),RPGConfig.BASE_STAMINA.get(),RPGConfig.ENDURANCE_STAMINA.get());
        return dev.rpgprogression.compat.EpicFightCompat.maximum(owner,base);
    }
    public long requiredXP() { return ProgressionMath.required(level,RPGConfig.BASE_XP.get(),RPGConfig.XP_MULTIPLIER.get()); }
    public double maxSpark(){return dev.rpgprogression.resource.ResourceMath.sparkCapacity(get(RPGAttribute.SPARK_POTENTIAL),RPGConfig.SPARK_START.get(),RPGConfig.SPARK_BASE.get(),RPGConfig.SPARK_PER_LEVEL.get());}
    public double sparkRecoveryTicks(){return dev.rpgprogression.resource.ResourceMath.sparkRecoveryTicks(get(RPGAttribute.SPARK_POTENTIAL),RPGConfig.SPARK_START.get(),RPGConfig.SPARK_RECOVERY_SECONDS.get(),RPGConfig.SPARK_RECOVERY_SPEED.get());}
    public double baseMana(){return RPGConfig.MANA_BASE.get()+Math.max(0,get(RPGAttribute.INTELLIGENCE)-RPGConfig.START.get())*RPGConfig.MANA_PER_INTELLIGENCE.get();}
    public CompoundTag save() {
        CompoundTag n=new CompoundTag(); n.putInt("version",4); n.putBoolean("initialized",initialized);
        n.putInt("attributeBaseline",attributeBaseline);n.putInt("sparkBaseline",sparkBaseline);
        n.putInt("skillPoints",skillPoints);n.putInt("skillMilestones",skillMilestones);
        var skills=new net.minecraft.nbt.ListTag();unlockedSkills.stream().sorted().forEach(s->skills.add(net.minecraft.nbt.StringTag.valueOf(s)));n.put("unlockedSkills",skills);
        n.putBoolean("parcoolSkillsImported",parcoolSkillsImported);n.putBoolean("epicSkillsImported",epicSkillsImported);n.putBoolean("legacySkills",legacySkills);
        n.putInt("level",level); n.putLong("xp",xp); n.putInt("points",points);
        for (RPGAttribute a:RPGAttribute.values()) n.putInt(a.id(),get(a));
        n.putDouble("stamina",stamina); n.putBoolean("exhausted",exhausted); n.putDouble("sprintDistance",sprintDistance);
        n.putDouble("spark",spark);n.putDouble("mana",mana);n.putDouble("manaCapacity",manaCapacity);
        n.putBoolean("sparkDepletionNotified",sparkDepletionNotified);
        n.putBoolean("manaInitialized",manaInitialized);n.putBoolean("manaActive",manaActive);
        return n;
    }
    public void load(CompoundTag n) {
        initialized=n.getBoolean("initialized"); level=Math.max(1,Math.min(1000,n.getInt("level")));
        attributeBaseline=n.contains("attributeBaseline")?n.getInt("attributeBaseline"):(n.getInt("version")>=2?9:10);
        sparkBaseline=n.contains("sparkBaseline")?n.getInt("sparkBaseline"):1;
        skillPoints=Math.max(0,n.getInt("skillPoints"));skillMilestones=Math.max(0,n.getInt("skillMilestones"));
        unlockedSkills.clear();for(var tag:n.getList("unlockedSkills",8)){
            String id=tag.getAsString();if(id.length()<=256&&net.minecraft.resources.ResourceLocation.tryParse(id)!=null&&unlockedSkills.size()<4096)unlockedSkills.add(id);
        }
        parcoolSkillsImported=n.getBoolean("parcoolSkillsImported");epicSkillsImported=n.getBoolean("epicSkillsImported");
        legacySkills=n.getBoolean("legacySkills")||(initialized&&n.getInt("version")<4);
        xp=Math.max(0,Math.min(ProgressionMath.XP_LIMIT,n.getLong("xp"))); points=Math.max(0,n.getInt("points"));
        for(RPGAttribute a:RPGAttribute.values()) if(n.contains(a.id())) set(a,n.getInt(a.id()));
        if(!n.contains(RPGAttribute.SPARK_POTENTIAL.id()))set(RPGAttribute.SPARK_POTENTIAL,sparkBaseline);
        spark=n.contains("spark")?finite(n.getDouble("spark")):25;
        sparkDepletionNotified=n.getBoolean("sparkDepletionNotified");
        mana=finite(n.getDouble("mana"));manaCapacity=n.contains("manaCapacity")?finite(n.getDouble("manaCapacity")):100;
        manaInitialized=n.getBoolean("manaInitialized");manaActive=n.getBoolean("manaActive");
        stamina=finite(n.getDouble("stamina")); sprintDistance=finite(n.getDouble("sprintDistance"));
        exhausted=n.getBoolean("exhausted"); dirty=true;
    }
    private static double finite(double n) { return Double.isFinite(n)?Math.max(0,n):0; }
}

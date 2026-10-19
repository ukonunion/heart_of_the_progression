package dev.rpgprogression.skill;

import dev.rpgprogression.progression.PlayerProgression;
import java.util.*;

/** One RPG purchase grants the installed providers' three native dodge actions. */
public final class DodgeSkillGroup {
    public static final String ID="rpgprogression:dodge";
    public static final List<String> MEMBERS=List.of("parcool:dodge","epicfight:roll","epicfight:step");
    public static List<String> members(boolean parcool,boolean epic){
        return MEMBERS.stream().filter(id->id.startsWith("parcool:")?parcool:epic).toList();
    }
    /** Existing purchases retain their value, even if only one provider was installed before. */
    public static boolean migrate(PlayerProgression d,boolean parcool,boolean epic){
        boolean changed=migrate(d.unlockedSkills,parcool,epic);
        if(changed)d.dirty=true;
        return changed;
    }
    public static boolean migrate(Set<String> unlocked,boolean parcool,boolean epic){
        var available=members(parcool,epic);if(available.isEmpty())return false;
        if(!unlocked.contains(ID)&&MEMBERS.stream().noneMatch(unlocked::contains))return false;
        boolean changed=unlocked.add(ID);
        for(var member:available)changed|=unlocked.add(member);
        return changed;
    }
    private DodgeSkillGroup(){}
}

package dev.rpgprogression.skill;

import java.util.*;

/** Learning routes from the supplied 2.2.0 diagram; independent of optional mod classes. */
public final class DesignedSkillTree {
    public static final Set<String> ROOTS = Set.of(
            "parcool:crawl", "parcool:fast_run", "parcool:charge_jump", "parcool:hang_on", "parcool:skydive",
            "epicfight:endurance", "epicfight:impact_guard", "epicfight:revelation", "epicfight:swordmaster");
    private static final Map<String,List<String>> PARENTS = new LinkedHashMap<>();
    static {
        route("parcool:hide_in_block", "parcool:crawl");
        route("parcool:slide", "parcool:crawl", "parcool:fast_run");
        route("parcool:dive", "parcool:crawl", "parcool:fast_run");
        route("parcool:fast_swim", "parcool:fast_run", "parcool:charge_jump");
        route("parcool:trick_jump", "parcool:charge_jump");
        route("parcool:vault", "parcool:charge_jump");
        route("parcool:wall_jump", "parcool:charge_jump");
        route("parcool:horizontal_wall_run", "parcool:fast_run");
        route("parcool:wall_run", "parcool:horizontal_wall_run", "parcool:wall_jump");
        route("parcool:hang_down", "parcool:hang_on");
        route("parcool:ride_zipline", "parcool:horizontal_wall_run", "parcool:hang_down");
        route("parcool:pole_climb", "parcool:hang_on");
        route("parcool:slide_down", "parcool:hang_on");
        route("parcool:climb_up", "parcool:hang_on");
        route("parcool:breakfall", "parcool:skydive");
        route("parcool:long_jump", "parcool:fast_run");
        route(DodgeSkillGroup.ID, "parcool:dive");
        route("parcool:dodge", DodgeSkillGroup.ID);
        route("epicfight:roll", DodgeSkillGroup.ID);
        route("epicfight:step", DodgeSkillGroup.ID);
        route("epicfight:emergency_escape", DodgeSkillGroup.ID);
        route("epicfight:technician", DodgeSkillGroup.ID);
        route("epicfight:phantom_ascent", "parcool:long_jump");
        route("epicfight:demolition_leap", "parcool:long_jump");
        route("epicfight:meteor_slam", "epicfight:demolition_leap");
        route("epicfight:adaptive_skin", "epicfight:impact_guard");
        route("epicfight:vengeance", "epicfight:impact_guard");
        route("epicfight:guard", "epicfight:revelation");
        route("epicfight:parrying", "epicfight:revelation");
        route("epicfight:stamina_pillager", "epicfight:endurance");
        route("epicfight:hypervitality", "epicfight:stamina_pillager");
        route("epicfight:adrenaline_fiend", "epicfight:hypervitality");
        route("epicfight:forbidden_strength", "epicfight:stamina_pillager");
        route("epicfight:death_harvest", "epicfight:forbidden_strength");
        route("epicfight:catharsis", "epicfight:stamina_pillager");
        route("epicfight:bonebreaker", "epicfight:swordmaster");
        route("epicfight:berserker", "epicfight:swordmaster");
    }
    private static void route(String id,String... parents){PARENTS.put(id,List.of(parents));}
    public static List<String> parents(String id,List<String> nativeParents,Set<String> installed){
        if(ROOTS.contains(id))return List.of();
        var designed=PARENTS.get(id);
        if(designed==null)return nativeParents.stream().map(parent->DodgeSkillGroup.MEMBERS.contains(parent)?DodgeSkillGroup.ID:parent).filter(installed::contains).distinct().toList();
        var available=designed.stream().filter(installed::contains).toList();
        if(!available.isEmpty())return available;
        // Keep Epic's movement branch usable when ParCool is absent.
        if(id.equals(DodgeSkillGroup.ID)&&installed.contains("epicfight:endurance"))return List.of("epicfight:endurance");
        if((id.equals("epicfight:phantom_ascent")||id.equals("epicfight:demolition_leap"))&&installed.contains(DodgeSkillGroup.ID))return List.of(DodgeSkillGroup.ID);
        return nativeParents.stream().filter(installed::contains).toList();
    }
    private DesignedSkillTree(){}
}

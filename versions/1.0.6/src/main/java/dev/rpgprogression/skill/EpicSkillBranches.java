package dev.rpgprogression.skill;

import java.util.*;

/** Supplemental branches only apply when a skill has no native learnable prerequisite. */
public final class EpicSkillBranches {
    private static final Map<String, String> PARENTS = Map.ofEntries(
        edge("step", "roll"), edge("emergency_escape", "step"), edge("technician", "step"),
        edge("phantom_ascent", "roll"), edge("demolition_leap", "phantom_ascent"), edge("meteor_slam", "demolition_leap"),
        edge("parrying", "guard"), edge("impact_guard", "guard"), edge("adaptive_skin", "guard"), edge("vengeance", "adaptive_skin"),
        edge("hypervitality", "endurance"), edge("stamina_pillager", "endurance"),
        edge("adrenaline_fiend", "stamina_pillager"), edge("death_harvest", "stamina_pillager"), edge("catharsis", "death_harvest"),
        edge("bonebreaker", "swordmaster"), edge("berserker", "bonebreaker"), edge("forbidden_strength", "berserker"),
        edge("revelation", "swordmaster")
    );
    private static Map.Entry<String, String> edge(String skill, String parent) { return Map.entry("epicfight:" + skill, "epicfight:" + parent); }
    public static List<String> parents(String id, List<String> nativeParents, Set<String> registered) {
        if (!nativeParents.isEmpty()) return List.copyOf(nativeParents);
        String parent = PARENTS.get(id);
        return parent != null && registered.contains(parent) ? List.of(parent) : List.of();
    }
    private EpicSkillBranches() {}
}

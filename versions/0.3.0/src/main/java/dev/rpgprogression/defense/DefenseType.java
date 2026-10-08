package dev.rpgprogression.defense;
public enum DefenseType {
    PHYSICAL, ELEMENTAL, FIRE, POISON, EXHAUSTION;
    public String key() { return "rpgprogression.defense."+name().toLowerCase(java.util.Locale.ROOT); }
}

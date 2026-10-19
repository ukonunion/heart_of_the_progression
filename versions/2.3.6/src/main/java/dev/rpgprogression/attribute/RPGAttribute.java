package dev.rpgprogression.attribute;

import java.util.Locale;

public enum RPGAttribute {
    STRENGTH, ENDURANCE, AGILITY, VITALITY, INTELLIGENCE, SPARK_POTENTIAL;
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String key() { return "rpgprogression.attribute." + id(); }
}

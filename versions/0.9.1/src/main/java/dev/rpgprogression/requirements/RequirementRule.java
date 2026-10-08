package dev.rpgprogression.requirements;

/** Configuration grammar is independent of registries; missing optional mods are harmless. */
public record RequirementRule(String id,boolean tag,int minimum) {
    public static RequirementRule parse(String text){
        String[] parts=text.trim().split("=",-1);
        if(parts.length!=2)throw new IllegalArgumentException("Expected item:id=level or #tag:id=level");
        String id=parts[0].trim();boolean tag=id.startsWith("#");if(tag)id=id.substring(1);
        if(!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw new IllegalArgumentException("Invalid item/tag identifier");
        int level=Integer.parseInt(parts[1].trim());
        if(level<0||level>10000)throw new IllegalArgumentException("Requirement outside 0..10000");
        return new RequirementRule(id,tag,level);
    }
    public static boolean valid(String value){try{parse(value);return true;}catch(IllegalArgumentException e){return false;}}
}

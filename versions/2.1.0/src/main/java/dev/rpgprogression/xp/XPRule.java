package dev.rpgprogression.xp;

public record XPRule(String id,boolean tag,double amount) {
    public static XPRule parse(String text){
        String[] parts=text.trim().split("=",-1);if(parts.length!=2)throw new IllegalArgumentException("Expected namespace:id=XP");
        String id=parts[0].trim();boolean tag=id.startsWith("#");if(tag)id=id.substring(1);
        if(!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))throw new IllegalArgumentException("Invalid ID");
        double amount=Double.parseDouble(parts[1].trim());
        if(!Double.isFinite(amount)||amount<0||amount>1e9)throw new IllegalArgumentException("XP outside 0..1e9");
        return new XPRule(id,tag,amount);
    }
    public static boolean valid(String text){try{parse(text);return true;}catch(IllegalArgumentException e){return false;}}
}

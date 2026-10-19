package dev.rpgprogression.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Optional Relics detection without linking the mod or assigning requirements to its materials. */
public final class RelicsRequirements {
    private static boolean relicType(Class<?> type){
        if(type==null)return false;
        if(type.getName().equals("it.hurts.sskirillss.relics.items.relics.base.IRelicItem"))return true;
        for(var implemented:type.getInterfaces())if(relicType(implemented))return true;
        return relicType(type.getSuperclass());
    }
    private static final ClassValue<Boolean> ARTIFACT=new ClassValue<>(){
        @Override protected Boolean computeValue(Class<?> type){return relicType(type);}
    };
    public static boolean artifact(ItemStack stack){return !stack.isEmpty()&&BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("relics")&&ARTIFACT.get(stack.getItem().getClass());}
    private RelicsRequirements(){}
}

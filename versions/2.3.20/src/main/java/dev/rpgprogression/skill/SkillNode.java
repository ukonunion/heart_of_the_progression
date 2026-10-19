package dev.rpgprogression.skill;

import net.minecraft.nbt.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import java.util.List;

/** A common, serializable description; optional mod types never cross the network boundary. */
public record SkillNode(String id,int branch,Component name,String category,List<String> parents){
    public CompoundTag save(HolderLookup.Provider registries){
        var n=new CompoundTag();n.putString("id",id);n.putInt("branch",branch);n.putString("name",Component.Serializer.toJson(name,registries));n.putString("category",category);
        var list=new ListTag();parents.forEach(s->list.add(StringTag.valueOf(s)));n.put("parents",list);return n;
    }
    public static SkillNode load(CompoundTag n,HolderLookup.Provider registries){
        var parents=new java.util.ArrayList<String>();for(var tag:n.getList("parents",8))parents.add(tag.getAsString());
        Component name=Component.Serializer.fromJson(n.getString("name"),registries);
        return new SkillNode(n.getString("id"),n.getInt("branch"),name==null?Component.literal(n.getString("id")):name,n.getString("category"),List.copyOf(parents));
    }
}

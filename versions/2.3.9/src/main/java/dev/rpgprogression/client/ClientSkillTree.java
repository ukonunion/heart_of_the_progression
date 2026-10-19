package dev.rpgprogression.client;
import dev.rpgprogression.skill.SkillNode;
import net.minecraft.nbt.*;
import java.util.*;
public final class ClientSkillTree {
    public static List<SkillNode> nodes=List.of();
    public static List<CompoundTag> slots=List.of();
    public static boolean ready=false,parcool=false,epic=false,irons=false;
    public static int revision=0;
    public static void load(CompoundTag data){
        var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
        if(data==null||connection==null)return;var list=new ArrayList<SkillNode>();for(var tag:data.getList("nodes",10))list.add(SkillNode.load((CompoundTag)tag,connection.registryAccess()));nodes=List.copyOf(list);
        var containers=new ArrayList<CompoundTag>();for(var tag:data.getList("slots",10))containers.add(((CompoundTag)tag).copy());slots=List.copyOf(containers);
        parcool=data.getBoolean("parcool");epic=data.getBoolean("epicfight");irons=data.getBoolean("irons");ready=true;revision++;
    }
    public static void reset(){nodes=List.of();slots=List.of();ready=false;parcool=false;epic=false;irons=false;revision++;}
    private ClientSkillTree(){}
}

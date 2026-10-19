package dev.rpgprogression.skill;

import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** All prices and prerequisites are resolved on the server, never supplied by a client. */
public final class SkillTreeManager {
    // Loot tables are loaded before per-world SERVER config. Presence is safe at that stage too.
    public static boolean parcool(){return ParCoolCompat.present();}
    public static boolean epic(){return EpicFightCompat.present();}
    public static List<SkillNode> catalog(ServerPlayer p){
        var nodes=new ArrayList<SkillNode>();
        if(parcool())nodes.addAll(ParCoolSkills.catalog(p));
        if(epic())nodes.addAll(EpicFightSkills.catalog());
        return List.copyOf(nodes);
    }
    public static void refresh(ServerPlayer p,PlayerProgression d){
        d.awardSkillPoints();
        if(parcool())ParCoolSkills.refresh(p,d);
        if(epic())EpicFightSkills.refresh(p,d);
    }
    public static boolean canUnlock(PlayerProgression d,SkillNode node){
        return d.skillPoints>0&&!d.unlockedSkills.contains(node.id())&&d.unlockedSkills.containsAll(node.parents());
    }
    public static boolean unlock(ServerPlayer p,String id){
        if(p==null||p.isRemoved()||!p.isAlive()||p.isSpectator()||id==null||id.length()>256)return false;
        var d=p.getCapability(PlayerProgressionProvider.CAPABILITY).orElse(null);if(d==null)return false;
        d.initialize();refresh(p,d);
        var node=catalog(p).stream().filter(n->n.id().equals(id)).findFirst().orElse(null);
        if(node==null||!canUnlock(d,node))return false;
        if(node.branch()==1&&!ParCoolSkills.canUnlock(p,id))return false;
        if(node.branch()==2&&!EpicFightSkills.canUnlock(p,id))return false;
        d.skillPoints--;d.unlockedSkills.add(id);d.dirty=true;
        if(node.branch()==1)ParCoolSkills.apply(p,d);
        if(node.branch()==2)EpicFightSkills.learn(p,id);
        ModNetworking.sync(p);sync(p);return true;
    }
    public static void equip(ServerPlayer p,String id,int slot){
        if(p==null||p.isRemoved()||!p.isAlive()||p.isSpectator()||!epic())return;
        var d=p.getCapability(PlayerProgressionProvider.CAPABILITY).orElse(null);if(d==null)return;
        if(d.unlockedSkills.contains(id))EpicFightSkills.equip(p,id,slot);
        sync(p);
    }
    public static void sync(ServerPlayer p){
        if(p==null||p.isRemoved()||!p.isAlive())return;
        var d=p.getCapability(PlayerProgressionProvider.CAPABILITY).orElse(null);if(d==null)return;
        refresh(p,d);ModNetworking.sync(p);
        var n=new CompoundTag();var list=new ListTag();catalog(p).forEach(node->list.add(node.save()));n.put("nodes",list);
        n.putBoolean("parcool",parcool());n.putBoolean("epicfight",epic());n.putBoolean("irons",IronsCompat.present());
        if(epic())n.put("slots",EpicFightSkills.slots(p));
        ModNetworking.treeSnapshot(p,n);
    }
    private SkillTreeManager(){}
}

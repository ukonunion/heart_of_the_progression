package dev.rpgprogression.compat;

import com.alrex.parcool.ParCool;
import com.alrex.parcool.api.action.ActionEntry;
import com.alrex.parcool.common.Parkourability;
import com.alrex.parcool.common.network.ActionCapabilitiesPacket;
import com.alrex.parcool.common.resource.skilltree.SkilltreeResourceManager;
import com.alrex.parcool.common.skilltree.SkillTree;
import dev.rpgprogression.mixin.parcool.SkillEntryAccessor;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Native ParCool actions and datapack prerequisites, paid for with RPG points. */
public final class ParCoolSkills {
    private static List<SkillTree> trees(ServerPlayer p){
        var manager=SkilltreeResourceManager.getInstance();
        return manager==null?Parkourability.get(p).getSkillTrees():manager.getSkillTrees();
    }
    private static void visit(SkillTree.Entry<?> entry,Map<String,Set<String>> parents){
        String id=entry.getActionEntry().id().toString();var dependencies=parents.computeIfAbsent(id,s->new LinkedHashSet<>());
        if(entry.getParent()!=null)dependencies.add(entry.getParent().getActionEntry().id().toString());
        for(var action:((SkillEntryAccessor)(Object)entry).rpg$dependencies())dependencies.add(action.id().toString());
        for(var child:entry.getChildren())visit(child,parents);
    }
    public static List<SkillNode> catalog(ServerPlayer p){
        var parents=new LinkedHashMap<String,Set<String>>();for(var tree:trees(p))visit(tree.getRoot(),parents);
        var actions=ParCool.getActionRegistry().getRegisteredActions();
        // Learnable addon actions outside the native JSON trees remain accessible as roots.
        actions.values().stream().filter(a->a.option().needLearning()).sorted().forEach(a->parents.computeIfAbsent(a.id().toString(),s->new LinkedHashSet<>()));
        var nodes=new ArrayList<SkillNode>();
        for(var pair:parents.entrySet()){
            var action=actions.get(net.minecraft.resources.ResourceLocation.parse(pair.getKey()));if(action==null||!action.option().needLearning())continue;
            var prerequisites=pair.getValue().stream().filter(s->{var a=actions.get(net.minecraft.resources.ResourceLocation.parse(s));return a!=null&&a.option().needLearning();}).toList();
            nodes.add(new SkillNode(pair.getKey(),1,Component.translatable(action.getTranslationKey()),"parkour",prerequisites));
        }
        return nodes;
    }
    public static void refresh(ServerPlayer p,PlayerProgression d){
        var capabilities=Parkourability.get(p).getCapabilities();
        if(!d.parcoolSkillsImported){
            if(d.legacySkills)for(var node:catalog(p)){
                var action=ParCool.getActionRegistry().get(net.minecraft.resources.ResourceLocation.parse(node.id()));
                if(capabilities.can(action))d.unlockedSkills.add(node.id());
            }
            d.parcoolSkillsImported=true;d.dirty=true;
        }
        boolean changed=false;
        for(var action:ParCool.getActionRegistry().getRegisteredActions().values())if(action.option().needLearning()){
            boolean unlocked=d.unlockedSkills.contains(action.id().toString());
            if(capabilities.can(action)!=unlocked){capabilities.set(action,unlocked);changed=true;}
        }
        if(changed)capabilities.sync(p,ActionCapabilitiesPacket.Target.CAPABILITY);
    }
    public static void apply(ServerPlayer p,PlayerProgression d){refresh(p,d);}
    public static boolean canUnlock(ServerPlayer p,String id){
        var action=ParCool.getActionRegistry().get(net.minecraft.resources.ResourceLocation.parse(id));return action!=null&&action.option().needLearning();
    }
    private ParCoolSkills(){}
}

package dev.rpgprogression.compat;

import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.*;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import java.util.*;

/** Discover all registered book skills, including addons, instead of maintaining a partial list. */
public final class EpicFightSkills {
    private static ServerPlayerPatch patch(ServerPlayer p){return EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);}
    public static List<SkillNode> catalog(){
        return SkillManager.getSkills(s->s.getCategory().learnable()).stream()
                .sorted(Comparator.comparing((Skill s)->s.getCategory().toString()).thenComparing(s->s.getRegistryName().toString()))
                .map(s->{var prior=s.getPriorSkill();return new SkillNode(s.getRegistryName().toString(),2,s.getDisplayName(),s.getCategory().toString().toLowerCase(Locale.ROOT),
                        prior!=null&&prior.getCategory().learnable()?List.of(prior.getRegistryName().toString()):List.of());}).toList();
    }
    public static void refresh(ServerPlayer p,PlayerProgression d){
        var patch=patch(p);if(patch==null)return;
        var cap=patch.getSkillCapability();
        if(!d.epicSkillsImported){
            cap.listAcquiredSkills().filter(s->s.getCategory().learnable()).forEach(s->d.unlockedSkills.add(s.getRegistryName().toString()));
            d.epicSkillsImported=true;d.dirty=true;
        }
        var restored=new ArrayList<String>();
        for(var node:catalog())if(d.unlockedSkills.contains(node.id())){
            var skill=SkillManager.getSkill(node.id());if(!cap.hasLearned(skill)){cap.addLearnedSkill(skill);restored.add(node.id());}
        }
        if(!restored.isEmpty())EpicFightNetworkManager.sendToPlayer(new SPAddLearnedSkill(restored.toArray(String[]::new)),p);
    }
    public static boolean canUnlock(ServerPlayer p,String id){
        var skill=SkillManager.getSkill(id);return patch(p)!=null&&skill!=null&&skill.getCategory().learnable();
    }
    public static void learn(ServerPlayer p,String id){
        var patch=patch(p);var skill=SkillManager.getSkill(id);if(patch==null||skill==null)return;
        var cap=patch.getSkillCapability();cap.addLearnedSkill(skill);
        EpicFightNetworkManager.sendToPlayer(new SPAddLearnedSkill(id),p);
        var empty=cap.getFirstEmptyContainer(skill.getCategory());if(empty!=null)equip(p,id,empty.getSlotId());
    }
    public static boolean equip(ServerPlayer p,String id,int slot){
        var patch=patch(p);if(patch==null)return false;var cap=patch.getSkillCapability();var skill=SkillManager.getSkill(id);
        if(skill==null||!skill.getCategory().learnable()||!cap.hasLearned(skill)||slot<0||slot>=cap.skillContainers.length)return false;
        var container=cap.getSkillContainerFor(slot);
        if(container==null||container.getSlot().category()!=skill.getCategory()||container.onReplaceCooldown())return false;
        if(!container.setSkill(skill))return false;
        int cooldown=yesman.epicfight.world.gamerule.EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(p.level());
        container.setReplaceCooldown(cooldown);
        EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.replaceCooldown(container.getSlot(),cooldown,p.getId()),p);
        EpicFightNetworkManager.sendToPlayer(container.createSyncPacketToLocalPlayer(),p);
        EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(container.createSyncPacketToRemotePlayer(),p);
        return true;
    }
    public static ListTag slots(ServerPlayer p){
        var list=new ListTag();var patch=patch(p);if(patch==null)return list;
        patch.getSkillCapability().listSkillContainers().filter(c->c.getSlot().category().learnable()).forEach(c->{
            var n=new CompoundTag();n.putInt("slot",c.getSlotId());n.putString("category",c.getSlot().category().toString().toLowerCase(Locale.ROOT));
            n.putString("name",c.getSlot().toString().toLowerCase(Locale.ROOT));n.putString("skill",c.isEmpty()?"":c.getSkill().getRegistryName().toString());n.putInt("cooldown",c.getReplaceCooldown());list.add(n);
        });return list;
    }
    public static boolean nativeChangeAllowed(ServerPlayerPatch patch,SkillSlot slot,int bookSlot,Skill skill){
        if(bookSlot>=0||slot==null||!slot.category().learnable()||patch.getOriginal().isRemoved()||!patch.getOriginal().isAlive())return false;
        if(skill==null)return true;
        var d=patch.getOriginal().getCapability(PlayerProgressionProvider.CAPABILITY).orElse(null);
        return d!=null&&skill.getCategory()==slot.category()&&d.unlockedSkills.contains(skill.getRegistryName().toString())&&patch.getSkillCapability().hasLearned(skill);
    }
    public static boolean book(ItemStack stack){return stack.getItem() instanceof yesman.epicfight.world.item.SkillBookItem;}
    private EpicFightSkills(){}
}

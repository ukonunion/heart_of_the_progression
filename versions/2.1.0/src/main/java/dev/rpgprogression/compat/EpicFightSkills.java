package dev.rpgprogression.compat;

import dev.rpgprogression.progression.*;
import dev.rpgprogression.skill.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.registry.EpicFightRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.*;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import java.util.*;

/** Discover all registered book skills, including addons, instead of maintaining a partial list. */
public final class EpicFightSkills {
    private static ServerPlayerPatch patch(ServerPlayer p){return EpicFightCapabilities.getEntityPatch(p,ServerPlayerPatch.class);}
    private static Skill skill(String id){var key=ResourceLocation.tryParse(id);return key==null?null:EpicFightRegistries.SKILL.get(key);}
    public static List<SkillNode> catalog(){
        var skills=EpicFightRegistries.SKILL.stream().filter(s->s.getCategory().learnable()).toList();
        var registered=skills.stream().map(s->s.getRegistryName().toString()).collect(java.util.stream.Collectors.toSet());
        return skills.stream()
                .sorted(Comparator.comparing((Skill s)->s.getCategory().toString()).thenComparing(s->s.getRegistryName().toString()))
                .map(s->{var prior=s.getPriorSkill();var nativeParents=prior!=null&&prior.getCategory().learnable()?List.of(prior.getRegistryName().toString()):List.<String>of();
                    return new SkillNode(s.getRegistryName().toString(),2,s.getDisplayName(),s.getCategory().toString().toLowerCase(Locale.ROOT),
                            EpicSkillBranches.parents(s.getRegistryName().toString(),nativeParents,registered));}).toList();
    }
    public static void refresh(ServerPlayer p,PlayerProgression d){
        var patch=patch(p);if(patch==null)return;
        var cap=patch.getPlayerSkills();
        if(!d.epicSkillsImported){
            cap.listAcquiredSkills().filter(s->s.getCategory().learnable()).forEach(s->d.unlockedSkills.add(s.getRegistryName().toString()));
            d.epicSkillsImported=true;d.dirty=true;
        }
        var restored=new ArrayList<Holder<Skill>>();
        for(var node:catalog())if(d.unlockedSkills.contains(node.id())){
            var skill=skill(node.id());if(!cap.hasLearned(skill)){cap.addLearnedSkill(skill);restored.add(skill.holder());}
        }
        if(!restored.isEmpty())EpicFightNetworkManager.sendToPlayer(new SPAddLearnedSkill(restored),p);
    }
    public static boolean canUnlock(ServerPlayer p,String id){
        var skill=skill(id);return patch(p)!=null&&skill!=null&&skill.getCategory().learnable();
    }
    public static void learn(ServerPlayer p,String id){
        var patch=patch(p);var skill=skill(id);if(patch==null||skill==null)return;
        var cap=patch.getPlayerSkills();cap.addLearnedSkill(skill);
        EpicFightNetworkManager.sendToPlayer(new SPAddLearnedSkill(List.of(skill.holder())),p);
        var empty=cap.getFirstEmptyContainer(skill.getCategory());if(empty!=null)equip(p,id,empty.getSlotId());
    }
    public static boolean equip(ServerPlayer p,String id,int slot){
        var patch=patch(p);if(patch==null)return false;var cap=patch.getPlayerSkills();var skill=skill(id);
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
        patch.getPlayerSkills().listSkillContainers().filter(c->c.getSlot().category().learnable()).forEach(c->{
            var n=new CompoundTag();n.putInt("slot",c.getSlotId());n.putString("category",c.getSlot().category().toString().toLowerCase(Locale.ROOT));
            n.putString("name",c.getSlot().toString().toLowerCase(Locale.ROOT));n.putString("skill",c.isEmpty()?"":c.getSkill().getRegistryName().toString());n.putInt("cooldown",c.getReplaceCooldown());list.add(n);
        });return list;
    }
    public static boolean nativeChangeAllowed(ServerPlayerPatch patch,SkillSlot slot,int bookSlot,Skill skill){
        if(bookSlot>=0||slot==null||!slot.category().learnable()||patch.getOriginal().isRemoved()||!patch.getOriginal().isAlive())return false;
        if(skill==null)return true;
        var d=PlayerProgressionProvider.get(patch.getOriginal()).orElse(null);
        return d!=null&&skill.getCategory()==slot.category()&&d.unlockedSkills.contains(skill.getRegistryName().toString())&&patch.getPlayerSkills().hasLearned(skill);
    }
    public static boolean book(ItemStack stack){return stack.getItem() instanceof yesman.epicfight.world.item.SkillBookItem;}
    private EpicFightSkills(){}
}

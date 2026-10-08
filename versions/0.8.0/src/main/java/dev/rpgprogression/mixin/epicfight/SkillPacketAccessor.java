package dev.rpgprogression.mixin.epicfight;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Pseudo
@Mixin(value=CPChangeSkill.class,remap=false)
public interface SkillPacketAccessor {
    @Accessor("skillSlot") SkillSlot rpg$slot();
    @Accessor("skillBookSlotIndex") int rpg$bookSlot();
    @Accessor("skill") Skill rpg$skill();
}

package dev.rpgprogression.mixin.parcool;
import com.alrex.parcool.api.action.ActionEntry;
import com.alrex.parcool.common.skilltree.SkillTree;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;
@Pseudo
@Mixin(value=SkillTree.Entry.class,remap=false)
public interface SkillEntryAccessor {
    @Accessor("dependingActions") List<ActionEntry<?>> rpg$dependencies();
}

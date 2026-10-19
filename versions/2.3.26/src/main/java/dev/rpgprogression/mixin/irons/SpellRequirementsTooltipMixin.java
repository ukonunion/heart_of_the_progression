package dev.rpgprogression.mixin.irons;

import dev.rpgprogression.compat.IronsRequirements;
import dev.rpgprogression.requirements.RequirementText;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

@Pseudo @Mixin(targets="io.redspace.ironsspellbooks.util.TooltipsUtils",remap=false)
public abstract class SpellRequirementsTooltipMixin {
    @Inject(method="formatActiveSpellTooltip",at=@At("RETURN"),cancellable=true)
    private static void rpg$requirements(ItemStack stack,SpellData spell,CastSource source,LocalPlayer player,CallbackInfoReturnable<List<MutableComponent>> cir){
        if(player==null||spell==null||spell==SpellData.EMPTY)return;
        var missing=IronsRequirements.spellUnmet(player,spell.getSpell(),spell.getSpell().getLevelFor(spell.getLevel(),player));
        if(missing.isEmpty())return;
        var lines=new ArrayList<>(cir.getReturnValue());lines.addAll(Math.min(1,lines.size()),RequirementText.lines(missing));cir.setReturnValue(lines);
    }
}

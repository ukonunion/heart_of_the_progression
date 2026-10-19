package dev.rpgprogression.mixin.epicfight;
import dev.rpgprogression.skill.SkillTreeManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="yesman.epicfight.data.loot.SkillBookLootModifier",remap=false)
public abstract class BookMobLootMixin {
    @Inject(method="doApply",at=@At("HEAD"),cancellable=true)
    private void rpg$noBookDrops(ObjectArrayList<ItemStack> loot,LootContext context,CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir){if(SkillTreeManager.epic())cir.setReturnValue(loot);}
}

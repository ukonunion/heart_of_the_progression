package dev.rpgprogression.mixin;
import dev.rpgprogression.compat.EpicFightSkills;
import dev.rpgprogression.skill.SkillTreeManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Final filter also handles datapack/addon books, after every global loot modifier. */
@Mixin(value=ForgeHooks.class,remap=false)
public abstract class GeneratedLootMixin {
    @Inject(method="modifyLoot(Lnet/minecraft/resources/ResourceLocation;Lit/unimi/dsi/fastutil/objects/ObjectArrayList;Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",at=@At("RETURN"))
    private static void rpg$filterBooks(ResourceLocation id,ObjectArrayList<ItemStack> loot,LootContext context,CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir){
        if(SkillTreeManager.epic())cir.getReturnValue().removeIf(EpicFightSkills::book);
        cir.getReturnValue().removeIf(dev.rpgprogression.compat.DisabledIronsWeapons::blocked);
    }
}

package dev.rpgprogression.mixin.epicfight;
import dev.rpgprogression.skill.SkillTreeManager;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="yesman.epicfight.data.loot.EpicFightLootTables",remap=false)
public abstract class BookChestLootMixin {
    @Inject(method="onLootTableRegistry",at=@At("HEAD"),cancellable=true)
    private static void rpg$noBookPools(LootTableLoadEvent event,CallbackInfo ci){if(SkillTreeManager.epic())ci.cancel();}
}

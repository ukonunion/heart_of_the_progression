package dev.rpgprogression.mixin;

import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Import the previous mod's player data once; an existing NeoForge save wins. */
@Mixin(Entity.class)
public abstract class LegacyProgressionMixin {
    @Inject(method="load", at=@At("TAIL"))
    private void rpg$importForgeProgression(CompoundTag entityTag, CallbackInfo ci) {
        // Optional resource integrations may request default data during native
        // entity loading. Only data actually present in this save takes priority.
        if((Object)this instanceof Player player && !entityTag.getCompound(AttachmentHolder.ATTACHMENTS_NBT_KEY)
                .contains("rpgprogression:progression",10)) {
            CompoundTag old=entityTag.getCompound("ForgeCaps");
            if(old.contains("rpgprogression:progression",10)) {
                PlayerProgressionProvider.get(player).orElseThrow().load(old.getCompound("rpgprogression:progression"));
            }
        }
    }
}

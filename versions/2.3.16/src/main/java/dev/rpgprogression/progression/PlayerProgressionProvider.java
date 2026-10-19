package dev.rpgprogression.progression;

import dev.rpgprogression.RPGProgression;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.*;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Persistent, owner-aware data; a respawn always gets its own resource owner. */
public final class PlayerProgressionProvider {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RPGProgression.MOD_ID);
    public static final Supplier<AttachmentType<PlayerProgression>> PROGRESSION = ATTACHMENTS.register("progression", () ->
        AttachmentType.builder(holder -> new PlayerProgression((Player)holder)).serialize(new IAttachmentSerializer<CompoundTag, PlayerProgression>() {
            @Override public PlayerProgression read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider lookup) {
                var data = new PlayerProgression((Player)holder); data.load(tag); return data;
            }
            @Override public CompoundTag write(PlayerProgression data, HolderLookup.Provider lookup) { return data.save(); }
        }).build());
    public static Optional<PlayerProgression> get(Player player) {
        return player == null ? Optional.empty() : Optional.of(player.getData(PROGRESSION));
    }
    private PlayerProgressionProvider() {}
}

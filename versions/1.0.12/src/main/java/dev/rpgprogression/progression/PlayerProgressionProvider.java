package dev.rpgprogression.progression;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerProgressionProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerProgression> CAPABILITY=CapabilityManager.get(new CapabilityToken<>(){});
    private final PlayerProgression data;
    public PlayerProgressionProvider(net.minecraft.world.entity.player.Player owner){data=new PlayerProgression(owner);optional=LazyOptional.of(()->data);}
    private LazyOptional<PlayerProgression> optional;
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,@Nullable Direction side) {
        if(cap!=CAPABILITY)return LazyOptional.empty();
        // Entity.reviveCaps re-enables the dispatcher during Clone. Recreate the invalidated
        // handle while retaining the data; previously acquired handles remain invalid.
        if(optional==null)optional=LazyOptional.of(()->data);
        return optional.cast();
    }
    @Override public CompoundTag serializeNBT() { return data.save(); }
    @Override public void deserializeNBT(CompoundTag nbt) { data.load(nbt); }
    public void invalidate() { if(optional!=null){optional.invalidate();optional=null;} }
}

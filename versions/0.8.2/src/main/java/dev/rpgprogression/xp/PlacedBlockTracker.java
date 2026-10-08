package dev.rpgprogression.xp;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-dimension saved positions survive restart and chunk unload. No world/chunk scans. */
public final class PlacedBlockTracker extends SavedData {
    private final LongOpenHashSet placed=new LongOpenHashSet();
    public static PlacedBlockTracker get(ServerLevel l) { return l.getDataStorage().computeIfAbsent(PlacedBlockTracker::load,PlacedBlockTracker::new,"rpgprogression_placed_blocks"); }
    private static PlacedBlockTracker load(CompoundTag n) { PlacedBlockTracker t=new PlacedBlockTracker(); t.placed.addAll(LongOpenHashSet.of(n.getLongArray("positions"))); return t; }
    @Override public CompoundTag save(CompoundTag n) { n.putLongArray("positions",placed.toLongArray()); return n; }
    public void mark(BlockPos p) { if(placed.add(p.asLong()))setDirty(); }
    public boolean contains(BlockPos p) { return placed.contains(p.asLong()); }
    public boolean remove(BlockPos p) { boolean found=placed.remove(p.asLong()); if(found)setDirty(); return found; }
}

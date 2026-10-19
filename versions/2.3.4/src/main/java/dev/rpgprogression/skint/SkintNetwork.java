package dev.rpgprogression.skint;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide point identities. Replacing a removed block creates a new point. */
public final class SkintNetwork extends SavedData {
    public record Point(UUID id,ResourceKey<Level> dimension,BlockPos pos){
        public GlobalPos location(){return GlobalPos.of(dimension,pos);}
        public CompoundTag tag(){var n=new CompoundTag();n.putUUID("id",id);n.putString("dimension",dimension.location().toString());n.putLong("pos",pos.asLong());return n;}
        public static Point read(CompoundTag n){return new Point(n.getUUID("id"),ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(n.getString("dimension"))),BlockPos.of(n.getLong("pos")));}
    }
    private final Map<UUID,Point> points=new LinkedHashMap<>();
    private final Map<GlobalPos,UUID> positions=new HashMap<>();
    public static SkintNetwork get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(SkintNetwork::new,SkintNetwork::load),"rpgprogression_skints");}
    public Point ensure(ServerLevel level,BlockPos pos){
        var location=GlobalPos.of(level.dimension(),pos.immutable());var existing=positions.get(location);
        if(existing!=null)return points.get(existing);
        var point=new Point(UUID.randomUUID(),level.dimension(),pos.immutable());points.put(point.id(),point);positions.put(location,point.id());setDirty();return point;
    }
    public Point point(UUID id){return points.get(id);}
    public void remove(ServerLevel level,BlockPos pos){var id=positions.remove(GlobalPos.of(level.dimension(),pos));if(id!=null){points.remove(id);setDirty();}}
    public List<Point> discovered(Set<UUID> ids){return points.values().stream().filter(p->ids.contains(p.id())).toList();}
    public static SkintNetwork load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new SkintNetwork();for(var entry:tag.getList("points",Tag.TAG_COMPOUND)){
            try{var point=Point.read((CompoundTag)entry);if(data.positions.putIfAbsent(point.location(),point.id())==null)data.points.put(point.id(),point);}
            catch(RuntimeException malformed){/* Retain the other saved points. */}
        }return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){var entries=new ListTag();for(var point:points.values())entries.add(point.tag());tag.put("points",entries);return tag;}
}

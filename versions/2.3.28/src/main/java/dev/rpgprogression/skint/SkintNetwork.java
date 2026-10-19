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
    public record Point(UUID id,ResourceKey<Level> dimension,BlockPos pos,String name){
        public Point { name=SkintNames.clean(name); }
        public Point(UUID id,ResourceKey<Level> dimension,BlockPos pos){this(id,dimension,pos,"");}
        public boolean sameIdentity(Point other){return other!=null&&id.equals(other.id)&&dimension.equals(other.dimension)&&pos.equals(other.pos);}
        public GlobalPos location(){return GlobalPos.of(dimension,pos);}
        public CompoundTag tag(){var n=new CompoundTag();n.putUUID("id",id);n.putString("dimension",dimension.location().toString());n.putLong("pos",pos.asLong());n.putString("name",name);return n;}
        public static Point read(CompoundTag n){return new Point(n.getUUID("id"),ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(n.getString("dimension"))),BlockPos.of(n.getLong("pos")),n.getString("name"));}
    }
    private final Map<UUID,Point> points=new LinkedHashMap<>();
    private final Map<GlobalPos,UUID> positions=new HashMap<>();
    public static SkintNetwork get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(SkintNetwork::new,SkintNetwork::load),"rpgprogression_skints");}
    public Point ensure(ServerLevel level,BlockPos pos){
        level.getChunkSource().getLightEngine().checkBlock(pos);
        var location=GlobalPos.of(level.dimension(),pos.immutable());var existing=positions.get(location);
        if(existing!=null)return points.get(existing);
        var point=new Point(UUID.randomUUID(),level.dimension(),pos.immutable(),level.getBlockEntity(pos) instanceof dev.rpgprogression.block.TravelSkintBlockEntity e?e.customName():"");points.put(point.id(),point);positions.put(location,point.id());setDirty();return point;
    }
    public boolean rename(ServerLevel level,BlockPos pos,String name){
        var old=pointAt(level,pos);if(old==null)return false;
        String clean=SkintNames.clean(name);if(old.name().equals(clean))return true;
        points.put(old.id(),new Point(old.id(),old.dimension(),old.pos(),clean));setDirty();return true;
    }
    public Point point(UUID id){return points.get(id);}
    public Point pointAt(ServerLevel level,BlockPos pos){return points.get(positions.get(GlobalPos.of(level.dimension(),pos)));}
    /** Saved chunk light predates this release; refresh known sources when a player receives the chunk. */
    public void refreshLight(ServerLevel level,net.minecraft.world.level.ChunkPos chunk){
        for(var point:points.values())if(point.dimension().equals(level.dimension())&&(point.pos().getX()>>4)==chunk.x&&(point.pos().getZ()>>4)==chunk.z
                &&level.getBlockState(point.pos()).is(dev.rpgprogression.block.ModBlocks.TRAVELSKINT.get())){
            if(level.getBlockEntity(point.pos()) instanceof dev.rpgprogression.block.TravelSkintBlockEntity entity)entity.migrateLegacy();
            level.getChunkSource().getLightEngine().checkBlock(point.pos());
        }
    }
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

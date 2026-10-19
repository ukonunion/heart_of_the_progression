package dev.rpgprogression.mob;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** One first-dragon guarantee per save, shared by every dimension. */
public final class SkintChargeWorldData extends SavedData {
    private boolean firstDragonHandled;
    public static SkintChargeWorldData get(ServerLevel level){
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(SkintChargeWorldData::new,SkintChargeWorldData::load),"rpgprogression_skint_charge");
    }
    public boolean claimFirstDragon(boolean previouslyKilled){
        if(firstDragonHandled)return false;
        firstDragonHandled=true;setDirty();
        return !previouslyKilled;
    }
    public static SkintChargeWorldData load(CompoundTag tag,HolderLookup.Provider lookup){
        var data=new SkintChargeWorldData();data.firstDragonHandled=tag.getBoolean("firstDragonHandled");return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){tag.putBoolean("firstDragonHandled",firstDragonHandled);return tag;}
}

package dev.rpgprogression.block;

import dev.rpgprogression.skint.SkintNetwork;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A shared, persistent activation clock. Clients only render it; the server enables the point. */
public final class TravelSkintBlockEntity extends BlockEntity {
    public static final int ACTIVATION_TICKS=25;
    private long activationStarted=-1;
    private java.util.UUID activationOwner;
    private boolean initialized;
    private String customName="";
    public String customName(){return customName;}
    public void setCustomName(String name){
        customName=dev.rpgprogression.skint.SkintNames.clean(name);setChanged();
        if(level instanceof ServerLevel server){
            SkintNetwork.get(server).rename(server,worldPosition,customName);
            server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        }
    }
    public TravelSkintBlockEntity(BlockPos pos,BlockState state){super(ModBlocks.TRAVELSKINT_ENTITY.get(),pos,state);}
    public long activationStarted(){return activationStarted;}
    public float animationSeconds(float partialTick){
        if(getBlockState().getValue(TravelSkintBlock.ACTIVE))return ACTIVATION_TICKS/20f;
        return level==null||activationStarted<0?0:Math.clamp((level.getGameTime()-activationStarted+partialTick)/20f,0,ACTIVATION_TICKS/20f);
    }
    @Override public void onLoad(){super.onLoad();migrateLegacy();}
    /** Older releases registered every placed point and had no block entity or active property. */
    public void migrateLegacy(){
        if(!(level instanceof ServerLevel server)||initialized)return;
        initialized=true;
        if(SkintNetwork.get(server).pointAt(server,worldPosition)!=null&&!getBlockState().getValue(TravelSkintBlock.ACTIVE))
            server.setBlock(worldPosition,getBlockState().setValue(TravelSkintBlock.ACTIVE,true),3);
        setChanged();
    }
    public boolean beginActivation(){return beginActivation(null);}
    public boolean beginActivation(net.minecraft.server.level.ServerPlayer player){
        if(!(level instanceof ServerLevel server)||isRemoved())return false;
        migrateLegacy();
        if(getBlockState().getValue(TravelSkintBlock.ACTIVE)||activationStarted>=0||!getBlockState().canSurvive(server,worldPosition))return false;
        activationStarted=server.getGameTime();activationOwner=player==null?null:player.getUUID();setChanged();
        server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        return true;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,TravelSkintBlockEntity entity){
        if(!(level instanceof ServerLevel server)||!state.is(ModBlocks.TRAVELSKINT.get())||entity.isRemoved())return;
        entity.migrateLegacy();
        if(!state.getValue(TravelSkintBlock.ACTIVE)&&entity.activationStarted>=0&&level.getGameTime()-entity.activationStarted>=ACTIVATION_TICKS){
            if(!state.canSurvive(level,pos)){level.removeBlock(pos,false);return;}
            entity.activationStarted=-1;
            level.setBlock(pos,state.setValue(TravelSkintBlock.ACTIVE,true),3);
            SkintNetwork.get(server).ensure(server,pos);entity.setChanged();
            level.sendBlockUpdated(pos,entity.getBlockState(),entity.getBlockState(),3);
            var owner=entity.activationOwner==null?null:server.getServer().getPlayerList().getPlayer(entity.activationOwner);
            entity.activationOwner=null;entity.setChanged();
            if(owner!=null&&owner.isAlive()&&owner.serverLevel()==server)dev.rpgprogression.network.ModNetworking.skintCharged(owner);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putBoolean("activationInitialized",true);tag.putLong("activationStarted",activationStarted);tag.putString("skintName",customName);
        if(activationOwner!=null)tag.putUUID("activationOwner",activationOwner);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);customName=dev.rpgprogression.skint.SkintNames.clean(tag.getString("skintName"));initialized=tag.getBoolean("activationInitialized");
        activationStarted=tag.contains("activationStarted")?Math.max(-1,tag.getLong("activationStarted")):-1;
        activationOwner=activationStarted>=0&&tag.hasUUID("activationOwner")?tag.getUUID("activationOwner"):null;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}

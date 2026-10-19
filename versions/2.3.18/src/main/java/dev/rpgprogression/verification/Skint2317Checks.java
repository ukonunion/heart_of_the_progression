package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.block.*;
import dev.rpgprogression.skint.*;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;

public final class Skint2317Checks {
    public static final BlockPos SAVED=new BlockPos(220,196,30);
    private static void check(JsonArray a,boolean ok,String text){SkintTravelChecks.check(a,ok,text);}
    public static void run(ServerLevel level,JsonArray checks){
        var p=ConfigurationChecks.player(level);var pos=SAVED;SkintTravelChecks.point(level,pos);
        p.setPos(Vec3.atBottomCenterOf(pos).add(0,0,2));SkintManager.use(p,pos);
        var network=SkintNetwork.get(level);var old=network.pointAt(level,pos);var token=SkintManager.session(p).token();
        check(checks,SkintManager.canProgress(p),"rename fixture uses a real server-authorized seated session");
        SkintManager.rename(p,java.util.UUID.randomUUID(),"forged");check(checks,network.point(old.id()).name().isEmpty(),"rename rejects a forged session token");
        SkintManager.rename(p,token,"  Островок Невера  ");var renamed=network.point(old.id());
        check(checks,renamed.name().equals("Островок Невера")&&renamed.sameIdentity(old)&&SkintManager.canProgress(p),"renaming preserves identity, discovery and the active progression session");
        var entity=(TravelSkintBlockEntity)level.getBlockEntity(pos);
        check(checks,entity.customName().equals(renamed.name()),"the block entity and global travel list share the new name");
        SkintManager.rename(p,token,"x".repeat(65));check(checks,network.point(old.id()).name().equals("Островок Невера"),"oversized malicious rename requests are rejected");
        SkintManager.rename(p,token,"");check(checks,network.point(old.id()).name().isEmpty()&&SkintManager.canProgress(p),"an empty name restores the coordinate label without closing the session");
        SkintManager.rename(p,token,"Островок Невера");
        var loaded=SkintNetwork.load(network.save(new CompoundTag(),level.registryAccess()),level.registryAccess());
        check(checks,loaded.point(old.id()).name().equals("Островок Невера")&&SkintNetwork.Point.read(network.point(old.id()).tag()).equals(network.point(old.id())),"saved network and network-payload round trips retain the Unicode name");
        var legacy=old.tag();legacy.remove("name");check(checks,SkintNetwork.Point.read(legacy).name().isEmpty(),"older waypoint records without a name remain compatible");
        var state=level.getBlockState(pos);var block=(TravelSkintBlock)state.getBlock();
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false);
        check(checks,state.useItemOn(new ItemStack(Items.WATER_BUCKET),level,p,InteractionHand.MAIN_HAND,hit)==ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,"bucket block interaction bypasses the activation/menu action");
        check(checks,((BucketItem)Items.WATER_BUCKET).emptyContents(p,level,pos,hit,new ItemStack(Items.WATER_BUCKET)),"a real water bucket can fill the skint");
        state=level.getBlockState(pos);
        check(checks,state.getValue(TravelSkintBlock.WATERLOGGED)&&state.getFluidState().isSource()&&TravelSkintBlock.active(state)&&state.getLightEmission(level,pos)==15,"waterlogging retains the active state, source water and light level 15");
        check(checks,entity.customName().equals("Островок Невера")&&network.point(old.id()).sameIdentity(old)&&SkintManager.canProgress(p),"filling the block preserves the label, UUID and current seated session");
        var copy=(TravelSkintBlockEntity)BlockEntity.loadStatic(pos,state,entity.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
        check(checks,copy.customName().equals("Островок Невера")&&copy.getBlockState().getValue(TravelSkintBlock.WATERLOGGED),"waterlogged named block survives a complete block-entity NBT round trip");
        check(checks,block.pickupBlock(p,level,pos,state).is(Items.WATER_BUCKET)&&!level.getBlockState(pos).getValue(TravelSkintBlock.WATERLOGGED),"an empty bucket drains the contained water without removing the skint");
        check(checks,!block.placeLiquid(level,pos,level.getBlockState(pos),Fluids.LAVA.getSource(false)),"the skint refuses lava");
        block.placeLiquid(level,pos,level.getBlockState(pos),Fluids.WATER.getSource(false));
        var destination=pos.offset(12,0,0);SkintTravelChecks.point(level,destination);var target=network.pointAt(level,destination);
        ((TravelSkintBlockEntity)level.getBlockEntity(destination)).setCustomName("Назначение");
        var d=ProgressionManager.get(p);d.discoveredSkints.add(target.id());d.spark=d.maxSpark();
        check(checks,SkintManager.travel(p,target.id()),"a renamed source can still start travel to a renamed destination");SkintManager.close(p,false);
        var gone=SkintActivationChecks.inactive(level,pos.offset(0,0,12));var gonePos=gone.getBlockPos();block.placeLiquid(level,gonePos,gone.getBlockState(),Fluids.WATER.getSource(false));level.removeBlock(gonePos.below(),false);
        check(checks,level.getBlockState(gonePos).is(Blocks.WATER)&&level.getFluidState(gonePos).isSource()&&level.getBlockEntity(gonePos)==null,"removing support leaves the contained water and removes the inactive skint");
    }
    public static void restart(ServerLevel level,JsonArray checks){
        var state=level.getBlockState(SAVED);var entity=level.getBlockEntity(SAVED);var point=SkintNetwork.get(level).pointAt(level,SAVED);
        check(checks,state.is(ModBlocks.TRAVELSKINT.get())&&state.getValue(TravelSkintBlock.WATERLOGGED)&&TravelSkintBlock.active(state),"full dedicated-server restart retains the waterlogged activated block");
        check(checks,entity instanceof TravelSkintBlockEntity e&&e.customName().equals("Островок Невера")&&point!=null&&point.name().equals("Островок Невера"),"full dedicated-server restart retains the name in the block and travel network");
    }
}

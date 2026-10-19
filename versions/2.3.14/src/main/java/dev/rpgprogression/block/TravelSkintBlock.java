package dev.rpgprogression.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.*;

/** Waypoint and progression access. Removing its solid floor removes it without drops. */
public final class TravelSkintBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TravelSkintBlock> CODEC=simpleCodec(TravelSkintBlock::new);

    public TravelSkintBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<TravelSkintBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var state=defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
        return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        BlockPos floor=pos.below();
        return level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP);
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        return direction==Direction.DOWN&&!state.canSurvive(level,pos)?Blocks.AIR.defaultBlockState():super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return Shapes.block();
    }
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override protected void onPlace(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState old,boolean moved){
        super.onPlace(state,level,pos,old,moved);
        if(!old.is(this)&&level instanceof net.minecraft.server.level.ServerLevel server)dev.rpgprogression.skint.SkintNetwork.get(server).ensure(server,pos);
    }
    @Override protected void onRemove(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState next,boolean moved){
        if(!next.is(this)&&level instanceof net.minecraft.server.level.ServerLevel server)dev.rpgprogression.skint.SkintNetwork.get(server).remove(server,pos);
        super.onRemove(state,level,pos,next,moved);
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(player instanceof net.minecraft.server.level.ServerPlayer server)dev.rpgprogression.skint.SkintManager.use(server,pos);
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}

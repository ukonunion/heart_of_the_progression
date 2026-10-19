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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.shapes.*;

/** Waypoint and progression access. Removing its solid floor removes it without drops. */
public final class TravelSkintBlock extends HorizontalDirectionalBlock implements EntityBlock,SimpleWaterloggedBlock {
    public static final MapCodec<TravelSkintBlock> CODEC=simpleCodec(TravelSkintBlock::new);
    public static final BooleanProperty WATERLOGGED=net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active");

    public TravelSkintBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(ACTIVE,false).setValue(WATERLOGGED,false));
    }
    @Override protected MapCodec<TravelSkintBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,ACTIVE,WATERLOGGED);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TravelSkintBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,BlockState state,BlockEntityType<T> type){
        return !level.isClientSide&&type==ModBlocks.TRAVELSKINT_ENTITY.get()?(l,p,s,e)->TravelSkintBlockEntity.tick(l,p,s,(TravelSkintBlockEntity)e):null;
    }
    public static boolean active(BlockState state){return state.is(ModBlocks.TRAVELSKINT.get())&&state.getValue(ACTIVE);}
    public static boolean active(net.minecraft.world.level.Level level,BlockPos pos){
        if(!level.getBlockState(pos).is(ModBlocks.TRAVELSKINT.get()))return false;
        // Old chunks have no saved block-entity tag. A remote travel request may be their first access.
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof TravelSkintBlockEntity entity)entity.migrateLegacy();
        return active(level.getBlockState(pos));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var state=defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).getType()==net.minecraft.world.level.material.Fluids.WATER);
        return state.canSurvive(context.getLevel(),context.getClickedPos())?state:null;
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        BlockPos floor=pos.below();
        return level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP);
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        if(state.getValue(WATERLOGGED))level.scheduleTick(pos,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        return direction==Direction.DOWN&&!state.canSurvive(level,pos)?state.getFluidState().createLegacyBlock():super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState state){
        return state.getValue(WATERLOGGED)?net.minecraft.world.level.material.Fluids.WATER.getSource(false):super.getFluidState(state);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        // Let the ordinary bucket item handle filling/draining, without activating or opening a menu.
        return stack.is(net.minecraft.world.item.Items.WATER_BUCKET)||stack.is(net.minecraft.world.item.Items.BUCKET)?net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION:net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public void setPlacedBy(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack){
        super.setPlacedBy(level,pos,state,entity,stack);
        var name=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if(name!=null&&level.getBlockEntity(pos) instanceof TravelSkintBlockEntity skint)skint.setCustomName(name.getString());
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return Shapes.block();
    }
    @Override protected VoxelShape getBlockSupportShape(BlockState state,BlockGetter level,BlockPos pos){return Shapes.empty();}
    @Override protected void onPlace(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState old,boolean moved){
        super.onPlace(state,level,pos,old,moved);
        if(state.getValue(ACTIVE)&&!old.is(this)&&level instanceof net.minecraft.server.level.ServerLevel server)dev.rpgprogression.skint.SkintNetwork.get(server).ensure(server,pos);
    }
    @Override protected void onRemove(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState next,boolean moved){
        if(!next.is(this)&&level instanceof net.minecraft.server.level.ServerLevel server)dev.rpgprogression.skint.SkintNetwork.get(server).remove(server,pos);
        super.onRemove(state,level,pos,next,moved);
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(player instanceof net.minecraft.server.level.ServerPlayer server){
            if(level.getBlockEntity(pos) instanceof TravelSkintBlockEntity entity)entity.migrateLegacy();
            if(active(level.getBlockState(pos)))dev.rpgprogression.skint.SkintManager.use(server,pos);
            else if(server.isAlive()&&!server.isSpectator()&&!server.isSleeping()&&server.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=36
                    &&level.getBlockEntity(pos) instanceof TravelSkintBlockEntity entity)entity.beginActivation();
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
}

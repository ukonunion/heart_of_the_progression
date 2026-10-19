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

/** Decorative only. Removing its solid floor removes the block without drops. */
public final class TravelSkintBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<TravelSkintBlock> CODEC=simpleCodec(TravelSkintBlock::new);
    private static final VoxelShape[] SHAPES=shapes();

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
        return SHAPES[switch(state.getValue(FACING)){case EAST->1;case SOUTH->2;case WEST->3;default->0;}];
    }
    private static VoxelShape[] shapes(){
        // Bounds of the ten supplied model parts after their original rotations.
        double[][] boxes={
            {4.832861,.252580,3.911211,15.999334,18.670093,15.025388},
            {6.987306,17.498491,8.482758,16.691898,23.049366,13.325416},
            {9.498269,16.954284,6.085001,14.180935,23.593574,15.723174},
            {.004349,0,1.178952,9.075621,14.523752,9.847709},
            {-.691898,12.691846,1.800422,6.837985,18.431012,7.021240},
            {.313418,13.570766,.654073,5.832670,17.552092,8.167590},
            {1.294778,.694106,9.088138,6.544418,7.636043,13.795823},
            {6.367393,1.288161,.019747,12.203999,7.493249,6.047975},
            {5.150446,1.514481,10.904852,9.275483,6.514795,15.966661},
            {3.037099,.899473,12.505466,5.734618,4.571784,15.980253}
        };
        var result=new VoxelShape[4];
        for(int turn=0;turn<4;turn++){
            VoxelShape shape=Shapes.empty();
            for(double[] source:boxes){
                double x0=source[0],z0=source[2],x1=source[3],z1=source[5];
                for(int i=0;i<turn;i++){double a=16-z1,b=x0,c=16-z0,d=x1;x0=a;z0=b;x1=c;z1=d;}
                shape=Shapes.or(shape,Block.box(x0,source[1],z0,x1,source[4],z1));
            }
            result[turn]=shape.optimize();
        }
        return result;
    }
}

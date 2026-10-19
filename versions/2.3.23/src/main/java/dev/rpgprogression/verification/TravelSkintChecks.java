package dev.rpgprogression.verification;

import com.google.gson.*;
import dev.rpgprogression.block.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.*;

/** Native block checks used by the opt-in client and dedicated-server runs. */
public final class TravelSkintChecks {
    private static void check(JsonArray checks,boolean passed,String name){
        var row=new JsonObject();row.addProperty("check",name);row.addProperty("passed",passed);checks.add(row);
        com.mojang.logging.LogUtils.getLogger().info("SKINT_CHECK {} {}",passed?"PASSED":"FAILED",name);
    }
    private static InteractionResult place(ServerPlayer player,BlockPos floor,BlockState support){
        var level=player.serverLevel();level.setBlock(floor.above(),Blocks.AIR.defaultBlockState(),3);level.setBlock(floor,support,3);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.TRAVELSKINT_ITEM.get(),2));
        return player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false)));
    }
    public static JsonArray run(ServerLevel level,ServerPlayer player,BlockPos base){
        var checks=new JsonArray();var block=ModBlocks.TRAVELSKINT.get();var state=block.defaultBlockState();var pos=base.above();
        player.setGameMode(GameType.SURVIVAL);player.getAbilities().invulnerable=true;player.setNoGravity(true);
        player.teleportTo(base.getX()+.5,base.getY()+1,base.getZ()+3.5);player.setYRot(0);
        check(checks,BuiltInRegistries.BLOCK.getKey(block).toString().equals("rpgprogression:travelskint"),"block is registered under the stable ID");
        check(checks,ModBlocks.TRAVELSKINT_ITEM.get().getBlock()==block,"creative item places the registered block");
        check(checks,place(player,base,Blocks.STONE.defaultBlockState()).consumesAction()&&level.getBlockState(pos).is(block),"real BlockItem placement succeeds on stone");
        check(checks,player.getMainHandItem().getCount()==1,"survival placement consumes exactly one item");
        check(checks,level.getBlockState(pos).getValue(TravelSkintBlock.FACING)==Direction.NORTH,"placement faces toward the player");
        check(checks,!state.hasBlockEntity()&&level.getBlockEntity(pos)==null,"decorative block has no block entity or ticking functionality");
        check(checks,state.getMenuProvider(level,pos)==null,"decorative block does not expose a menu");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.NETHERITE_PICKAXE));
        player.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+2.5);
        check(checks,state.getDestroySpeed(level,pos)==-1&&state.getDestroyProgress(player,level,pos)==0,"netherite pickaxe has zero survival mining progress, as for bedrock");
        player.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,Direction.SOUTH,level.getMaxBuildHeight(),1);
        player.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,Direction.SOUTH,level.getMaxBuildHeight(),2);
        check(checks,level.getBlockState(pos).is(block),"native survival mining actions leave the block intact");
        check(checks,block.getExplosionResistance()==Blocks.BEDROCK.getExplosionResistance(),"explosion resistance matches bedrock");
        check(checks,state.getPistonPushReaction()==PushReaction.BLOCK,"pistons cannot move the block");
        check(checks,Block.getDrops(state,level,pos,null,player,player.getMainHandItem()).isEmpty(),"the block has no loot drops");
        level.setBlock(base,Blocks.AIR.defaultBlockState(),3);
        check(checks,level.getBlockState(pos).isAir(),"removing the supporting block immediately removes the skint");
        check(checks,level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().noneMatch(e->e.getItem().is(ModBlocks.TRAVELSKINT_ITEM.get())),"support loss does not drop a skint item");
        check(checks,!place(player,base,Blocks.AIR.defaultBlockState()).consumesAction()&&!level.getBlockState(pos).is(block),"BlockItem rejects unsupported placement in air");
        check(checks,!place(player,base,Blocks.WATER.defaultBlockState()).consumesAction()&&!level.getBlockState(pos).is(block),"BlockItem rejects water as its floor");
        var slab=Blocks.STONE_SLAB.defaultBlockState();
        check(checks,!place(player,base,slab).consumesAction()&&!level.getBlockState(pos).is(block),"bottom slab is not a full supporting top face");
        check(checks,place(player,base,slab.setValue(SlabBlock.TYPE,SlabType.TOP)).consumesAction()&&level.getBlockState(pos).is(block),"a top slab provides a solid supporting top face");
        level.setBlock(base,slab,3);
        check(checks,level.getBlockState(pos).isAir(),"replacing support with a non-supporting shape also removes the skint");
        place(player,base,Blocks.BEDROCK.defaultBlockState());
        player.setGameMode(GameType.CREATIVE);boolean removed=player.gameMode.destroyBlock(pos);
        check(checks,removed&&level.getBlockState(pos).isAir(),"creative mode can remove the block as it can bedrock");
        ModBlocks.MAIN_TAB.get().buildContents(new CreativeModeTab.ItemDisplayParameters(level.enabledFeatures(),true,level.registryAccess()));
        check(checks,ModBlocks.MAIN_TAB.get().getDisplayItems().stream().filter(s->s.is(ModBlocks.TRAVELSKINT_ITEM.get())).count()==1,"the separate mod creative tab lists the skint once");
        check(checks,ModBlocks.MAIN_TAB.get().getIconItem().is(ModBlocks.TRAVELSKINT_ITEM.get()),"the mod tab uses the skint as its icon");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        return checks;
    }
    private TravelSkintChecks(){}
}

package dev.rpgprogression.mixin;

import dev.rpgprogression.xp.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.UUID;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireMixin extends BlockEntity implements CampfireXPAccess {
    @Shadow @Final private NonNullList<ItemStack> items;
    @Shadow @Final private int[] cookingProgress;
    @Shadow @Final private int[] cookingTime;
    @Unique private final UUID[] rpg$owners=new UUID[4];
    @Unique private final ItemStack[] rpg$completing=new ItemStack[4];
    @Unique private int rpg$placing=-1;
    protected CampfireMixin(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    @Inject(method="placeFood",at=@At("HEAD"))
    private void rpg$slot(Entity entity,ItemStack food,int ticks,CallbackInfoReturnable<Boolean> cir){rpg$placing=-1;for(int i=0;i<4;i++)if(items.get(i).isEmpty()){rpg$placing=i;break;}}
    @Inject(method="placeFood",at=@At("RETURN"))
    private void rpg$owner(Entity entity,ItemStack food,int ticks,CallbackInfoReturnable<Boolean> cir){if(cir.getReturnValue()&&rpg$placing>=0){rpg$owners[rpg$placing]=entity instanceof Player p?p.getUUID():null;setChanged();}}
    @Inject(method="saveAdditional",at=@At("TAIL"))
    private void rpg$save(CompoundTag tag,CallbackInfo ci){for(int i=0;i<4;i++)if(rpg$owners[i]!=null)tag.putUUID("rpgCookOwner"+i,rpg$owners[i]);}
    @Inject(method="load",at=@At("TAIL"))
    private void rpg$load(CompoundTag tag,CallbackInfo ci){for(int i=0;i<4;i++)rpg$owners[i]=tag.hasUUID("rpgCookOwner"+i)?tag.getUUID("rpgCookOwner"+i):null;}
    @Inject(method="cookTick",at=@At("HEAD"))
    private static void rpg$start(Level level,BlockPos pos,BlockState state,CampfireBlockEntity campfire,CallbackInfo ci){if(!level.isClientSide)((CampfireXPAccess)campfire).rpg$beforeCooking();}
    @Inject(method="cookTick",at=@At("TAIL"))
    private static void rpg$finish(Level level,BlockPos pos,BlockState state,CampfireBlockEntity campfire,CallbackInfo ci){if(!level.isClientSide)((CampfireXPAccess)campfire).rpg$afterCooking();}
    @Unique public void rpg$beforeCooking(){
        for(int i=0;i<4;i++)rpg$completing[i]=!items.get(i).isEmpty()&&cookingProgress[i]+1>=cookingTime[i]?items.get(i).copy():null;
    }
    @Unique public void rpg$afterCooking(){
        for(int i=0;i<4;i++)if(rpg$completing[i]!=null&&items.get(i).isEmpty()){
            if(rpg$owners[i]!=null){
                var p=level.getServer().getPlayerList().getPlayer(rpg$owners[i]);
                if(p!=null){
                    var input=new SimpleContainer(rpg$completing[i]);
                    level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CAMPFIRE_COOKING,input,level)
                            .ifPresent(recipe->XPManager.cooked(p,recipe.assemble(input,level.registryAccess()),true));
                }
            }
            rpg$owners[i]=null;rpg$completing[i]=null;setChanged();
        }
    }
}

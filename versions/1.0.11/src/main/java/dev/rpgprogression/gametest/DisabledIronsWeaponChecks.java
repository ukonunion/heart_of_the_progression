package dev.rpgprogression.gametest;

import dev.rpgprogression.compat.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.ProgressionManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

public final class DisabledIronsWeaponChecks {
    public static void run(GameTestHelper h,ServerPlayer p){
        var expected=Set.of("magehunter","spellbreaker","claymore","keeper_flamberge","legionnaire_flamberge","amethyst_rapier","misery","autoloader_crossbow","hither_thither_wand","hellrazor","decrepit_scythe","boreal_blade","twilight_gale");
        var actual=new TreeSet<String>();
        for(var item:ForgeRegistries.ITEMS)if(DisabledIronsWeapons.blocked(item))actual.add(ForgeRegistries.ITEMS.getKey(item).getPath());
        h.assertTrue(actual.equals(expected),"all thirteen native non-staff weapons: "+actual);
        var staffIDs=Set.of("graybeard_staff","pyrium_staff","artificer_cane","ice_staff","lightning_rod","blood_staff","staff_of_the_nines");
        for(var id:staffIDs){var staff=ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks",id));
            h.assertTrue(staff!=null&&!DisabledIronsWeapons.blocked(staff),"staff permitted: "+id);
            h.assertTrue(ItemStack.of(new ItemStack(staff).save(new CompoundTag())).is(staff),"staff NBT survives: "+id);
        }
        h.assertFalse(DisabledIronsWeapons.blocked(ItemRegistry.SCROLL.get()),"scroll permitted");
        h.assertFalse(DisabledIronsWeapons.blocked(ItemRegistry.COPPER_SPELL_BOOK.get()),"book permitted");
        h.assertFalse(DisabledIronsWeapons.blocked(ItemRegistry.WEAPON_PARTS.get()),"weapon crafting material preserved");
        for(var id:expected){
            var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation("irons_spellbooks",id));var stack=new ItemStack(item);
            h.assertTrue(ItemStack.of(stack.save(new CompoundTag())).isEmpty(),"saved weapon purged: "+id);
            var insert=stack.copy();h.assertTrue(p.getInventory().add(insert)&&insert.isEmpty(),"direct give/pickup cannot insert: "+id);
            p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var use=new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);MinecraftForge.EVENT_BUS.post(use);
            h.assertTrue(use.isCanceled()&&use.getCancellationResult()==InteractionResult.FAIL,"right click denied: "+id);
            var mob=h.spawnWithNoFreeWill(EntityType.ZOMBIE,1,2,1);float health=mob.getHealth();
            p.attack(mob);h.assertTrue(mob.getHealth()==health,"native attack denied: "+id);mob.discard();
            DisabledWeaponEvents.sanitize(p);h.assertTrue(p.getMainHandItem().isEmpty(),"live inventory purged: "+id);
        }
        // Rebuilding every native creative tab includes its search contents.
        CreativeModeTabs.tryRebuildTabContents(p.level().enabledFeatures(),true,p.level().registryAccess());
        for(var tab:CreativeModeTabs.allTabs()){
            h.assertFalse(tab.getDisplayItems().stream().anyMatch(DisabledIronsWeapons::blocked),"creative display excludes weapons");
            h.assertFalse(tab.getSearchTabDisplayItems().stream().anyMatch(DisabledIronsWeapons::blocked),"creative search excludes weapons");
        }
        h.assertTrue(CreativeModeTabs.allTabs().stream().flatMap(t->t.getDisplayItems().stream()).anyMatch(s->s.is(ItemRegistry.GRAYBEARD_STAFF.get())),"staff remains in creative tabs");
        for(var id:List.of("amethyst_rapier","spellbreaker","twilight_gale","decrepit_scythe_repair","legionnaire_flamberge_repair"))h.assertTrue(p.server.getRecipeManager().byKey(new ResourceLocation("irons_spellbooks",id)).isEmpty(),"disabled recipe removed: "+id);
        for(var id:List.of("graybeard_staff","ice_staff","pyrium_staff"))h.assertTrue(p.server.getRecipeManager().byKey(new ResourceLocation("irons_spellbooks",id)).isPresent(),"staff recipe retained: "+id);
        h.assertFalse(p.server.getRecipeManager().getRecipes().stream().anyMatch(DisabledIronsWeapons::recipe),"all static disabled outputs removed");
        var manager=p.server.getRecipeManager();var originalRecipes=List.copyOf(manager.getRecipes());
        try{
            var additional=new ArrayList<net.minecraft.world.item.crafting.Recipe<?>>(originalRecipes);
            var forbiddenId=new ResourceLocation("rpgprogression","custom_forbidden_weapon");var staffId=new ResourceLocation("rpgprogression","custom_staff");
            var input=net.minecraft.core.NonNullList.of(net.minecraft.world.item.crafting.Ingredient.EMPTY,net.minecraft.world.item.crafting.Ingredient.of(Items.DIAMOND));
            additional.add(new net.minecraft.world.item.crafting.ShapelessRecipe(forbiddenId,"",net.minecraft.world.item.crafting.CraftingBookCategory.MISC,new ItemStack(ItemRegistry.MAGEHUNTER.get()),input));
            additional.add(new net.minecraft.world.item.crafting.ShapelessRecipe(staffId,"",net.minecraft.world.item.crafting.CraftingBookCategory.MISC,new ItemStack(ItemRegistry.GRAYBEARD_STAFF.get()),input));
            manager.replaceRecipes(additional);h.assertTrue(manager.byKey(forbiddenId).isEmpty()&&manager.byKey(staffId).isPresent(),"custom namespace/recipe replacement filters by output");
        }finally{manager.replaceRecipes(originalRecipes);}
        var loot=it.unimi.dsi.fastutil.objects.ObjectArrayList.of(new ItemStack(ItemRegistry.MAGEHUNTER.get()),new ItemStack(ItemRegistry.GRAYBEARD_STAFF.get()),new ItemStack(Items.DIAMOND));
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(p.serverLevel()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.EMPTY);
        var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(null);
        var result=net.minecraftforge.common.ForgeHooks.modifyLoot(new ResourceLocation("rpgprogression","disabled_weapon_test"),loot,context);
        h.assertTrue(result.size()==2&&!result.stream().anyMatch(DisabledIronsWeapons::blocked),"final global loot removes weapons and preserves staff");
        var drop=new ItemEntity(p.level(),p.getX(),p.getY(),p.getZ(),new ItemStack(ItemRegistry.MAGEHUNTER.get()));h.assertFalse(p.serverLevel().addFreshEntity(drop),"new dropped weapon cannot spawn");
        var village=h.spawnWithNoFreeWill(EntityType.VILLAGER,2,2,2);village.getOffers().clear();
        village.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD),new ItemStack(ItemRegistry.MAGEHUNTER.get()),10,1,.05f));
        village.getOffers().add(new MerchantOffer(new ItemStack(Items.EMERALD),new ItemStack(ItemRegistry.GRAYBEARD_STAFF.get()),10,1,.05f));
        h.assertTrue(village.getOffers().size()==1&&village.getOffers().get(0).getResult().is(ItemRegistry.GRAYBEARD_STAFF.get()),"trade filtering preserves staff");village.discard();
        var savedOffer=new MerchantOffer(new ItemStack(Items.EMERALD),new ItemStack(ItemRegistry.MAGEHUNTER.get()),10,1,.05f).createTag();
        var savedCost=new MerchantOffer(new ItemStack(ItemRegistry.MAGEHUNTER.get()),new ItemStack(Items.DIAMOND),10,1,.05f).createTag();
        var legacyVillage=h.spawnWithNoFreeWill(EntityType.VILLAGER,2,2,2);legacyVillage.getOffers().clear();
        legacyVillage.getOffers().add(new MerchantOffer(savedOffer));legacyVillage.getOffers().add(new MerchantOffer(savedCost));
        h.assertTrue(legacyVillage.getOffers().isEmpty(),"saved disabled output/input cannot leave blank or free trades");legacyVillage.discard();
        var data=ProgressionManager.get(p);data.set(dev.rpgprogression.attribute.RPGAttribute.SPARK_POTENTIAL,50);IronsCompat.refresh(p,data);data.mana=8;
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ItemRegistry.MAGEHUNTER.get()));
        h.assertFalse(SpellRegistry.FIREBOLT_SPELL.get().canBeCastedBy(1,CastSource.SWORD,MagicData.getPlayerMagicData(p),p).isSuccess(),"native weapon spell denied");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ItemRegistry.GRAYBEARD_STAFF.get()));
        h.assertTrue(SpellRegistry.FIREBOLT_SPELL.get().canBeCastedBy(1,CastSource.SWORD,MagicData.getPlayerMagicData(p),p).isSuccess(),"staff casting permitted");
        boolean old=RPGConfig.IRONS_MANA.get();RPGConfig.IRONS_MANA.set(false);
        try{h.assertTrue(DisabledIronsWeapons.blocked(ItemRegistry.MAGEHUNTER.get()),"ban independent of mana setting");}finally{RPGConfig.IRONS_MANA.set(old);}
        if(EpicFightCompat.present())DisabledIronsEpicChecks.run(h,p);
        p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ItemRegistry.MAGEHUNTER.get()));
        var use=new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);MinecraftForge.EVENT_BUS.post(use);h.assertTrue(use.isCanceled(),"creative cannot bypass weapon ban");DisabledWeaponEvents.sanitize(p);
        com.mojang.logging.LogUtils.getLogger().info("HEART_WEAPONS_VERIFIED disabled={}, allowed_staves={}",actual,staffIDs);
    }
    private DisabledIronsWeaponChecks(){}
}

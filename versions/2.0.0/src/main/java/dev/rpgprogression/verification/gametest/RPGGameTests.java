package dev.rpgprogression.verification.gametest;

import dev.rpgprogression.attribute.*;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.defense.*;
import dev.rpgprogression.event.CommonEvents;
import dev.rpgprogression.inventory.*;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.recipe.IntelligenceRecipeManager;
import dev.rpgprogression.stamina.StaminaManager;
import dev.rpgprogression.xp.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import java.util.List;

/** Integration tests run with real NeoForge events and applied mixins on a headless server. */
@GameTestHolder("rpgprogression")
@PrefixGameTestTemplate(false)
public final class RPGGameTests {
    private static boolean testConfig=false;
    private static ServerPlayer player(GameTestHelper h) {
        // Tests change rules in memory; file watcher reloads must not race those temporary values.
        if(!testConfig){testConfig=true;useInMemoryConfig();}
        ServerPlayer p=new TestPlayer(h.getLevel());p.setGameMode(GameType.SURVIVAL);
        p.setPos(h.absolutePos(new BlockPos(2,2,2)).getX(),h.absolutePos(new BlockPos(2,2,2)).getY(),h.absolutePos(new BlockPos(2,2,2)).getZ());
        PlayerProgression d=ProgressionManager.get(p);d.load(new PlayerProgression().save());d.initialize();
        return p;
    }
    private static void near(GameTestHelper h,double actual,double expected,String what) {h.assertTrue(Math.abs(actual-expected)<0.001,what+": "+actual+" != "+expected);}
    private static void useInMemoryConfig(){
        try{
            // FML's sealed implementation is package-private; construct its real in-memory form for this fixture.
            var field=net.neoforged.fml.config.ConfigTracker.class.getDeclaredField("configSets");field.setAccessible(true);
            var sets=(java.util.Map<net.neoforged.fml.config.ModConfig.Type,java.util.Set<net.neoforged.fml.config.ModConfig>>)field.get(net.neoforged.fml.config.ConfigTracker.INSTANCE);
            var modConfig=sets.get(net.neoforged.fml.config.ModConfig.Type.SERVER).stream().filter(c->c.getSpec()==RPGConfig.SERVER).findFirst().orElseThrow();
            var memory=com.electronwill.nightconfig.core.CommentedConfig.inMemory();RPGConfig.SERVER.correct(memory);
            var constructor=Class.forName("net.neoforged.fml.config.LoadedConfig").getDeclaredConstructor(com.electronwill.nightconfig.core.CommentedConfig.class,java.nio.file.Path.class,net.neoforged.fml.config.ModConfig.class);constructor.setAccessible(true);
            RPGConfig.SERVER.acceptConfig((net.neoforged.fml.config.IConfigSpec.ILoadedConfig)constructor.newInstance(memory,null,modConfig));
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Native in-memory test configuration could not be loaded",error);}
    }
    @GameTest(template="empty") public static void rangedStaminaActions(GameTestHelper h){
        RangedNativeChecks.run(player(h),(ok,message)->h.assertTrue(ok,message));h.succeed();
    }
    @GameTest(template="empty") public static void attributeBalanceAndEnduranceSavings(GameTestHelper h){
        AttributeBalanceChecks.run(player(h),(ok,message)->h.assertTrue(ok,message));h.succeed();
    }
    @GameTest(template="empty") public static void defaultsAndLevels(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);
        h.assertTrue(d.level==1&&d.xp==0&&d.points==0,"new player defaults");
        for(var a:RPGAttribute.values())h.assertTrue(d.get(a)==10,"initial "+a);
        ProgressionManager.addXP(p,499);h.assertTrue(d.level==1&&d.points==0&&d.xp==499,"499 XP stays below the new threshold");
        ProgressionManager.addXP(p,1);h.assertTrue(d.level==2&&d.points==1&&d.xp==0,"500 XP level-up");
        ProgressionManager.addXP(p,550+600+42);h.assertTrue(d.level==4&&d.points==3&&d.xp==42,"staged multi-level carry");
        int vanilla=p.totalExperience;ProgressionManager.addXP(p,1);h.assertTrue(p.totalExperience==vanilla,"vanilla XP untouched");
        d.level=1001;d.xp=0;ProgressionManager.addXP(p,d.requiredXP());
        h.assertTrue(d.level==1002&&d.xp==0,"XP continues past former level limits");
        var copy=new PlayerProgression();copy.load(d.save());h.assertTrue(copy.level==1002,"high level persists");
        double oldBase=RPGConfig.BASE_XP.get(),oldMultiplier=RPGConfig.XP_MULTIPLIER.get();
        var oldCurve=RPGConfig.XP_CURVE.get();
        try{RPGConfig.BASE_XP.set(1.0);RPGConfig.XP_MULTIPLIER.set(1.0);RPGConfig.XP_CURVE.set(ProgressionMath.Curve.EXPONENTIAL);d.level=Integer.MAX_VALUE-2;d.xp=0;d.points=0;
            ProgressionManager.addXPAdministrative(p,ProgressionMath.XP_LIMIT);
            h.assertTrue(d.level==Integer.MAX_VALUE&&d.points==2&&d.xp==ProgressionMath.XP_LIMIT-2,"large flat-curve award batches without overflow or losing XP");
        }finally{RPGConfig.BASE_XP.set(oldBase);RPGConfig.XP_MULTIPLIER.set(oldMultiplier);RPGConfig.XP_CURVE.set(oldCurve);}h.succeed();
    }
    @GameTest(template="empty") public static void mobExperienceVariation(GameTestHelper h){MobVariationChecks.run(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void optionalDisabledIronsWeapons(GameTestHelper h){
        h.assertFalse(dev.rpgprogression.compat.DisabledIronsWeapons.blocked(Items.IRON_SWORD),"vanilla weapon retained");
        if(dev.rpgprogression.compat.IronsCompat.present())DisabledIronsWeaponChecks.run(h,player(h));
        else h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.stream().noneMatch(dev.rpgprogression.compat.DisabledIronsWeapons::blocked),"standalone has no forbidden weapons");
        h.succeed();
    }
    @GameTest(template="empty") public static void stagedMilestonesAndLargeAwards(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);
        for(int level:new int[]{4,5,49,50,99,100}){
            d.level=level;d.xp=0;d.points=0;d.skillPoints=0;d.skillMilestones=level/RPGConfig.SKILL_LEVEL_INTERVAL.get();
            long cost=d.requiredXP();ProgressionManager.addXPAdministrative(p,cost-1);
            h.assertTrue(d.level==level&&d.xp==cost-1,"below milestone "+level);
            ProgressionManager.addXPAdministrative(p,1);
            h.assertTrue(d.level==level+1&&d.xp==0&&d.points==1,"exact milestone "+level);
            h.assertTrue(d.skillPoints==((level+1)%5==0?1:0),"skill milestone "+level);
        }
        d.level=1;d.xp=0;d.points=0;d.skillPoints=0;d.skillMilestones=0;
        ProgressionManager.addXPAdministrative(p,1137615);
        h.assertTrue(d.level==100&&d.xp==0&&d.points==99&&d.skillPoints==20,"complete staged progression to 100");
        var copy=new PlayerProgression();copy.load(d.save());h.assertTrue(copy.level==100&&copy.xp==0&&copy.points==99&&copy.skillPoints==20,"progress persists");
        double oldBase=RPGConfig.BASE_XP.get(),oldMultiplier=RPGConfig.XP_MULTIPLIER.get();
        try{
            // The legacy multiplier must not trigger flat-curve batching in staged mode.
            RPGConfig.XP_MULTIPLIER.set(1.0);d.level=1;d.xp=0;d.points=0;
            ProgressionManager.addXPAdministrative(p,2000);
            h.assertTrue(d.level==4&&d.xp==350&&d.points==3,"staged curve ignores legacy multiplier");
            RPGConfig.BASE_XP.set(1.0);d.level=1;d.xp=0;d.points=0;
            ProgressionManager.addXPAdministrative(p,ProgressionMath.XP_LIMIT);
            h.assertTrue(d.level>1000000&&d.level<2000000&&d.points==d.level-1&&d.xp<d.requiredXP(),"huge staged award is bounded and retains carry");
            d.level=Integer.MAX_VALUE-1;d.xp=0;d.points=0;
            ProgressionManager.addXPAdministrative(p,ProgressionMath.XP_LIMIT);
            h.assertTrue(d.level==Integer.MAX_VALUE&&d.xp==0&&d.points==1,"saturated high-level cost without overflow");
        }finally{RPGConfig.BASE_XP.set(oldBase);RPGConfig.XP_MULTIPLIER.set(oldMultiplier);}h.succeed();
    }
    @GameTest(template="empty") public static void attributesAndPacketValidation(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);double base=p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ProgressionManager.spend(p,-1);ProgressionManager.spend(p,999);ProgressionManager.spend(p,0);
        h.assertTrue(d.get(RPGAttribute.STRENGTH)==10,"invalid requests and no points rejected");
        d.points=1;ProgressionManager.spend(p,0);near(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),base*1.02,"melee bonus");
        d.set(RPGAttribute.VITALITY,16);d.set(RPGAttribute.ENDURANCE,18);d.set(RPGAttribute.AGILITY,21);
        AttributeManager.apply(p,d);AttributeManager.apply(p,d);
        near(h,p.getMaxHealth(),26,"vitality");near(h,d.maxStamina(),180,"endurance");near(h,p.getAttributeValue(Attributes.MOVEMENT_SPEED),.111,"agility modifier idempotent");
        d.set(RPGAttribute.VITALITY,10);p.setHealth(32);AttributeManager.apply(p,d);h.assertTrue(p.getHealth()<=20,"health clamped");
        d.set(RPGAttribute.STRENGTH,RPGConfig.MAX_ATTRIBUTE.get());d.points=1;ProgressionManager.spend(p,0);h.assertTrue(d.points==1,"stat cap enforced");h.succeed();
    }
    @GameTest(template="empty") public static void miningSuccessAndPlacedBlockProtection(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);var pos=h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_PICKAXE));
        p.gameMode.destroyBlock(pos);h.assertTrue(d.xp>0&&d.stamina<100,"successful mining grants XP and costs stamina");
        long xp=d.xp;h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());PlacedBlockTracker.get(h.getLevel()).mark(pos);
        p.gameMode.destroyBlock(pos);h.assertTrue(d.xp==xp,"placed blocks give no XP");
        p.gameMode.destroyBlock(pos);h.assertTrue(d.xp==xp,"air gives no XP");h.succeed();
    }
    @GameTest(template="empty") public static void mobKillAndFishing(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);var zombie=h.spawnWithNoFreeWill(EntityType.ZOMBIE,1,2,1);
        zombie.hurt(p.damageSources().playerAttack(p),1000);h.assertTrue(d.xp>0,"combat event grants XP");
        long before=d.xp;NeoForge.EVENT_BUS.post(new ItemFishedEvent(List.of(new ItemStack(Items.COD),new ItemStack(Items.BOWL),new ItemStack(Items.NAME_TAG)),1,new net.minecraft.world.entity.projectile.FishingHook(p,p.level(),0,0)));
        h.assertTrue(d.xp-before==36,"fish/junk/treasure categorization");h.succeed();
    }
    @GameTest(template="empty") public static void sprintDistanceAndExclusions(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);p.setOnGround(true);p.setSprinting(true);
        XPManager.running(p,d);
        for(int i=0;i<80;i++){p.setPos(p.getX()+.25,p.getY(),p.getZ());XPManager.running(p,d);}
        h.assertTrue(d.xp==1,"20 actual blocks grant one XP");
        long xp=d.xp;p.getAbilities().flying=true;
        for(int i=0;i<80;i++){p.setPos(p.getX()+.25,p.getY(),p.getZ());XPManager.running(p,d);}
        h.assertTrue(d.xp==xp,"flying excluded");p.getAbilities().flying=false;
        p.setPos(p.getX()+100,p.getY(),p.getZ());XPManager.running(p,d);h.assertTrue(d.xp==xp,"teleport excluded");h.succeed();
    }
    @GameTest(template="empty") public static void staminaAndExhaustion(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);p.setSprinting(true);
        for(int i=0;i<20;i++)StaminaManager.tick(p,d);near(h,d.stamina,96,"sprint cost per second");
        p.setSprinting(false);double before=d.stamina;
        for(int i=0;i<29;i++)StaminaManager.tick(p,d);near(h,d.stamina,before,"regen delay");
        for(int i=0;i<21;i++)StaminaManager.tick(p,d);h.assertTrue(d.stamina>before,"regen");
        StaminaManager.spend(p,d,1000);p.setSprinting(true);StaminaManager.updateExhaustion(p,d);
        h.assertTrue(d.exhausted&&!p.isSprinting(),"exhaustion stops sprint");near(h,p.getAttributeValue(Attributes.ATTACK_SPEED),2,"attack penalty");
        d.stamina=14;StaminaManager.updateExhaustion(p,d);h.assertTrue(d.exhausted,"hysteresis below threshold");
        d.stamina=15;StaminaManager.updateExhaustion(p,d);h.assertFalse(d.exhausted,"recovery threshold");h.succeed();
    }
    @GameTest(template="empty") public static void attackAndMiningSlowdown(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);var cow=h.spawnWithNoFreeWill(EntityType.COW,1,2,1);
        // This test covers the vanilla attack path; the native Epic strike path has its own integration checks.
        if(dev.rpgprogression.compat.EpicFightCompat.enabled())RangedEpicSetup.mode(p,false);
        h.assertFalse(dev.rpgprogression.compat.EpicFightCompat.battle(p),"ordinary attack test uses vanilla mode");
        p.attack(cow);near(h,d.stamina,88,"attack cost");StaminaManager.spend(p,d,1000);
        var e=new PlayerEvent.BreakSpeed(p,Blocks.STONE.defaultBlockState(),10,p.blockPosition());NeoForge.EVENT_BUS.post(e);
        near(h,e.getNewSpeed(),4,"exhausted mining 40 percent");h.succeed();
    }
    @GameTest(template="empty") public static void lockedSlotInteractions(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);h.assertTrue(InventorySlotRules.unlocked(p)==0,"27 main slots locked");
        var chest=ChestMenu.threeRows(1,p.getInventory());p.containerMenu=chest;
        for(int i=0;i<9;i++)p.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
        chest.getSlot(0).set(new ItemStack(Items.DIAMOND));
        chest.clicked(0,0,ClickType.QUICK_MOVE,p);h.assertTrue(chest.getSlot(0).hasItem(),"shift-click cannot use locked slots");
        for(int i=9;i<36;i++)h.assertTrue(p.getInventory().getItem(i).isEmpty(),"no hidden insertion");
        chest.setCarried(new ItemStack(Items.STONE,16));
        for(ClickType type:ClickType.values())chest.clicked(27,0,type,p);
        h.assertTrue(p.getInventory().getItem(9).isEmpty(),"all click types blocked");
        chest.clicked(-999,0,ClickType.QUICK_CRAFT,p);chest.clicked(27,1,ClickType.QUICK_CRAFT,p);chest.clicked(-999,2,ClickType.QUICK_CRAFT,p);
        h.assertTrue(p.getInventory().getItem(9).isEmpty(),"drag blocked");
        h.assertFalse(p.getInventory().add(9,new ItemStack(Items.DIAMOND)),"direct add blocked");
        var wrapper=new PlayerMainInvWrapper(p.getInventory());h.assertFalse(wrapper.insertItem(9,new ItemStack(Items.DIAMOND),false).isEmpty(),"Forge item handler blocked");
        p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,1));
        h.assertTrue(wrapper.extractItem(9,1,false).isEmpty(),"Forge extraction blocked for retained legacy items");
        chest.quickMoveStack(p,0);h.assertTrue(p.getInventory().getItem(9).getCount()==1,"merge into existing locked stack blocked");
        h.assertTrue(p.getInventory().findSlotMatchingUnusedItem(new ItemStack(Items.DIAMOND))==-1,"recipe book cannot consume locked items");
        for(int endurance=10;endurance<=37;endurance++){d.set(RPGAttribute.ENDURANCE,endurance);h.assertTrue(InventorySlotRules.unlocked(p)==endurance-10,"one slot per Endurance: "+endurance);}
        d.set(RPGAttribute.ENDURANCE,100);h.assertTrue(InventorySlotRules.unlocked(p)==27,"inventory cap");h.succeed();
    }
    @GameTest(template="empty") public static void legacyInventoryMigration(GameTestHelper h) {
        var p=player(h);p.getInventory().setItem(9,new ItemStack(Items.DIAMOND,5));InventoryLockManager.sanitize(p);
        h.assertTrue(p.getInventory().getItem(9).isEmpty()&&p.getInventory().getItem(0).getCount()==5,"legacy items relocated");
        for(int i=0;i<9;i++)p.getInventory().setItem(i,new ItemStack(Items.DIRT,64));
        p.getInventory().setItem(10,new ItemStack(Items.EMERALD,7));InventoryLockManager.sanitize(p);
        h.assertTrue(p.getInventory().getItem(10).isEmpty(),"overflow removed only after spawning item");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(3),e->e.getItem().is(Items.EMERALD)&&e.getItem().getCount()==7).isEmpty(),"overflow items preserved in world");h.succeed();
    }
    @GameTest(template="empty") public static void craftingBothGridsAndUnlock(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);
        var small=p.inventoryMenu;small.getSlot(1).set(new ItemStack(Items.ENDER_PEARL));small.getSlot(2).set(new ItemStack(Items.BLAZE_POWDER));small.slotsChanged(p.getInventory());
        h.assertTrue(small.getSlot(0).getItem().isEmpty(),"manual 2x2 gated");h.assertTrue(small.quickMoveStack(p,0).isEmpty(),"2x2 shift-craft gated");
        var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.CRAFTING_TABLE.defaultBlockState());
        var menu=new CraftingMenu(2,p.getInventory(),ContainerLevelAccess.create(h.getLevel(),pos));p.containerMenu=menu;
        for(int i=1;i<=3;i++)menu.getSlot(i).set(new ItemStack(Items.DIAMOND));menu.getSlot(5).set(new ItemStack(Items.STICK));menu.getSlot(8).set(new ItemStack(Items.STICK));
        menu.slotsChanged(p.getInventory());h.assertTrue(menu.getSlot(0).getItem().isEmpty(),"manual 3x3 gated");h.assertTrue(menu.quickMoveStack(p,0).isEmpty(),"3x3 shift-craft gated");
        d.set(RPGAttribute.INTELLIGENCE,20);IntelligenceRecipeManager.updateBook(p,10,true);menu.slotsChanged(p.getInventory());
        h.assertTrue(menu.getSlot(0).getItem().is(Items.DIAMOND_PICKAXE),"recipe unlocked immediately");menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().is(Items.DIAMOND_PICKAXE),"unlocked result can be taken");
        small.slotsChanged(p.getInventory());h.assertTrue(small.getSlot(0).getItem().is(Items.ENDER_EYE),"2x2 unlocked");h.succeed();
    }
    @GameTest(template="empty") public static void saveLoadAndDeathClone(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);d.level=12;d.xp=542;d.points=2;d.set(RPGAttribute.STRENGTH,19);d.stamina=17;d.sprintDistance=8.5;
        d.skillPoints=1;d.skillMilestones=2;d.unlockedSkills.add("rpgprogression:test_skill");
        var n=p.serializeNBT(p.registryAccess());var other=player(h);other.deserializeNBT(other.registryAccess(),n);var loaded=ProgressionManager.get(other);
        h.assertTrue(loaded.level==12&&loaded.xp==542&&loaded.points==2&&loaded.get(RPGAttribute.STRENGTH)==19,"entity capability NBT round trip");near(h,loaded.sprintDistance,8.5,"sprint remainder saved");
        var clone=player(h);CommonEvents.clone(new PlayerEvent.Clone(clone,p,true));var copy=ProgressionManager.get(clone);
        h.assertTrue(copy.level==12&&copy.xp==542&&copy.get(RPGAttribute.STRENGTH)==19,"death clone keeps progress");near(h,copy.stamina,copy.maxStamina(),"respawn stamina restored");
        h.assertTrue(copy.skillPoints==1&&copy.skillMilestones==2&&copy.unlockedSkills.contains("rpgprogression:test_skill"),"death clone keeps learned tree skills and separate points");
        h.succeed();
    }
    @GameTest(template="empty") public static void legacyForgeProgressionMigration(GameTestHelper h) {
        var source=player(h);var original=ProgressionManager.get(source);
        original.level=42;original.xp=12345;original.points=7;original.skillPoints=3;original.skillMilestones=8;
        for(var attribute:RPGAttribute.values())original.set(attribute,20+attribute.ordinal());
        original.unlockedSkills.add("rpgprogression:crawl");original.unlockedSkills.add("parcool:slide");
        original.spark=12.5;original.stamina=37;original.mana=3.75;original.manaCapacity=8;
        original.manaInitialized=true;original.manaActive=true;original.energyFoodTicks=123;
        var legacy=source.serializeNBT(source.registryAccess());
        legacy.remove(net.neoforged.neoforge.attachment.AttachmentHolder.ATTACHMENTS_NBT_KEY);
        var forgeCaps=new net.minecraft.nbt.CompoundTag();forgeCaps.put("rpgprogression:progression",original.save());legacy.put("ForgeCaps",forgeCaps);
        var imported=player(h);imported.removeData(PlayerProgressionProvider.PROGRESSION);
        imported.deserializeNBT(imported.registryAccess(),legacy);var migrated=ProgressionManager.get(imported);
        h.assertTrue(migrated.level==42&&migrated.xp==12345&&migrated.points==7,"legacy Forge level, XP and attribute points imported");
        for(var attribute:RPGAttribute.values())h.assertTrue(migrated.get(attribute)==20+attribute.ordinal(),"legacy attribute imported: "+attribute);
        h.assertTrue(migrated.skillPoints==3&&migrated.skillMilestones==8&&migrated.unlockedSkills.equals(original.unlockedSkills),"legacy skill points and learned skills imported");
        near(h,migrated.spark,12.5,"legacy spark preserved");near(h,migrated.stamina,37,"legacy stamina preserved");
        near(h,migrated.mana,3.75,"legacy mana preserved");near(h,migrated.manaCapacity,8,"legacy mana capacity preserved");
        h.assertTrue(migrated.manaActive&&migrated.manaInitialized&&migrated.energyFoodTicks==123,"legacy resource and food flags preserved");
        var saved=imported.serializeNBT(imported.registryAccess());
        var conflicting=original.save();conflicting.putInt("level",999);conflicting.putLong("xp",987654);
        var staleCaps=new net.minecraft.nbt.CompoundTag();staleCaps.put("rpgprogression:progression",conflicting);saved.put("ForgeCaps",staleCaps);
        var reloaded=player(h);reloaded.removeData(PlayerProgressionProvider.PROGRESSION);
        reloaded.deserializeNBT(reloaded.registryAccess(),saved);var neo=ProgressionManager.get(reloaded);
        h.assertTrue(neo.level==42&&neo.xp==12345&&neo.points==7,"saved NeoForge attachment wins over stale ForgeCaps");
        h.assertTrue(neo.unlockedSkills.equals(original.unlockedSkills)&&neo.get(RPGAttribute.STRENGTH)==20,"NeoForge attributes and skills survive repeat loading");
        near(h,neo.mana,3.75,"NeoForge mana survives repeat loading");
        var endClone=player(h);CommonEvents.clone(new PlayerEvent.Clone(endClone,reloaded,false));var end=ProgressionManager.get(endClone);
        h.assertTrue(end!=neo&&end.level==42&&end.xp==12345&&end.unlockedSkills.equals(neo.unlockedSkills),"End return has an independent progression owner");
        near(h,end.stamina,37,"End return preserves stamina");near(h,end.mana,3.75,"End return preserves mana");
        h.assertTrue(end.energyFoodTicks==123,"End return preserves timed food bonuses");h.succeed();
    }
    @GameTest(template="empty") public static void removedPlayerTicksAndRevival(GameTestHelper h){
        var p=player(h);var d=ProgressionManager.get(p);d.level=12;d.xp=542;d.points=2;d.stamina=37;d.regenCooldown=20;d.spark=8;
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
        var rule=RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH);var previous=rule.get();
        p.remove(Entity.RemovalReason.KILLED);
        h.assertTrue(p.isRemoved(),"removed player is marked before queued actions");
        // The native Player.tick still posts END after removal during the tick (including loading a dead player).
        CommonEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        dev.rpgprogression.network.ModNetworking.spend(p,0);dev.rpgprogression.network.ModNetworking.request(p);
        dev.rpgprogression.network.ModNetworking.sync(p);dev.rpgprogression.network.ModNetworking.stamina(p);dev.rpgprogression.network.ModNetworking.resources(p);
        h.assertTrue(d.points==2&&d.get(RPGAttribute.STRENGTH)==10,"queued messages cannot spend points on a removed player");
        try{
            rule.set(List.of("minecraft:iron_chestplate=100"));
            dev.rpgprogression.requirements.RequirementEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
            h.assertTrue(p.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),"removed player armor is not moved or dropped");
        }finally{rule.set(previous);}
        h.assertTrue(d.stamina==37&&d.regenCooldown==20&&d.spark==8,"removed ticks leave saved resources alone");
        var replacement=player(h);CommonEvents.clone(new PlayerEvent.Clone(replacement,p,true));var copy=ProgressionManager.get(replacement);
        h.assertTrue(copy.level==12&&copy.xp==542&&copy.points==2&&copy.spark==8,"death replacement retains progression and spark");
        near(h,copy.stamina,copy.maxStamina(),"respawn restores stamina");
        h.assertTrue(ProgressionManager.get(p)==d&&ProgressionManager.get(replacement)!=d,"clone retains independent attachment owners");
        CommonEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        replacement.remove(Entity.RemovalReason.KILLED);
        CommonEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(replacement));
        dev.rpgprogression.network.ModNetworking.spend(replacement,0);dev.rpgprogression.network.ModNetworking.request(replacement);
        dev.rpgprogression.network.ModNetworking.sync(replacement);dev.rpgprogression.network.ModNetworking.stamina(replacement);dev.rpgprogression.network.ModNetworking.resources(replacement);
        replacement.revive();h.assertTrue(ProgressionManager.get(replacement)==copy,"revival keeps original data object");
        replacement.setHealth(0);double stamina=copy.stamina;int delay=copy.regenCooldown;
        CommonEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(replacement));
        h.assertTrue(copy.stamina==stamina&&copy.regenCooldown==delay,"dead player tick is ignored before access or synchronization");h.succeed();
    }
    @GameTest(template="empty") public static void bowAndCrossbowUseEvents(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);d.set(RPGAttribute.AGILITY,20);
        p.getInventory().setItem(1,new ItemStack(Items.ARROW,32));
        var bow=new ItemStack(Items.BOW);p.setItemInHand(InteractionHand.MAIN_HAND,bow);p.startUsingItem(InteractionHand.MAIN_HAND);
        for(int i=0;i<16;i++)((TestPlayer)p).useTick();
        h.assertTrue(bow.getUseDuration(p)-p.getUseItemRemainingTicks()==20,"Agility19 fully draws bow in16 ticks");
        p.releaseUsingItem();h.assertTrue(p.getInventory().getItem(1).getCount()==31,"bow released a real arrow");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,p.getBoundingBox().inflate(4),a->a.isCritArrow()&&a.getOwner()==p).isEmpty(),"full-draw critical projectile");
        var crossbow=new ItemStack(Items.CROSSBOW);crossbow.enchant(p.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.QUICK_CHARGE),3);p.setItemInHand(InteractionHand.MAIN_HAND,crossbow);p.startUsingItem(InteractionHand.MAIN_HAND);
        for(int i=0;i<8;i++)((TestPlayer)p).useTick();p.releaseUsingItem();
        h.assertTrue(CrossbowItem.isCharged(crossbow),"QuickCharge3 + Agility19 loads a real crossbow in8 ticks");h.succeed();
    }
    @GameTest(template="empty") public static void defensesAndCreative(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);d.set(RPGAttribute.VITALITY,10000);
        near(h,DefenseManager.percent(d,DefenseType.PHYSICAL),75,"defense cap");
        h.assertTrue(DefenseManager.classify(p.damageSources().onFire())==DefenseType.FIRE,"fire category");
        p.setGameMode(GameType.CREATIVE);StaminaManager.spend(p,d,1000);near(h,d.stamina,100,"creative stamina exemption");
        h.assertTrue(InventorySlotRules.unlocked(p)==27,"creative inventory exemption");ProgressionManager.addXP(p,1000);h.assertTrue(d.level==1&&d.xp==0,"creative XP disabled");h.succeed();
    }
    @GameTest(template="empty") public static void cancelledBreakAndGameModes(GameTestHelper h) {
        var p=player(h);var d=ProgressionManager.get(p);var pos=h.absolutePos(new BlockPos(1,1,1));
        h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> veto=e->{if(e.getPlayer()==p)e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(veto);
        try {p.gameMode.destroyBlock(pos);}finally{NeoForge.EVENT_BUS.unregister(veto);}
        h.assertTrue(d.xp==0&&d.stamina==100&&h.getLevel().getBlockState(pos).is(Blocks.STONE),"cancelled mining gives no XP/cost");
        p.setGameMode(GameType.SPECTATOR);ProgressionManager.addXP(p,810);StaminaManager.spend(p,d,1000);
        h.assertTrue(d.xp==0&&d.stamina==100,"spectator exempt");h.succeed();
    }
    @GameTest(template="empty") public static void optionalParCoolStamina(GameTestHelper h) {
        if(dev.rpgprogression.compat.ParCoolCompat.present())ParCoolGameTests.check(h,player(h));
        else h.assertFalse(dev.rpgprogression.compat.ParCoolCompat.replacesSprint(player(h)),"standalone without ParCool");
        h.succeed();
    }
    @GameTest(template="empty") public static void optionalEpicFightStamina(GameTestHelper h) {
        if(dev.rpgprogression.compat.EpicFightCompat.present())EpicFightGameTests.check(h,player(h));
        else h.assertFalse(dev.rpgprogression.compat.EpicFightCompat.enabled(),"standalone without Epic Fight");
        h.succeed();
    }
    @GameTest(template="empty") public static void optionalEpicFightStaminaSkills(GameTestHelper h){
        if(dev.rpgprogression.compat.EpicFightCompat.enabled())EpicStaminaSkillGameTests.check(h,player(h));
        h.succeed();
    }
    @GameTest(template="empty") public static void itemAttributeRequirements(GameTestHelper h){RequirementGameTests.check(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void sparkTransitionsAndPersistence(GameTestHelper h){ResourceGameTests.check(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void sparkExhaustionLifecycle(GameTestHelper h){SparkEffectGameTests.check(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void configurableXPAndCooking(GameTestHelper h){CookingGameTests.check(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void optionalIronsMana(GameTestHelper h){
        if(dev.rpgprogression.compat.IronsCompat.present())IronsGameTests.check(h,player(h));
        else h.assertFalse(dev.rpgprogression.compat.IronsCompat.enabled(),"mana absent without Iron's");
        h.succeed();
    }
    @GameTest(template="empty") public static void skillPointsAndMigration(GameTestHelper h){SkillTreeGameTests.common(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void optionalParCoolTree(GameTestHelper h){if(dev.rpgprogression.skill.SkillTreeManager.parcool())ParCoolTreeGameTests.check(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void optionalEpicFightTree(GameTestHelper h){if(dev.rpgprogression.skill.SkillTreeManager.epic())EpicTreeGameTests.check(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void magicalDefense(GameTestHelper h){SkillTreeGameTests.magic(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void hungerRemovalAndHealing(GameTestHelper h){FoodGameTests.healing(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void foodBonusesAndExpiry(GameTestHelper h){FoodGameTests.bonuses(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void armorReductionWithoutDoubleApplication(GameTestHelper h){FoodGameTests.armor(h,player(h));h.succeed();}
    @GameTest(template="empty") public static void swimmingStaminaXPAndExclusions(GameTestHelper h){
        var p=(TestPlayer)player(h);var d=ProgressionManager.get(p);
        for(int x=1;x<=3;x++)for(int y=1;y<=4;y++)for(int z=1;z<=3;z++)h.setBlock(new BlockPos(x,y,z),Blocks.WATER);
        p.setPos(h.absolutePos(new BlockPos(2,2,2)).getX()+.5,h.absolutePos(new BlockPos(2,2,2)).getY(),h.absolutePos(new BlockPos(2,2,2)).getZ()+.5);p.sampleWater();
        h.assertTrue(p.isInWater(),"actual world water is detected");p.setOnGround(false);p.setSwimming(true);p.setSprinting(true);
        for(int i=0;i<20;i++)StaminaManager.tick(p,d);near(h,d.stamina,96,"fast vanilla swimming costs the same four stamina per second as sprinting");
        XPManager.running(p,d);double middle=p.getX();
        for(int i=0;i<80;i++){p.setPos(middle+(i%2==0?.25:0),p.getY(),p.getZ());XPManager.running(p,d);}
        h.assertTrue(d.xp==1,"twenty blocks of real fast swimming award one RPG XP");long before=d.xp;
        p.setSprinting(false);for(int i=0;i<80;i++){p.setPos(middle+(i%2==0?.25:0),p.getY(),p.getZ());XPManager.running(p,d);}h.assertTrue(d.xp==before,"slow swimming awards no running XP");
        d.regenCooldown=0;double stamina=d.stamina;StaminaManager.tick(p,d);h.assertTrue(d.stamina>stamina,"resting in water can regenerate stamina");
        p.setSprinting(true);p.setPos(p.getX()+100,p.getY(),p.getZ());XPManager.running(p,d);h.assertTrue(d.xp==before,"water teleport does not award XP");
        d.stamina=.1;StaminaManager.tick(p,d);h.assertTrue(d.exhausted&&!p.isSprinting(),"empty stamina stops fast swimming");
        h.succeed();
    }
    private static final class TestPlayer extends net.neoforged.neoforge.common.util.FakePlayer {
        TestPlayer(net.minecraft.server.level.ServerLevel level){super(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"rpg-test"));}
        void useTick(){updateUsingItem(getUseItem());}
        void sampleWater(){updateInWaterStateAndDoFluidPushing();}
    }
}

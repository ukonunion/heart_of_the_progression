package dev.rpgprogression.verification;

import com.google.gson.JsonArray;
import dev.rpgprogression.attribute.RPGAttribute;
import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.progression.*;
import dev.rpgprogression.requirements.*;
import dev.rpgprogression.resource.SparkManager;
import dev.rpgprogression.skint.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import java.nio.file.*;
import java.util.*;

/** Real config values, casts, travel completion and progression; all temporary changes are restored. */
public final class ConfigurationChecks {
    private static void check(JsonArray checks,boolean ok,String text){SkintTravelChecks.check(checks,ok,text);if(!ok)throw new IllegalStateException(text);}
    private static boolean close(double a,double b){return Math.abs(a-b)<.00001;}
    private static void reset(PlayerProgression d){d.level=1;d.xp=0;d.points=0;d.skillPoints=0;d.skillMilestones=0;d.levelingPending=false;}
    public static void run(ServerPlayer p,JsonArray checks){
        var d=ProgressionManager.get(p);var saved=d.save();var mode=p.gameMode.getGameModeForPlayer();var position=p.position();
        var items=new EnumMap<RPGAttribute,List<? extends String>>(RPGAttribute.class);RPGConfig.ITEM_REQUIREMENTS.forEach((a,c)->items.put(a,List.copyOf(c.get())));
        var spells=new EnumMap<RPGAttribute,List<? extends String>>(RPGAttribute.class);RPGConfig.SPELL_REQUIREMENTS.forEach((a,c)->spells.put(a,List.copyOf(c.get())));
        boolean defaults=RPGConfig.DEFAULT_ITEM_REQUIREMENTS.get();var curve=RPGConfig.XP_CURVE.get();var formula=RPGConfig.XP_FORMULA.get();
        double base=RPGConfig.BASE_XP.get(),multiplier=RPGConfig.XP_MULTIPLIER.get(),cost=RPGConfig.SKINT_TRAVEL_COST.get(),dimension=RPGConfig.SPARK_TRAVEL_COST.get();int perMana=RPGConfig.SPELL_INTELLIGENCE_PER_MANA.get();
        try{
            p.setGameMode(GameType.SURVIVAL);d.initialize();RPGConfig.ITEM_REQUIREMENTS.values().forEach(c->c.set(List.of()));
            var sword=new ItemStack(Items.DIAMOND_SWORD);
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.ENDURANCE).set(List.of("minecraft:diamond_sword=12"));
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of("minecraft:diamond_sword=18","minecraft:bow=13"));
            check(checks,ItemRequirements.required(sword).equals(Map.of(RPGAttribute.ENDURANCE,12,RPGAttribute.STRENGTH,18)),"exact config entries lower a built-in requirement and add a second attribute");
            d.set(RPGAttribute.ENDURANCE,12);d.set(RPGAttribute.STRENGTH,17);
            check(checks,!ItemRequirements.allowed(p,sword)&&ItemRequirements.unmet(p,sword).equals(Map.of(RPGAttribute.STRENGTH,18)),"configured thresholds control real authorization and hide the fulfilled attribute");
            d.set(RPGAttribute.STRENGTH,18);check(checks,ItemRequirements.allowed(p,sword)&&ItemRequirements.unmet(p,sword).isEmpty(),"the custom item unlocks exactly at both configured thresholds");
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.ENDURANCE).set(List.of("#minecraft:swords=7"));
            check(checks,ItemRequirements.required(sword).get(RPGAttribute.ENDURANCE)==7,"a tag config rule can also lower automatic material requirements");
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.ENDURANCE).set(List.of("#minecraft:swords=7","minecraft:diamond_sword=9"));
            check(checks,ItemRequirements.required(sword).get(RPGAttribute.ENDURANCE)==9,"the greatest matching configured item/tag value wins");
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.ENDURANCE).set(List.of("minecraft:diamond_sword=0"));
            check(checks,!ItemRequirements.required(sword).containsKey(RPGAttribute.ENDURANCE),"zero explicitly removes an automatic item requirement");
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.VITALITY).set(List.of("minecraft:diamond_chestplate=30","uninstalled_mod:future_item=25"));
            check(checks,ItemRequirements.required(new ItemStack(Items.DIAMOND_CHESTPLATE)).get(RPGAttribute.VITALITY)==30&&ItemRequirements.required(new ItemStack(Items.BOW)).get(RPGAttribute.STRENGTH)==13,"new requirements apply to armor and previously unrestricted items; absent optional IDs are harmless");
            RPGConfig.ITEM_REQUIREMENTS.get(RPGAttribute.INTELLIGENCE).set(List.of("minecraft:brewing_stand=18"));
            check(checks,ItemRequirements.required(new ItemStack(Items.BREWING_STAND)).get(RPGAttribute.INTELLIGENCE)==18,"a workstation item and its interaction rule use the configured threshold");
            RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(false);
            check(checks,ItemRequirements.required(new ItemStack(Items.IRON_PICKAXE)).isEmpty()&&ItemRequirements.required(new ItemStack(Items.BOW)).get(RPGAttribute.STRENGTH)==13,"automatic requirements can be disabled while keeping custom item rules");
            RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(true);items.forEach((a,v)->RPGConfig.ITEM_REQUIREMENTS.get(a).set(v));

            if(dev.rpgprogression.compat.IronsCompat.present())irons(p,checks);
            var a=new BlockPos(105,180,20);var b=a.offset(12,0,0);SkintTravelChecks.point(p.serverLevel(),a);SkintTravelChecks.point(p.serverLevel(),b);
            var destination=SkintNetwork.get(p.serverLevel()).ensure(p.serverLevel(),b);d.discoveredSkints.add(destination.id());
            RPGConfig.SPARK_TRAVEL_COST.set(17.25);
            for(double price:new double[]{7.5,2.25,0}){
                SkintManager.close(p,false);p.setPos(a.getX()+.5,a.getY(),a.getZ()+2.5);SkintManager.use(p,a);RPGConfig.SKINT_TRAVEL_COST.set(price);
                check(checks,close(SparkManager.skintTravelCost(),price)&&SparkManager.skintTravelCostText().equals(java.math.BigDecimal.valueOf(price).stripTrailingZeros().toPlainString()),"configured skint price and displayed decimal agree: "+price);
                if(price>0){d.spark=price-.01;check(checks,!SkintManager.travel(p,destination.id())&&!SkintManager.pending(p),"insufficient spark rejects the configured route before standing: "+price);}
                d.spark=20;var token=SkintManager.session(p).token();
                check(checks,SkintManager.travel(p,destination.id()),"a discovered route accepts the configured skint price: "+price);
                SkintManager.finishedStanding(p,(SkintSeat)p.getVehicle());SkintManager.action(p,4,token,new UUID(0,0));
                check(checks,close(d.spark,20-price)&&SkintManager.canProgress(p)&&SkintManager.session(p).origin().id().equals(destination.id()),"actual completed skint travel pays only its configured price and opens destination access: "+price);
            }
            SkintManager.close(p,false);p.setPos(a.getX()+.5,a.getY(),a.getZ()+2.5);SkintManager.use(p,a);RPGConfig.SKINT_TRAVEL_COST.set(5.0);d.spark=10;
            var token=SkintManager.session(p).token();check(checks,SkintManager.travel(p,destination.id()),"the price-reload scenario starts with affordable spark");RPGConfig.SKINT_TRAVEL_COST.set(12.0);
            SkintManager.finishedStanding(p,(SkintSeat)p.getVehicle());SkintManager.action(p,4,token,new UUID(0,0));
            check(checks,d.spark==10&&!SkintManager.pending(p)&&SkintManager.canProgress(p)&&SkintManager.session(p).origin().pos().equals(a),"a price increase during departure is revalidated and cancels without payment");
            SkintManager.close(p,false);d.spark=20;SparkManager.charge(p);check(checks,close(d.spark,2.75),"ordinary dimension travel uses its separate configurable 17.25 price");
            RPGConfig.SKINT_TRAVEL_COST.set(5.0);check(checks,SparkManager.skintTravelCostText().equals("5"),"the default price remains a clean integer in the UI");

            RPGConfig.XP_CURVE.set(ProgressionMath.Curve.CUSTOM);RPGConfig.XP_FORMULA.set("base + (level - 1) * 125");RPGConfig.BASE_XP.set(250.0);reset(d);
            check(checks,d.requiredXP()==250,"the custom XP curve uses the configured base at level one");
            ProgressionManager.addXPAdministrative(p,1125);
            check(checks,d.level==4&&d.xp==0&&d.points==3&&d.requiredXP()==625,"real leveling consumes custom costs 250,375,500 and awards the expected levels and points");
            RPGConfig.BASE_XP.set(100.0);check(checks,d.requiredXP()==475,"changing baseLevelXP immediately affects the custom formula");
            RPGConfig.XP_FORMULA.set("base * level ^ 2");check(checks,d.requiredXP()==1600,"changing the formula invalidates its cached arithmetic expression");
            RPGConfig.XP_FORMULA.set("base");RPGConfig.BASE_XP.set(1.0);d.level=Integer.MAX_VALUE-2;d.xp=0;d.points=0;
            ProgressionManager.addXPAdministrative(p,ProgressionMath.XP_LIMIT);
            check(checks,d.level==Integer.MAX_VALUE&&d.points==2&&d.xp==ProgressionMath.XP_LIMIT-2,"a constant custom formula batches a huge award safely without losing XP");
            RPGConfig.XP_FORMULA.set("1 + level % 2");reset(d);ProgressionManager.addXPAdministrative(p,6000);
            check(checks,d.levelingPending&&d.level==1025&&d.xp==4464,"a large award on an arbitrary custom curve is bounded per processing pass");
            var savedPending=d.save();var copy=new PlayerProgression();copy.load(savedPending);
            check(checks,copy.level==d.level&&copy.xp==d.xp,"unfinished custom leveling preserves all level and XP progress in NBT");
            for(int i=0;i<8&&d.levelingPending;i++)dev.rpgprogression.event.CommonEvents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            check(checks,d.level==4001&&d.points==4000&&d.xp==0&&!d.levelingPending,"following native player ticks finish arbitrary-curve leveling with exact carry and points");
            RPGConfig.XP_CURVE.set(ProgressionMath.Curve.STAGED);RPGConfig.BASE_XP.set(500.0);reset(d);
            ProgressionManager.addXPAdministrative(p,1137615);check(checks,d.level==100&&d.xp==0&&d.points==99,"the previous staged progression to level 100 is unchanged");
            templates(checks);
        }catch(java.io.IOException error){throw new RuntimeException(error);}
        finally{
            SkintManager.close(p,false);items.forEach((a,v)->RPGConfig.ITEM_REQUIREMENTS.get(a).set(v));spells.forEach((a,v)->RPGConfig.SPELL_REQUIREMENTS.get(a).set(v));
            RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(defaults);RPGConfig.SPELL_INTELLIGENCE_PER_MANA.set(perMana);RPGConfig.BASE_XP.set(base);RPGConfig.XP_MULTIPLIER.set(multiplier);RPGConfig.XP_CURVE.set(curve);RPGConfig.XP_FORMULA.set(formula);
            RPGConfig.SKINT_TRAVEL_COST.set(cost);RPGConfig.SPARK_TRAVEL_COST.set(dimension);d.load(saved);p.removeAllEffects();p.setPos(position);p.setGameMode(mode);
        }
    }
    private static void irons(ServerPlayer p,JsonArray checks){
        var spell=io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();String id=spell.getSpellId().toString();
        RPGConfig.SPELL_INTELLIGENCE_PER_MANA.set(3);RPGConfig.SPELL_REQUIREMENTS.get(RPGAttribute.INTELLIGENCE).set(List.of(id+"=18"));RPGConfig.SPELL_REQUIREMENTS.get(RPGAttribute.STRENGTH).set(List.of(id+"=12"));
        check(checks,dev.rpgprogression.compat.IronsRequirements.spellRequired(spell,1).equals(Map.of(RPGAttribute.INTELLIGENCE,18,RPGAttribute.STRENGTH,12)),"configured per-spell rules replace intelligence and add a second attribute");
        var d=ProgressionManager.get(p);d.set(RPGAttribute.INTELLIGENCE,18);d.set(RPGAttribute.STRENGTH,11);
        check(checks,!dev.rpgprogression.compat.IronsRequirements.spellAllowed(p,spell,1,io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK),"book spells reject the configured second attribute below its threshold");
        d.set(RPGAttribute.STRENGTH,12);check(checks,dev.rpgprogression.compat.IronsRequirements.spellAllowed(p,spell,1,io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK),"book spells accept all configured attributes exactly at their thresholds");
        RPGConfig.SPELL_REQUIREMENTS.get(RPGAttribute.INTELLIGENCE).set(List.of(id+"=0"));check(checks,!dev.rpgprogression.compat.IronsRequirements.spellRequired(spell,1).containsKey(RPGAttribute.INTELLIGENCE),"zero removes automatic intelligence for one spell");
        var scroll=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("irons_spellbooks:scroll")));
        io.redspace.ironsspellbooks.api.spells.ISpellContainer.createScrollContainer(spell,1,scroll);
        check(checks,ItemRequirements.required(scroll).get(RPGAttribute.STRENGTH)==12,"scroll item tooltips and book casting use the same configurable spell rules");
        RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(false);
        check(checks,ItemRequirements.required(scroll).get(RPGAttribute.STRENGTH)==12,"disabling automatic item requirements keeps separately configured spell requirements visible on scrolls");
        RPGConfig.DEFAULT_ITEM_REQUIREMENTS.set(true);
        RPGConfig.SPELL_REQUIREMENTS.values().forEach(c->c.set(List.of()));RPGConfig.SPELL_INTELLIGENCE_PER_MANA.set(0);
        check(checks,dev.rpgprogression.compat.IronsRequirements.spellRequired(spell,1).isEmpty(),"zero intelligence per mana disables the automatic spell threshold");RPGConfig.SPELL_INTELLIGENCE_PER_MANA.set(5);
    }
    private static void templates(JsonArray checks)throws java.io.IOException{
        var defaults=com.electronwill.nightconfig.core.CommentedConfig.inMemory();RPGConfig.SERVER.correct(defaults);defaults.set("general.progressionRulesVersion",11);
        var writer=new com.electronwill.nightconfig.toml.TomlWriter();var parser=new com.electronwill.nightconfig.toml.TomlParser();var directory=Path.of("verification/config-2.3.14");Files.createDirectories(directory);
        String text=writer.writeToString(defaults);check(checks,RPGConfig.SERVER.isCorrect(parser.parse(text)),"the complete default TOML template round-trips through the real server config schema");Files.writeString(directory.resolve("rpgprogression-server-default.toml"),text);
        var custom=parser.parse(text);custom.set("spark.skintTravelCost",7.5);custom.set("spark.dimensionTravelCost",20.0);custom.set("experience.baseLevelXP",250.0);custom.set("experience.levelXPCurve","CUSTOM");custom.set("experience.customLevelXPFormula","base + (level - 1) * 125");
        custom.set("itemRequirements.strength",List.of("minecraft:diamond_sword=18","minecraft:bow=13"));custom.set("itemRequirements.endurance",List.of("minecraft:diamond_sword=12","minecraft:netherite_sword=0"));custom.set("itemRequirements.intelligence",List.of("minecraft:brewing_stand=18"));
        custom.set("spellRequirements.intelligence",List.of("irons_spellbooks:fireball=18"));custom.set("spellRequirements.strength",List.of("irons_spellbooks:fireball=12"));
        text=writer.writeToString(custom);check(checks,RPGConfig.SERVER.isCorrect(parser.parse(text)),"the editable custom TOML example validates, including formula, item/spell rules and fractional travel price");Files.writeString(directory.resolve("rpgprogression-server-custom-example.toml"),text);
    }
    public static ServerPlayer player(net.minecraft.server.level.ServerLevel level){
        var profile=new com.mojang.authlib.GameProfile(UUID.fromString("c7a7372e-e00f-4159-855c-bad230014000"),"ConfigVerifier");
        var player=new ServerPlayer(level.getServer(),level,profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public void setListenerForServerboundHandshake(net.minecraft.network.PacketListener listener){}};
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),connection,player,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
        player.setPos(100,180,20);player.setNoGravity(true);return player;
    }
    public static void loadedOverride(net.minecraft.server.level.ServerLevel level,JsonArray checks){
        check(checks,RPGConfig.XP_CURVE.get()==ProgressionMath.Curve.CUSTOM&&RPGConfig.BASE_XP.get()==250&&RPGConfig.XP_FORMULA.get().equals("base + (level - 1) * 125"),"a real world/serverconfig TOML override loads the custom XP mode, formula and base after restart");
        check(checks,RPGConfig.RULES_VERSION.get()==11&&RPGConfig.XP_MULTIPLIER.get()==1.2,"legacy config migration preserves an explicitly selected CUSTOM formula even with a custom exponential multiplier");
        check(checks,SparkManager.skintTravelCost()==7.5&&RPGConfig.SPARK_TRAVEL_COST.get()==20,"the real TOML override loads separate fractional skint and dimension prices");
        check(checks,ItemRequirements.required(new ItemStack(Items.DIAMOND_SWORD)).equals(Map.of(RPGAttribute.STRENGTH,18,RPGAttribute.ENDURANCE,12)),"the real TOML override loads all item attribute lists");
        var player=player(level);var d=ProgressionManager.get(player);d.initialize();ProgressionManager.addXPAdministrative(player,1125);
        check(checks,d.level==4&&d.xp==0&&d.points==3&&d.requiredXP()==625,"the formula read from the actual TOML file governs native level gains");
        var config=net.neoforged.fml.config.ModConfigs.getFileMap().get("rpgprogression-server.toml");
        check(checks,config!=null&&config.getLoadedConfig().config().get("spark.skintTravelCost").equals(7.5),"the loaded config object exposes the same price that server checks and client rule sync use");
    }
    private ConfigurationChecks(){}
}

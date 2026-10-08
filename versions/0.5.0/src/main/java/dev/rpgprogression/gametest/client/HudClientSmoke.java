package dev.rpgprogression.gametest.client;

import dev.rpgprogression.client.hud.*;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.ProgressionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.*;
import java.util.function.BiConsumer;

/** Real potion synchronization and framebuffer checks in the isolated smoke world. */
public final class HudClientSmoke {
    private static final String[] cases={"normal-half","normal-full","normal-empty","absorption","poison","wither","frozen","hunger-effect","overlapping-effects","normal-hardcore","absorption-hardcore","poison-hardcore","wither-hardcore","frozen-hardcore","effects-cleared"};
    public static void tick(int tick,Minecraft mc,BiConsumer<Boolean,String> check){
        if(tick<670)return;
        int stage=(tick-670)/35,offset=(tick-670)%35;
        if(stage>=cases.length){hardcore(mc,false,check);ClientSmokeTest.finish(mc);return;}
        String name=cases[stage];boolean hc=name.endsWith("hardcore");
        if(offset==0){
            hardcore(mc,hc,check);
            mc.getSingleplayerServer().execute(()->{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var d=ProgressionManager.get(p);
                p.removeAllEffects();p.setTicksFrozen(0);p.setAbsorptionAmount(0);p.setHealth(p.getMaxHealth()/2);p.getFoodData().setFoodLevel(10);p.getFoodData().setSaturation(0);
                d.stamina=d.maxStamina()/2;d.exhausted=false;d.regenCooldown=20000;d.spark=d.maxSpark();d.mana=d.manaCapacity;
                if(name.equals("normal-full")){p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);d.stamina=d.maxStamina();}
                if(name.equals("normal-empty")){p.setHealth(1);p.getFoodData().setFoodLevel(0);d.stamina=0;}
                if(name.startsWith("absorption")||name.equals("overlapping-effects"))p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,20000,0));
                if(name.startsWith("poison")||name.equals("overlapping-effects"))p.addEffect(new MobEffectInstance(MobEffects.POISON,20000,0));
                if(name.startsWith("wither")||name.equals("overlapping-effects"))p.addEffect(new MobEffectInstance(MobEffects.WITHER,20000,0));
                if(name.startsWith("frozen"))p.setTicksFrozen(1000);
                if(name.equals("hunger-effect"))p.addEffect(new MobEffectInstance(MobEffects.HUNGER,20000,0));
                ModNetworking.stamina(p);ModNetworking.resources(p);
            });
        }
        if(offset==20){
            HealthStyle expected=name.startsWith("wither")||name.equals("overlapping-effects")?HealthStyle.WITHER:name.startsWith("poison")?HealthStyle.POISON:name.startsWith("frozen")?HealthStyle.FROZEN:name.startsWith("absorption")?HealthStyle.ABSORPTION:HealthStyle.NORMAL;
            check.accept(CenteredHud.healthStyle()==expected,"exclusive health skin: "+name);
            check.accept(CenteredHud.hungerEffect()==name.equals("hunger-effect"),"exclusive food skin: "+name);
            check.accept(mc.level.getLevelData().isHardcore()==hc,"hardcore skin selection: "+name);
            ClientSmokeTest.captureHud("hud-"+name+".png");
            com.mojang.logging.LogUtils.getLogger().info("RPG_HUD_METRICS {} gui={}x{} scale={}",name,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),mc.getWindow().getGuiScale());
        }
    }
    private static void hardcore(Minecraft mc,boolean value,BiConsumer<Boolean,String> check){
        try{var data=mc.level.getLevelData();var field=data.getClass().getDeclaredField("hardcore");field.setAccessible(true);field.setBoolean(data,value);}
        catch(ReflectiveOperationException e){check.accept(false,"temporary client hardcore fixture: "+e);}
    }
    private HudClientSmoke(){}
}

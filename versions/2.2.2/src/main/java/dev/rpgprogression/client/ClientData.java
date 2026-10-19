package dev.rpgprogression.client;

import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class ClientData {
    private static final PlayerProgression EMPTY=new PlayerProgression();
    private static net.minecraft.nbt.CompoundTag pending;
    public static PlayerProgression get() {
        var p=Minecraft.getInstance().player;
        return p==null?EMPTY:PlayerProgressionProvider.get(p).orElse(EMPTY);
    }
    public static void install() {
        ModNetworking.clientSync=p->{pending=p.data();applyPending();};
        ModNetworking.clientStamina=p->{get().stamina=p.current();get().exhausted=p.exhausted();};
        ModNetworking.clientResources=p->{var d=get();d.spark=p.spark();d.mana=p.mana();d.manaCapacity=p.manaCapacity();d.manaActive=p.manaActive();};
        ModNetworking.clientTree=p->ClientSkillTree.load(p.data());
        ModNetworking.clientLevelUp=p->{
            Minecraft mc=Minecraft.getInstance();
            dev.rpgprogression.client.hud.LevelUpHud.show();
            if(mc.player!=null&&p.points()>0)mc.player.displayClientMessage(Component.translatable("rpgprogression.points_available",p.points()).withStyle(ChatFormatting.YELLOW),false);
        };
        ModNetworking.clientRecipe=p->{var player=Minecraft.getInstance().player;if(player!=null)player.displayClientMessage(Component.translatable("rpgprogression.recipe_unlocked",p.name()).withStyle(ChatFormatting.GOLD),false);};
        ModNetworking.clientRules=p->{
            if(!Minecraft.getInstance().hasSingleplayerServer()) {
                var config=net.neoforged.fml.config.ModConfigs.getFileMap().get("rpgprogression-server.toml");
                if(config!=null)net.neoforged.fml.config.ConfigTracker.acceptSyncedConfig(config,p.toml().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        };
    }
    public static void applyPending() {
        if(pending!=null&&Minecraft.getInstance().player!=null){get().load(pending);pending=null;}
    }
    public static void reset(){pending=null;ClientSkillTree.reset();dev.rpgprogression.client.gui.SkillTreeScreen.resetView();dev.rpgprogression.client.hud.LevelUpHud.reset();}
    private ClientData() {}
}

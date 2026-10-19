package dev.rpgprogression.client;

import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.*;
import net.minecraft.client.Minecraft;

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
        ModNetworking.clientLevelUp=p->dev.rpgprogression.client.hud.LevelUpHud.show();
        ModNetworking.clientSkintCharged=p->dev.rpgprogression.client.hud.SkintChargedHud.show();
        ModNetworking.clientRecipe=p->{}; // Ignore legacy recipe notification payloads, including chat.
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
    public static void reset(){pending=null;ClientSkillTree.reset();ClientSkintData.clear();dev.rpgprogression.client.gui.SkillTreeScreen.resetView();dev.rpgprogression.client.hud.LevelUpHud.reset();dev.rpgprogression.client.hud.SkintChargedHud.reset();dev.rpgprogression.client.hud.ProgressionExperienceHud.reset();}
    private ClientData() {}
}

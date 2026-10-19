package dev.rpgprogression;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.PlayerProgression;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RPGProgression.MOD_ID)
public final class RPGProgression {
    public static final String MOD_ID="rpgprogression";
    public RPGProgression() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,RPGConfig.SERVER);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT,RPGConfig.CLIENT);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerCaps);
        dev.rpgprogression.effect.ModEffects.EFFECTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModNetworking.register();
    }
    private void registerCaps(RegisterCapabilitiesEvent e) { e.register(PlayerProgression.class); }
}

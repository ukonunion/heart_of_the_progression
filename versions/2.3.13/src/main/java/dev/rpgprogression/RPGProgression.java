package dev.rpgprogression;

import dev.rpgprogression.config.RPGConfig;
import dev.rpgprogression.network.ModNetworking;
import dev.rpgprogression.progression.PlayerProgressionProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(RPGProgression.MOD_ID)
public final class RPGProgression {
    public static final String MOD_ID="rpgprogression";
    public RPGProgression(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER,RPGConfig.SERVER);
        container.registerConfig(ModConfig.Type.CLIENT,RPGConfig.CLIENT);
        PlayerProgressionProvider.ATTACHMENTS.register(modBus);
        dev.rpgprogression.effect.ModEffects.EFFECTS.register(modBus);
        dev.rpgprogression.particle.ModParticles.PARTICLES.register(modBus);
        dev.rpgprogression.block.ModBlocks.register(modBus);
        dev.rpgprogression.skint.SkintSeating.register(modBus);
        modBus.addListener(ModNetworking::register);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            if(dev.rpgprogression.compat.ParCoolCompat.present())dev.rpgprogression.compat.ParCoolActionGuards.register();
            if(dev.rpgprogression.compat.IronsCompat.present())dev.rpgprogression.compat.CurioRequirementGuards.register();
        }));
    }
}

package dev.rpgprogression.particle;

import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.*;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES=DeferredRegister.create(Registries.PARTICLE_TYPE,"rpgprogression");
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> SKINT_CHARGED=PARTICLES.register("skint_charged",()->new SimpleParticleType(false));
    private ModParticles(){}
}

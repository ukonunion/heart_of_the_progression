package dev.rpgprogression.mixin.clavis;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.*;

@Pseudo
@Mixin(targets="it.hurts.octostudios.octolib.client.particle.ParticleSystem",remap=false)
public interface ClavisParticleAccess {
    @Accessor("SCREEN_PARTICLES") static Map<Screen,List<Object>> rpgprogression$particles(){throw new AssertionError();}
}

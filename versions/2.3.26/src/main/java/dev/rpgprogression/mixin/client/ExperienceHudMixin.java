package dev.rpgprogression.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Allows the actual vanilla XP bar to appear during mounted XP changes. */
@Mixin(Gui.class)
public interface ExperienceHudMixin {
    @Invoker("renderExperienceBar") void rpg$renderExperienceBar(GuiGraphics graphics,int x);
}

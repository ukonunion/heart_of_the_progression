package dev.rpgprogression.gametest.client;
import net.minecraft.client.Minecraft;
import com.alrex.parcool.common.Parkourability;
public final class ParCoolTreeClientCheck {
    public static void open(Minecraft mc){var p=Parkourability.get(mc.player);mc.setScreen(new com.alrex.parcool.client.gui.screen.ParCoolSkillTreeScreen(p.getCapabilities(),p.getEnabledActions(),p.getSkillTrees()));}
    private ParCoolTreeClientCheck(){}
}

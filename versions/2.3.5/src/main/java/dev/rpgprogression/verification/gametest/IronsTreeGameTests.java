package dev.rpgprogression.verification.gametest;
import dev.rpgprogression.defense.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
public final class IronsTreeGameTests {
    public static void magic(GameTestHelper h,ServerPlayer p){
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("irons_spellbooks","fire_magic"));
        var source=new net.minecraft.world.damagesource.DamageSource(p.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key),p);
        h.assertTrue(DefenseManager.classify(source)==DefenseType.MAGICAL,"Iron's tagged spell uses magical defense");
    }
    private IronsTreeGameTests(){}
}

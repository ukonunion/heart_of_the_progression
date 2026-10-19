package dev.rpgprogression.mixin.client;

import net.minecraft.client.gui.components.toasts.*;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recipes still unlock normally; only their discovery notification is disabled. */
@Mixin(RecipeToast.class)
public abstract class RecipeToastMixin {
    @Inject(method="addOrUpdate",at=@At("HEAD"),cancellable=true)
    private static void rpgprogression$noRecipePopup(ToastComponent toasts,RecipeHolder<?> recipe,CallbackInfo ci){ci.cancel();}
}

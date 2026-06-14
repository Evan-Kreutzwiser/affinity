package io.wispforest.affinity.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.affinity.object.AffinityItems;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFrameEntityRenderer.class)
public class ItemFrameEntityRendererMixin {

    @ModifyExpressionValue(method = "getModelId", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
    private boolean weHaveCorrectModels(boolean original, @Local(argsOnly = true) ItemStack stack) {
        if (!stack.isOf(AffinityItems.REALIZED_AETHUM_MAP)) return original;
        return true;
    }

}

package io.wispforest.affinity.mixin.client;

import io.wispforest.affinity.item.ArtifactBladeItem;
import io.wispforest.affinity.misc.MixinHooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    @Shadow
    public abstract void fill(RenderLayer layer, int x1, int y1, int x2, int y2, int color);

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void injectSecondaryItemBar(TextRenderer textRenderer, ItemStack stack, int x, int y, String countOverride, CallbackInfo ci) {
        var barHeight = y + 13;
        if (stack.isItemBarVisible()) {
            barHeight = y + 11;
        }

        this.affinity$renderSecondaryBar(x + 2, barHeight, stack);
    }

    @Unique
    private void affinity$renderSecondaryBar(int x, int y, ItemStack stack) {
        if (!(stack.getItem() instanceof ArtifactBladeItem blade)) return;

        int abilityTicks = ArtifactBladeItem.getAbilityTicks(MinecraftClient.getInstance().world, stack);
        if (abilityTicks < 0) return;

        int progress = 13 - Math.round((abilityTicks / (float) blade.abilityDuration()) * 13);
        int color = 0xFF0096FF;

        this.fill(RenderLayer.getGuiOverlay(), x, y, x + 13, y + 2, 0xFF000000);
        this.fill(RenderLayer.getGuiOverlay(), x, y, x + progress, y + 1, color);
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", at = @At("HEAD"), cancellable = true)
    private void captureTooltips(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo ci) {
        if (MixinHooks.tooltipConsumer == null) return;

        MixinHooks.tooltipConsumer.accept(components);
        ci.cancel();
    }
}

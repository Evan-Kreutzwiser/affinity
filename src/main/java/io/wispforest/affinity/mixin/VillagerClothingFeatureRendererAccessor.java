package io.wispforest.affinity.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VillagerClothingFeatureRenderer.class)
public class VillagerClothingFeatureRendererAccessor {
    // Intentionally does not follow accessor pattern for the sake of compatability
    // with sinytra connector / neoforge environments, where the field goes by a different name.
    // This format automatically remaps.
    @Final
    @Shadow
    public static Int2ObjectMap<Identifier> LEVEL_TO_ID;
}

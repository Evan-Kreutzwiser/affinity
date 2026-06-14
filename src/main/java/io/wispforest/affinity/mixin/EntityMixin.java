package io.wispforest.affinity.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.wispforest.affinity.Affinity;
import io.wispforest.affinity.component.AffinityComponents;
import io.wispforest.affinity.item.ArtifactBladeItem;
import io.wispforest.affinity.misc.ArcaneFadeFluid;
import io.wispforest.affinity.misc.EntityReference;
import io.wispforest.affinity.misc.quack.AffinityEntityAddon;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(Entity.class)
public abstract class EntityMixin implements AffinityEntityAddon {

    @Shadow
    public abstract Box getBoundingBox();

    @Shadow
    public abstract boolean isRegionUnloaded();

    @Shadow
    private World world;

    @Shadow
    public abstract void setVelocity(Vec3d velocity);

    @Unique private static final boolean SINYTRA_CONNECTOR = FabricLoader.getInstance().isModLoaded("connector");

    @Unique
    private static final TagKey<Fluid> ARCANE_FADE = TagKey.of(RegistryKeys.FLUID, Affinity.id("arcane_fade"));

    @Unique
    private Map<DataKey<?>, Object> affinity$dataStorage = null;

    @Unique
    private boolean affinity$touchingBleach = false;

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void hookRemove(CallbackInfo ci) {
        EntityReference.dropAll((Entity) (Object) this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getData(DataKey<V> key) {
        final var data = this.affinity$getStorage().get(key);
        return data != null
                ? (V) data
                : key.makeDefaultValue();
    }

    @Override
    public <V> void setData(DataKey<V> key, V value) {
        affinity$getStorage().put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V removeData(DataKey<V> key) {
        return hasData(key) ? (V) affinity$dataStorage.remove(key) : null;
    }

    @Override
    public <V> boolean hasData(DataKey<V> key) {
        return affinity$dataStorage != null && affinity$dataStorage.containsKey(key);
    }

    @Unique
    private Map<DataKey<?>, Object> affinity$getStorage() {
        if (this.affinity$dataStorage == null) this.affinity$dataStorage = new HashMap<>();
        return this.affinity$dataStorage;
    }

    @Inject(method = "getJumpVelocityMultiplier", at = @At("TAIL"), cancellable = true)
    protected void lessJumping(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof PlayerEntity player)) return;

        var weapon = player.getMainHandStack();
        if (!(weapon.getItem() instanceof ArtifactBladeItem blade) || ArtifactBladeItem.getAbilityTicks(player.getWorld(), weapon) < 0 || blade.tier.ordinal() < 2) {
            return;
        }

        cir.setReturnValue(cir.getReturnValueF() * 2.5f);
    }

    // Replicate vanilla fluid collision mechanic for use under unreliable sinytra connector environments
    @Unique
    public boolean affinity$vanillaUpdateFluidHeight(TagKey<Fluid> fluidTag) {
        if (this.isRegionUnloaded()) {
            return false;
        } else {
            Box aabb = this.getBoundingBox().contract(0.001);
            int i = MathHelper.floor(aabb.minX);
            int j = MathHelper.ceil(aabb.maxX);
            int k = MathHelper.floor(aabb.minY);
            int l = MathHelper.ceil(aabb.maxY);
            int i1 = MathHelper.floor(aabb.minZ);
            int j1 = MathHelper.ceil(aabb.maxZ);
            BlockPos.Mutable pos = new BlockPos.Mutable();

            for (int l1 = i; l1 < j; l1++) {
                for (int i2 = k; i2 < l; i2++) {
                    for (int j2 = i1; j2 < j1; j2++) {
                        pos.set(l1, i2, j2);
                        FluidState fluidstate = this.world.getFluidState(pos);
                        if (fluidstate.isIn(fluidTag)) {
                            double d1 = (float)i2 + fluidstate.getHeight(this.world, pos);
                            if (d1 >= aabb.minY) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    @WrapOperation(
            method = "checkWaterState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;updateMovementInFluid(Lnet/minecraft/registry/tag/TagKey;D)Z"
            )
    )
    private boolean checkFadeState(Entity instance, TagKey<Fluid> tag, double speed, Operation<Boolean> original) {
        boolean vanillaWater = original.call(instance, tag, speed);
        boolean wasTouchingFade = this.affinity$touchingBleach;
        if (!SINYTRA_CONNECTOR) {
            this.affinity$touchingBleach = instance.updateMovementInFluid(ARCANE_FADE, 0.014);
        } else {
            // Under neoforge with sinytra connector, updateMovementInFluid doesn't work with tags
            // and we have to duplicate the collision logic ourselves.
            this.affinity$touchingBleach = affinity$vanillaUpdateFluidHeight(ARCANE_FADE);
        }

        if (this.affinity$touchingBleach && !wasTouchingFade) {
            ArcaneFadeFluid.ENTITY_TOUCH_EVENT.invoker().onTouch((Entity) (Object) this);
        }

        return vanillaWater || this.affinity$touchingBleach;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    protected void invokeFadeTickEvent(CallbackInfo ci) {
        if (!this.affinity$touchingBleach) return;
        ArcaneFadeFluid.ENTITY_TICK_IN_FADE_EVENT.invoker().onTouch((Entity) (Object) this);
    }

    @Inject(method = "move", at = @At("HEAD"))
    protected void injectEvadeVelocity(MovementType movementType, Vec3d movement, CallbackInfo ci, @Local(argsOnly = true) LocalRef<Vec3d> movementRef) {
        var evadeComponent = AffinityComponents.EVADE.getNullable(this);
        if (evadeComponent == null || !evadeComponent.isActive()) return;

        movementRef.set(evadeComponent.velocity());
    }
}

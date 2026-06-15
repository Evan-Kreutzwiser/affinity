package io.wispforest.affinity.blockentity.impl;

import io.wispforest.affinity.aethumflux.net.AethumNetworkMember;
import io.wispforest.affinity.aethumflux.net.MultiblockAethumNetworkMember;
import io.wispforest.affinity.block.impl.SunshineMonolithBlock;
import io.wispforest.affinity.blockentity.template.AethumNetworkMemberBlockEntity;
import io.wispforest.affinity.blockentity.template.TickedBlockEntity;
import io.wispforest.affinity.component.AffinityComponents;
import io.wispforest.affinity.object.AffinityBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;

public class SunshineMonolithBlockEntity extends AethumNetworkMemberBlockEntity implements TickedBlockEntity, MultiblockAethumNetworkMember {
    public SunshineMonolithBlockEntity(BlockPos pos, BlockState state) {
        super(AffinityBlocks.Entities.SUNSHINE_MONOLITH, pos, state);

        this.fluxStorage.setFluxCapacity(8000);
        this.fluxStorage.setMaxInsert(64);
    }

    @Override
    public void tickServer() {
        long flux = flux();
        boolean shouldBeEnabled = flux >= 1 && !this.getCachedState().get(SunshineMonolithBlock.POWERED);

        if (shouldBeEnabled != this.getCachedState().get(SunshineMonolithBlock.ENABLED)) {
            this.world.setBlockState(this.pos, this.getCachedState().with(SunshineMonolithBlock.ENABLED, shouldBeEnabled));

            if (shouldBeEnabled) {
                this.addMonolithToChunks();
            } else {
                this.removeMonolithFromChunks();
            }
        }

        if (shouldBeEnabled && this.world.isRaining()) {
            this.updateFlux(flux - 1);
        }
    }


    @Override
    public void onBroken() {
        super.onBroken();
        this.removeMonolithFromChunks();
    }

    private void addMonolithToChunks() {
        int blockChunkX = this.pos.getX() >> 4;
        int blockChunkZ = this.pos.getZ() >> 4;
        int radius = 3;

        for (int x = blockChunkX - radius; x <= blockChunkX + radius; x++) {
            for (int z = blockChunkZ - radius; z <= blockChunkZ + radius; z++) {
                world.getChunk(x, z).getComponent(AffinityComponents.LOCAL_WEATHER).addMonolith(this.pos);
            }
        }
    }

    private void removeMonolithFromChunks() {
        int blockChunkX = this.pos.getX() >> 4;
        int blockChunkZ = this.pos.getZ() >> 4;
        int radius = 3;

        for (int x = blockChunkX - radius; x <= blockChunkX + radius; x++) {
            for (int z = blockChunkZ - radius; z <= blockChunkZ + radius; z++) {
                world.getChunk(x, z).getComponent(AffinityComponents.LOCAL_WEATHER).removeMonolith(this.pos);
            }
        }
    }

    @Override
    public void appendTooltipEntries(List<Entry> entries) {
        super.appendTooltipEntries(entries);

        if (getCachedState().get(Properties.POWERED)) {
            entries.add(Entry.icon(Text.translatable("text.affinity.tooltip.disabled_by_redstone"), 24, 8));
        }
    }

    @Override
    public Collection<BlockPos> memberBlocks() {
        return List.of(this.getPos(), this.getPos().up());
    }

    @Override
    public boolean isParent() {
        return true;
    }

    @Override
    public AethumNetworkMember parent() {
        return this;
    }
}

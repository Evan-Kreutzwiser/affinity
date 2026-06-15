package io.wispforest.affinity.block.impl;

import io.wispforest.affinity.block.template.AethumNetworkMemberBlock;
import io.wispforest.affinity.blockentity.impl.SunshineMonolithBlockEntity;
import io.wispforest.affinity.blockentity.template.TickedBlockEntity;
import io.wispforest.affinity.object.AffinityBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class SunshineMonolithBlock extends AethumNetworkMemberBlock {

    public static final BooleanProperty ENABLED = Properties.ENABLED;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public static final VoxelShape LOWER_SHAPE = VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 4, 16),
            Block.createCuboidShape(2, 4, 2, 14, 16, 14)
    );

    public static final VoxelShape UPPER_SHAPE = Block.createCuboidShape(2, 0, 2, 14, 16, 14);

    public SunshineMonolithBlock() {
        super(Settings.copy(Blocks.SMOOTH_STONE), CONSUMER_TOOLTIP);
        this.setDefaultState(this.getDefaultState().with(ENABLED, false).with(HALF, DoubleBlockHalf.LOWER).with(POWERED, false));
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        var otherHalfPos = pos.offset(state.get(HALF) == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        var powered = world.isReceivingRedstonePower(pos) | world.isReceivingRedstonePower(otherHalfPos);

        if (state.get(POWERED) != powered) {
            world.setBlockState(pos, state.with(POWERED, powered), 2);

            var otherState = world.getBlockState(otherHalfPos);
            if (otherState.isOf(this) && otherState.get(POWERED) != powered) {
                world.setBlockState(otherHalfPos, otherState.with(POWERED, powered));
            }
        }

        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() != Direction.Axis.Y) return state;
        if (state.get(HALF) == DoubleBlockHalf.LOWER && direction == Direction.UP && !neighborState.isOf(this)) {
            return Blocks.AIR.getDefaultState();
        }
        if (state.get(HALF) == DoubleBlockHalf.UPPER && direction == Direction.DOWN && !neighborState.isOf(this)) {
            return Blocks.AIR.getDefaultState();
        }

        var half = state.get(HALF);
        if (half != DoubleBlockHalf.UPPER) return state;

        var enabled = state.get(ENABLED);
        var downState = world.getBlockState(pos.down());

        if (downState.isOf(this) && downState.get(ENABLED) != enabled) {
            return state.with(ENABLED, downState.get(ENABLED));
        } else {
            return state;
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        var pos = context.getBlockPos();
        var world = context.getWorld();
        var powered = world.isReceivingRedstonePower(pos);

        return pos.getY() < world.getTopY() - 1 && world.getBlockState(pos.up()).canReplace(context)
                ? super.getPlacementState(context).with(POWERED, powered)
                : null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(), this.getDefaultState().with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) != DoubleBlockHalf.UPPER) return super.canPlaceAt(state, world, pos);

        var downState = world.getBlockState(pos.down());
        return downState.isOf(this) && downState.get(HALF) == DoubleBlockHalf.LOWER;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // This is how vanilla doors handle preventing unwanted drops
        if (!world.isClient && (player.isCreative() || !player.canHarvest(state))) {
            TallPlantBlock.onBreakInCreative(world, pos, state, player);
        }

        return super.onBreak(world, pos, state, player);
    }


    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ENABLED).add(HALF).add(POWERED);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, AffinityBlocks.Entities.SUNSHINE_MONOLITH, TickedBlockEntity.ticker());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER
                ? new SunshineMonolithBlockEntity(pos, state)
                : null;
    }
}

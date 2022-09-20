package io.github.squid233.wires.block;

import io.github.squid233.wires.block.entity.InsulatorBlockEntity;
import io.github.squid233.wires.item.ModItems;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * @author squid233
 * @since 0.1.0
 */
public final class InsulatorBlock extends FacingBlock implements BlockEntityProvider {
    public static final BooleanProperty CONNECTED = BooleanProperty.of("connected");

    public InsulatorBlock(Settings settings) {
        super(settings);
        setDefaultState(stateManager.getDefaultState().with(FACING, Direction.NORTH).with(CONNECTED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getSide().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case DOWN -> createCuboidShape(6, -1, 6, 10, 8, 10);
            case UP -> createCuboidShape(6, 8, 6, 10, 17, 10);
            case NORTH -> createCuboidShape(6, 6, -1, 10, 10, 8);
            case SOUTH -> createCuboidShape(6, 6, 8, 10, 10, 17);
            case WEST -> createCuboidShape(-1, 6, 6, 8, 10, 10);
            case EAST -> createCuboidShape(8, 6, 6, 17, 10, 10);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case DOWN -> createCuboidShape(6, 0, 6, 10, 8, 10);
            case UP -> createCuboidShape(6, 8, 6, 10, 16, 10);
            case NORTH -> createCuboidShape(6, 6, 0, 10, 10, 8);
            case SOUTH -> createCuboidShape(6, 6, 8, 10, 10, 16);
            case WEST -> createCuboidShape(0, 6, 6, 8, 10, 10);
            case EAST -> createCuboidShape(8, 6, 6, 16, 10, 10);
        };
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InsulatorBlockEntity(pos, state);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
        super.afterBreak(world, player, pos, state, blockEntity, stack);
        if (blockEntity instanceof InsulatorBlockEntity insulator) {
            // TODO: 2022/9/20 Not affected ?
            insulator.disconnect();
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isSneaking()) {
            return ActionResult.PASS;
        }
        var stack = player.getStackInHand(hand);
        if (stack.isOf(ModItems.WIRE) && player.canModifyBlocks()) {
            var sub = stack.getSubNbt("connecting");
            if (sub == null) {
                var tag = new NbtCompound();
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                stack.setSubNbt("connecting", tag);
            } else {
                int x = sub.getInt("x");
                int y = sub.getInt("y");
                int z = sub.getInt("z");
                if (x == pos.getX() &&
                    y == pos.getY() &&
                    z == pos.getZ()) {
                    stack.removeSubNbt("connecting");
                } else {
                    if (world.getBlockEntity(pos) instanceof InsulatorBlockEntity insulator) {
                        insulator.connect(x, y, z);
                    }
                    stack.removeSubNbt("connecting");
                }
            }
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }
}
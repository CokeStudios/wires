package io.github.squid233.wires.item;

import io.github.squid233.wires.block.InsulatorBlock;
import io.github.squid233.wires.block.entity.InsulatorBlockEntity;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/**
 * @author squid233
 * @since 0.1.0
 */
public final class WireRemoverItem extends SelectorItem {
    public WireRemoverItem(String subKey, Settings settings) {
        super(subKey, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var pos = context.getBlockPos();
        var world = context.getWorld();
        if (context.shouldCancelInteraction() ||
            !(world.getBlockState(pos).getBlock() instanceof InsulatorBlock)) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        var stack = context.getStack();
        var sub = stack.getSubNbt(subKey);
        if (sub == null) {
            var tag = new NbtCompound();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            stack.setSubNbt(subKey, tag);
        } else {
            int x = sub.getInt("x");
            int y = sub.getInt("y");
            int z = sub.getInt("z");
            if (x == pos.getX() &&
                y == pos.getY() &&
                z == pos.getZ()) {
                stack.removeSubNbt(subKey);
            } else {
                if (world.getBlockEntity(pos) instanceof InsulatorBlockEntity insulator) {
                    var bpos = new BlockPos(x, y, z);
                    insulator.disconnect(bpos);
                    if (world.getBlockEntity(bpos) instanceof InsulatorBlockEntity other) {
                        other.disconnect(pos);
                    }
                }
                stack.removeSubNbt(subKey);
            }
        }
        return ActionResult.CONSUME;
    }
}

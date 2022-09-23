package io.github.squid233.wires.block.entity;

import io.github.squid233.wires.block.InsulatorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * @author squid233
 * @since 0.1.0
 */
public final class InsulatorBlockEntity extends BlockEntity {
    private final List<BlockPos> connectedTo = new ArrayList<>();

    public InsulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INSULATOR, pos, state);
    }

    private void _connect(BlockPos bpos) {
        if (connectedTo.contains(bpos)) {
            return;
        }
        connectedTo.add(bpos);
        var state = getCachedState().with(InsulatorBlock.CONNECTED, true);
        setCachedState(state);
        if (world != null) {
            world.setBlockState(pos, state);
        }
        markDirty();
    }

    public void connect(int targetX, int targetY, int targetZ) {
        var bpos = new BlockPos(targetX, targetY, targetZ);
        if (world != null && world.getBlockEntity(bpos) instanceof InsulatorBlockEntity insulator) {
            _connect(bpos);
            insulator._connect(pos);
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        disconnectAll(removed);
    }

    public void disconnect(int index, BlockPos bpos, boolean removed) {
        if (index < 0) {
            connectedTo.remove(bpos);
        } else {
            connectedTo.remove(index);
        }
        if (connectedTo.isEmpty() && !removed) {
            var state = getCachedState().with(InsulatorBlock.CONNECTED, false);
            setCachedState(state);
            if (world != null) {
                world.setBlockState(pos, state);
            }
        }
        markDirty();
    }

    public void disconnectAll(boolean removed) {
        if (world != null) {
            for (int i = connectedTo.size() - 1; i >= 0; i--) {
                var bpos = connectedTo.get(i);
                if (world.getBlockEntity(bpos) instanceof InsulatorBlockEntity insulator) {
                    insulator.disconnect(-1, pos, false);
                }
                disconnect(i, bpos, removed);
            }
        }
    }

    public List<BlockPos> getConnectedTo() {
        return connectedTo;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbtWithIdentifyingData();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        var list = nbt.getList("connectedTo", NbtElement.COMPOUND_TYPE);
        connectedTo.clear();
        for (int i = 0, len = list.size(); i < len; i++) {
            var c = list.getCompound(i);
            connectedTo.add(posFromNbt(c));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        var list = new NbtList();
        for (var bpos : connectedTo) {
            var c = new NbtCompound();
            c.putInt("x", bpos.getX());
            c.putInt("y", bpos.getY());
            c.putInt("z", bpos.getZ());
            list.add(list.size(), c);
        }
        nbt.put("connectedTo", list);
    }
}

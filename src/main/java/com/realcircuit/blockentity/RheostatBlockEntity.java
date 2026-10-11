package com.realcircuit.blockentity;

import com.realcircuit.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 滑动变阻器方块实体：存滑片位置 percent(0~1)、最大阻值、接线柱选择 connMask。 */
public class RheostatBlockEntity extends BlockEntity {
    public double percent = 0.5;
    public final double maxR;
    /** 低 4 位：bit0=左上 bit1=右上 bit2=左下 bit3=右下；默认 0 = 全部未连接。 */
    public int connMask = 0;

    public RheostatBlockEntity(BlockPos pos, BlockState state, double maxR) {
        super(ModBlockEntities.RHEOSTAT, pos, state);
        this.maxR = maxR;
    }

    /** 校正为恰好 2 个端子；非法则置 0（全部未连接）。 */
    public void setConnMask(int mask) {
        int bits = Integer.bitCount(mask & 0xF);
        this.connMask = (bits == 2) ? (mask & 0xF) : 0;
    }

    /** 改动后同步到客户端，重开界面才会显示最新值。 */
    public void syncToClient() {
        setChanged();
        if (getLevel() != null && !getLevel().isClientSide()) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider p) {
        return saveWithoutMetadata(p);
    }

    public boolean tl() { return (connMask & 1) != 0; }
    public boolean tr() { return (connMask & 2) != 0; }
    public boolean bl() { return (connMask & 4) != 0; }
    public boolean br() { return (connMask & 8) != 0; }

    public double currentR() {
        return Math.max(percent * maxR, 1e-4);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("Percent", percent);
        output.putInt("Conn", connMask);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        percent = input.getDoubleOr("Percent", 0.5);
        connMask = input.getIntOr("Conn", 0);
    }
}

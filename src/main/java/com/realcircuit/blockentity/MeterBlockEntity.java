package com.realcircuit.blockentity;

import com.realcircuit.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 电表方块实体：存最新读数（电流/电压），服务端求解后写入并同步到客户端浮窗显示。 */
public class MeterBlockEntity extends BlockEntity {
    public final boolean isAmmeter;
    /** 电流表=电流(A)，电压表=电压(V)；带符号表示极性方向。 */
    public double reading = 0.0;
    /** 当前生效量程：0=小量程（高精度、低阈值），1=大量程（低精度、高阈值）。 */
    public int range = 0;

    /** 量程上限（含）：小/大量程。电流表 0.6/3 A，电压表 3/15 V。 */
    public static double rangeLimit(boolean ammeter, int range) {
        return ammeter ? (range == 0 ? 0.6 : 3.0) : (range == 0 ? 3.0 : 15.0);
    }

    public void setRange(int r) {
        if (this.range != r) { this.range = r; setChanged(); }
    }

    public MeterBlockEntity(BlockPos pos, BlockState state, boolean isAmmeter) {
        super(ModBlockEntities.METER, pos, state);
        this.isAmmeter = isAmmeter;
    }

    public void setReading(double v) {
        this.reading = v;
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
    public CompoundTag getUpdateTag(HolderLookup.Provider p) {
        return saveWithoutMetadata(p);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("Reading", reading);
        output.putInt("Range", range);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        reading = input.getDoubleOr("Reading", 0.0);
        range = input.getIntOr("Range", 0);
    }
}

package com.realcircuit.blockentity;

import com.realcircuit.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** 学生电源方块实体：存当前输出电压 0~220V（两位小数）。 */
public class StudentPowerBlockEntity extends BlockEntity {
    public double voltage = 6.0;

    public StudentPowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STUDENT_POWER, pos, state);
    }

    public void setVoltage(double v) {
        this.voltage = Math.max(0, Math.min(220.0, Math.round(v * 100) / 100.0));
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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("Voltage", voltage);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        voltage = input.getDoubleOr("Voltage", 6.0);
    }
}

package com.realcircuit.network;

import com.realcircuit.RealCircuitMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 客户端→服务端：学生电源输出电压。 */
public record StudentPowerUpdatePayload(BlockPos pos, float voltage) implements CustomPacketPayload {
    public static final Type<StudentPowerUpdatePayload> TYPE =
            new Type<>(RealCircuitMod.id("student_power_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StudentPowerUpdatePayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeFloat(p.voltage); },
            buf -> new StudentPowerUpdatePayload(buf.readBlockPos(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

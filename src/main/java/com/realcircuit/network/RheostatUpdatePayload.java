package com.realcircuit.network;

import com.realcircuit.RealCircuitMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 客户端→服务端：滑动变阻器的滑片位置 percent 与接线柱选择 connMask。
 * connMask 低 4 位：bit0=左上(TL) bit1=右上(TR) bit2=左下(BL) bit3=右下(BR)，恰有 2 位置 1。
 */
public record RheostatUpdatePayload(BlockPos pos, float percent, int connMask) implements CustomPacketPayload {
    public static final Type<RheostatUpdatePayload> TYPE =
            new Type<>(RealCircuitMod.id("rheostat_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RheostatUpdatePayload> CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeFloat(p.percent); buf.writeInt(p.connMask); },
            buf -> new RheostatUpdatePayload(buf.readBlockPos(), buf.readFloat(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

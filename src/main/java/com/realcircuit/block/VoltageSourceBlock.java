package com.realcircuit.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

/** 直流电源方块：固定电压。带正负极。 */
public class VoltageSourceBlock extends CircuitBlock {
    public final double voltage;

    public VoltageSourceBlock(Properties p, double v) {
        super(p);
        this.voltage = v;
    }

    @Override
    public List<Component> circuitTooltip() {
        return List.of(
                Component.literal("§7输出电压：§e" + voltage + " V"),
                Component.literal("§7带正负极，可串联叠加")
        );
    }
}

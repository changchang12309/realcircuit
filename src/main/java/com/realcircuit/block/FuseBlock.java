package com.realcircuit.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

/** 保险丝方块。damaged=true 为熔断后的独立损坏方块。 */
public class FuseBlock extends CircuitBlock {
    public final boolean damaged;
    public FuseBlock(Properties p, boolean damaged) { super(p); this.damaged = damaged; }

    @Override
    public List<Component> circuitTooltip() {
        if (damaged) return List.of(Component.literal("§c已熔断：断路"));
        return List.of(Component.literal("§7过流即熔断（替换为熔断方块）"));
    }
}

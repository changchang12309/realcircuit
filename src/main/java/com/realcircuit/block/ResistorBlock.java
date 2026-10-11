package com.realcircuit.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

/** 定值电阻方块。损坏版 tooltip 标注"已损坏（断路）"。 */
public class ResistorBlock extends CircuitBlock {
    public final double resistance;
    public final boolean damaged;

    public ResistorBlock(Properties p, double r, boolean damaged) {
        super(p);
        this.resistance = r;
        this.damaged = damaged;
    }

    @Override
    public List<Component> circuitTooltip() {
        if (damaged) return List.of(Component.literal("§c已损坏：断路，无电阻效果"));
        return List.of(Component.literal("§7阻值：§e" + (int) resistance + " Ω"));
    }
}

package com.realcircuit.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;

/** LED 方块：固定 Vf=2.0V，内阻 10Ω。两段模型（正向导通/反向断路）。 */
public class LedBlock extends CircuitBlock {
    public static final double VF = 2.0;
    public static final double RF = 10.0;

    public LedBlock(Properties p) { super(p); }

    @Override
    public List<Component> circuitTooltip() {
        return List.of(
                Component.literal("§7正向压降：§e2.0 V §7内阻：§e10 Ω"),
                Component.literal("§7单向导电，反接断路")
        );
    }
}

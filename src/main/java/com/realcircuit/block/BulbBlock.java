package com.realcircuit.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.List;

/** 小灯泡方块：额定电压/电流。通电正向导通时发光（LIT）；过流/反接替换为损坏方块。 */
public class BulbBlock extends CircuitBlock {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public final double ratedU;
    public final double ratedI;
    public final boolean damaged;

    public BulbBlock(Properties p, double u, double i, boolean damaged) {
        super(p);
        this.ratedU = u; this.ratedI = i; this.damaged = damaged;
        registerDefaultState(getStateDefinition().any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(LIT);
    }

    @Override
    public List<Component> circuitTooltip() {
        if (damaged) return List.of(Component.literal("§c已损坏：断路"));
        return List.of(
                Component.literal("§7额定：§e" + ratedU + "V  " + ratedI + "A"),
                Component.literal("§7亮度随电流变化，过流损坏")
        );
    }
}

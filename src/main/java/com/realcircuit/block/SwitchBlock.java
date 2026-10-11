package com.realcircuit.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** 单刀开关：open 属性（true=断开）。右键切换。blockstate 已配 open=true/false 两模型。 */
public class SwitchBlock extends CircuitBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public SwitchBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(OPEN, true).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(OPEN, FACING);
    }

    /** 右键切换通断，并触发电路重算（neighborChanged 会自动触发）。 */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(OPEN), 3);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public List<Component> circuitTooltip() {
        return List.of(Component.literal("§7右键切换 通/断"));
    }
}

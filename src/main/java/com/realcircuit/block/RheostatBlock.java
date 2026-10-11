package com.realcircuit.block;

import com.realcircuit.blockentity.RheostatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 滑动变阻器方块：带方块实体，存滑片位置 percent 与最大阻值。 */
public class RheostatBlock extends CircuitBlock implements EntityBlock {
    public final double maxR;

    public RheostatBlock(Properties p, double maxR) {
        super(p);
        this.maxR = maxR;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RheostatBlockEntity(pos, state, maxR);
    }

    @Override
    public List<Component> circuitTooltip() {
        return List.of(
                Component.literal("§7最大阻值：§e" + (int) maxR + " Ω"),
                Component.literal("§7接法：一上一下；右键调节滑片")
        );
    }
}

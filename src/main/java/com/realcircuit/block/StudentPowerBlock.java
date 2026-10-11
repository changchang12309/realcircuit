package com.realcircuit.block;

import com.realcircuit.blockentity.StudentPowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 学生电源：0~220V 可调，方块实体存当前电压。 */
public class StudentPowerBlock extends CircuitBlock implements EntityBlock {
    public StudentPowerBlock(Properties p) { super(p); }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StudentPowerBlockEntity(pos, state);
    }

    @Override
    public List<Component> circuitTooltip() {
        return List.of(
                Component.literal("§7可调电压：§e0 ~ 220 V"),
                Component.literal("§7右键打开调节界面")
        );
    }
}

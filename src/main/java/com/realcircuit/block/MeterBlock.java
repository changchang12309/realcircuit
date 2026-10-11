package com.realcircuit.block;

import com.realcircuit.blockentity.MeterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 电表方块：isAmmeter 区分电流/电压表；electronic 为电子款（永不损坏）。
 * 机械款（!electronic）为 3×1×1 多方块结构：PART=0 左(−) / 1 中(+) / 2 右(+)。
 * 超量程 → DAMAGED=true（不导通，外观变为损坏）。
 */
public class MeterBlock extends CircuitBlock implements EntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");

    public final boolean isAmmeter;
    public final boolean electronic;
    /** 机械款为多方块结构；电子款保持单方块。 */
    public final boolean multiblock;
    /** 量程上限：电流表 3A、电压表 15V，超过即损坏（仅机械款）。 */
    public final double rated;

    /** 联动破坏重入保护。 */
    private static volatile boolean breaking;

    public MeterBlock(Properties p, boolean isAmmeter, boolean electronic) {
        super(p);
        this.isAmmeter = isAmmeter;
        this.electronic = electronic;
        this.multiblock = !electronic;
        this.rated = isAmmeter ? 3.0 : 15.0;
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(DAMAGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(PART);
        b.add(DAMAGED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MeterBlockEntity(pos, state, isAmmeter);
    }

    public static boolean isDamaged(BlockState s) {
        return s.hasProperty(DAMAGED) && s.getValue(DAMAGED);
    }

    /** 多方块排布方向：面朝 FACING 时，三格沿其顺时针方向（左→右）展开。 */
    public static Direction rowDir(Direction facing) {
        return facing.getClockWise();
    }

    /** 由任一格位置反推主控格（PART=0）位置。 */
    public static BlockPos masterPos(BlockPos any, Direction facing, int part) {
        return any.relative(rowDir(facing).getOpposite(), part);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!multiblock || level.isClientSide()) return;
        Direction facing = state.getValue(FACING);
        Direction row = rowDir(facing);
        BlockPos mid = pos.relative(row);
        BlockPos right = pos.relative(row, 2);
        // 空间不足则退化为单块（主控仍可用），不消耗多余判定
        if (!level.getBlockState(mid).canBeReplaced() || !level.getBlockState(right).canBeReplaced()) {
            return;
        }
        level.setBlock(mid, state.setValue(PART, 1), 3);
        level.setBlock(right, state.setValue(PART, 2), 3);
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState playerWillDestroy(Level level, BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.entity.player.Player player) {
        if (multiblock && !breaking && !level.isClientSide()) {
            breaking = true;
            try {
                Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
                int part = state.hasProperty(PART) ? state.getValue(PART) : 0;
                BlockPos master = masterPos(pos, facing, part);
                Direction row = rowDir(facing);
                for (int k = 0; k < 3; k++) {
                    BlockPos p = master.relative(row, k);
                    if (!p.equals(pos) && level.getBlockState(p).is(this)) {
                        level.removeBlock(p, false);
                    }
                }
            } finally {
                breaking = false;
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public List<Component> circuitTooltip() {
        String kind = isAmmeter ? "电流表（须串联）" : "电压表（须并联）";
        String dur = electronic ? "§a电子款：永不损坏" : "§e机械款：超量程将损坏";
        String term = "§71 黑公共端(−) + 2 红端：中=小量程(高精度)，右=大量程";
        return List.of(Component.literal("§7" + kind), Component.literal(dur), Component.literal(term));
    }
}

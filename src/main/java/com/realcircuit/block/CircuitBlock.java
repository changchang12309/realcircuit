package com.realcircuit.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;

import java.util.List;

/**
 * 电路元件方块基类。
 * 统一水平朝向属性 FACING：放置时正极(红面)固定朝向玩家，放置后不再变化。
 * 模型约定：北(north)面为正极(红)、南(south)面为负极(黑)，通过 blockstate 的 y 旋转对齐朝向。
 */
public abstract class CircuitBlock extends Block {
    /** 水平朝向：表示正极(红)面所指的方向。 */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public CircuitBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    /** 放置时正极朝向玩家：+ 面方向 = 玩家朝向的反方向（面朝玩家）。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getPlayer() != null
                ? context.getPlayer().getDirection().getOpposite()
                : Direction.NORTH;
        return defaultBlockState().setValue(FACING, dir);
    }

    /** 子类提供 tooltip 行（中文）——供物品/物品栏展示，由上层按需取用。 */
    public abstract List<Component> circuitTooltip();

    /** 方块放置 → 触发所在电路重算。 */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) com.realcircuit.world.CircuitWorldManager.solveAround(level, pos);
    }

    /** 玩家破坏方块 → 触发所在电路重算。 */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) com.realcircuit.world.CircuitWorldManager.solveAround(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** 邻接方块变化（含红石粉更新）→ 触发重算。 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide()) com.realcircuit.world.CircuitWorldManager.solveAround(level, pos);
    }

    protected static Component t(String s) { return Component.literal(s); }
}

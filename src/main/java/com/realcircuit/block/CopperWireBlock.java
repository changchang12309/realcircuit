package com.realcircuit.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 铜导线：红石粉式的可连接细长条导线。
 * - 不是完整方块，贴地薄条；
 * - 四周出现其它铜导线时自动连接（north/east/south/west 属性）；
 * - 电路层中作为零电阻导线节点（见 CircuitWorldManager.isWire）。
 */
public class CopperWireBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    private static final VoxelShape DOT = Shapes.box(5.0 / 16, 0.0, 5.0 / 16, 11.0 / 16, 2.0 / 16, 11.0 / 16);
    private static final VoxelShape N_SIDE = Shapes.box(5.0 / 16, 0.0, 0.0, 11.0 / 16, 2.0 / 16, 6.0 / 16);
    private static final VoxelShape S_SIDE = Shapes.box(5.0 / 16, 0.0, 10.0 / 16, 11.0 / 16, 2.0 / 16, 16.0 / 16);
    private static final VoxelShape W_SIDE = Shapes.box(0.0, 0.0, 5.0 / 16, 6.0 / 16, 2.0 / 16, 11.0 / 16);
    private static final VoxelShape E_SIDE = Shapes.box(10.0 / 16, 0.0, 5.0 / 16, 16.0 / 16, 2.0 / 16, 11.0 / 16);

    public CopperWireBlock(Properties p) {
        super(p);
        registerDefaultState(getStateDefinition().any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, EAST, SOUTH, WEST);
    }

    /** 该方向是否连接：邻格是铜导线，或任何电路元件方块（都视为可连接目标）。 */
    private boolean connectsTo(LevelReader level, BlockPos pos, Direction dir) {
        Block b = level.getBlockState(pos.relative(dir)).getBlock();
        return b instanceof CopperWireBlock || b instanceof CircuitBlock;
    }

    private BlockState withConnections(LevelReader level, BlockPos pos, BlockState in) {
        return in.setValue(NORTH, connectsTo(level, pos, Direction.NORTH))
                .setValue(EAST, connectsTo(level, pos, Direction.EAST))
                .setValue(SOUTH, connectsTo(level, pos, Direction.SOUTH))
                .setValue(WEST, connectsTo(level, pos, Direction.WEST));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        return withConnections(level, pos, state);
    }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) com.realcircuit.world.CircuitWorldManager.solveAround(level, pos);
    }

    @Override
    public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos,
                                        BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide()) com.realcircuit.world.CircuitWorldManager.solveAround(level, pos);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        VoxelShape shape = DOT;
        if (s.getValue(NORTH)) shape = Shapes.or(shape, N_SIDE);
        if (s.getValue(SOUTH)) shape = Shapes.or(shape, S_SIDE);
        if (s.getValue(WEST)) shape = Shapes.or(shape, W_SIDE);
        if (s.getValue(EAST)) shape = Shapes.or(shape, E_SIDE);
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty(); // 可穿过，像红石粉一样无碰撞
    }
}

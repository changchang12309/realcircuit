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
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final VoxelShape DOT = Shapes.box(5.0 / 16, 0.0, 5.0 / 16, 11.0 / 16, 2.0 / 16, 11.0 / 16);
    private static final VoxelShape N_SIDE = Shapes.box(5.0 / 16, 0.0, 0.0, 11.0 / 16, 2.0 / 16, 6.0 / 16);
    private static final VoxelShape S_SIDE = Shapes.box(5.0 / 16, 0.0, 10.0 / 16, 11.0 / 16, 2.0 / 16, 16.0 / 16);
    private static final VoxelShape W_SIDE = Shapes.box(0.0, 0.0, 5.0 / 16, 6.0 / 16, 2.0 / 16, 11.0 / 16);
    private static final VoxelShape E_SIDE = Shapes.box(10.0 / 16, 0.0, 5.0 / 16, 16.0 / 16, 2.0 / 16, 11.0 / 16);
    /** 竖直向上的连接臂：从中心点一直升到方块顶面（贴合上方导线/元件）。 */
    private static final VoxelShape UP_ARM = Shapes.box(5.0 / 16, 2.0 / 16, 5.0 / 16, 11.0 / 16, 16.0 / 16, 11.0 / 16);

    public CopperWireBlock(Properties p) {
        super(p);
        registerDefaultState(getStateDefinition().any()
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** 该方向是否连接：邻格是铜导线、电路元件方块，或可导电的原版方块（铁块/铜块）。 */
    private boolean connectsTo(LevelReader level, BlockPos pos, Direction dir) {
        Block b = level.getBlockState(pos.relative(dir)).getBlock();
        if (b instanceof CopperWireBlock || b instanceof CircuitBlock) return true;
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath();
        return path.equals("iron_block") || path.equals("copper_block");
    }

    private BlockState withConnections(LevelReader level, BlockPos pos, BlockState in) {
        return in.setValue(NORTH, connectsTo(level, pos, Direction.NORTH))
                .setValue(EAST, connectsTo(level, pos, Direction.EAST))
                .setValue(SOUTH, connectsTo(level, pos, Direction.SOUTH))
                .setValue(WEST, connectsTo(level, pos, Direction.WEST))
                .setValue(UP, connectsTo(level, pos, Direction.UP))
                .setValue(DOWN, connectsTo(level, pos, Direction.DOWN));
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

    /** 邻接方块变化（如放置/拆除原版导体方块）→ 触发所在电路重算。 */
    @Override
    public void neighborChanged(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                Block block, net.minecraft.world.level.redstone.Orientation fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide()) com.realcircuit.world.CircuitWorldManager.solveAround(level, pos);
    }

    /** 玩家放置电路元件 → 成就钩子（电路入门）。 */
    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof net.minecraft.world.entity.player.Player p) {
            com.realcircuit.world.CircuitAdvancements.onElementPlaced(level, p);
        }
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
        if (s.getValue(UP)) shape = Shapes.or(shape, UP_ARM);
        // DOWN 无需几何：下方连接由相邻导线自身的 UP 臂（或元件顶面）贴合
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return Shapes.empty(); // 可穿过，像红石粉一样无碰撞
    }
}

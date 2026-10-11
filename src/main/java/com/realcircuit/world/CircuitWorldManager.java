package com.realcircuit.world;

import com.realcircuit.block.*;
import com.realcircuit.circuit.*;
import com.realcircuit.circuit.elements.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * 世界电路桥接：把世界中的电路方块转成电路图，求解后把结果写回世界。
 *
 * 建模约定（无歧义，便于教学）：
 *  - 红石粉导线 = 等电位节点；相邻导线自动并入同一节点（并查集）。
 *  - 元件方块本身不是节点，而是一条二端支路；它的两个端子 = 它相邻的两个导线（或元件）所在节点。
 *    因此元件之间需用导线连接，不能直接贴面（除非后续扩展元件-元件直连）。
 *  - 元件的两个端子取其六邻域中前两个"已连接到导线"的方向，与朝向无关，简单稳健。
 *
 * 触发时机：方块放置/破坏/状态变化时由事件回调调用 solveAround（见 CircuitEvents）。
 * 服务端逻辑，须在主线程执行。
 */
public class CircuitWorldManager {

    private CircuitWorldManager() {}

    /** 重入保护：求解过程中 setBlock 会再次触发 neighborChanged，需阻止递归。 */
    private static volatile boolean solving = false;

    private static final Direction[] DIRS = Direction.values();

    // 铁块/铜块的基础电阻（欧），铜块会因铜锈增大（此处用基础值，锈态由方块数据扩展）
    private static final double IRON_R = 0.05;
    private static final double COPPER_R = 0.02;
    // 红石块固定 220V
    private static final double REDSTONE_BLOCK_V = 220.0;

    /** 主入口：给定一个发生变化的位置，重建并求解其所在电路。 */
    public static void solveAround(Level level, BlockPos center) {
        if (!(level instanceof ServerLevel server)) return;
        if (solving) return; // 重入保护
        solving = true;
        try {
            doSolve(server, center);
        } finally {
            solving = false;
        }
    }

    private static void doSolve(ServerLevel server, BlockPos center) {

        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> circuitBlocks = new ArrayList<>();
        Deque<BlockPos> stack = new ArrayDeque<>();
        stack.push(center);
        while (!stack.isEmpty()) {
            BlockPos p = stack.pop();
            if (visited.contains(p)) continue;
            if (visited.size() > 8192) break; // 防爆
            visited.add(p);
            if (!isCircuitRelated(server, p)) continue;
            circuitBlocks.add(p);
            for (Direction d : DIRS) {
                BlockPos n = p.relative(d);
                if (!visited.contains(n)) stack.push(n);
            }
        }
        if (circuitBlocks.isEmpty()) return;

        CircuitGraph graph = buildGraph(server, circuitBlocks);
        if (graph.branches.isEmpty()) return;

        CircuitSolution sol = DcSolver.solve(graph);
        applySolution(server, sol);
    }

    private static boolean isCircuitRelated(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        Block b = s.getBlock();
        String path = blockPath(b);
        return b instanceof CircuitBlock
                || isWire(s)
                || path.equals("redstone_block")
                || path.equals("iron_block")
                || path.equals("copper_block");
    }

    private static boolean isWire(BlockState s) {
        return s.getBlock() instanceof com.realcircuit.block.CopperWireBlock;
    }

    /** 构建电路图。 */
    private static CircuitGraph buildGraph(ServerLevel level, List<BlockPos> blocks) {
        CircuitGraph graph = new CircuitGraph();
        UnionFind uf = new UnionFind();

        // 1) 所有导线位置进并查集，相邻导线合并
        for (BlockPos p : blocks) {
            if (isWire(level.getBlockState(p))) {
                uf.make(key(p));
            }
        }
        for (BlockPos p : blocks) {
            if (!isWire(level.getBlockState(p))) continue;
            for (Direction d : DIRS) {
                BlockPos n = p.relative(d);
                if (isWire(level.getBlockState(n))) uf.union(key(p), key(n));
            }
        }

        // 2) 为每个并查集根分配节点 id
        Map<String, Integer> nodeOfRoot = new HashMap<>();
        for (BlockPos p : blocks) {
            if (!isWire(level.getBlockState(p))) continue;
            String root = uf.find(key(p));
            nodeOfRoot.computeIfAbsent(root, r -> {
                int id = graph.nodes.size();
                graph.nodes.add(new Node(id));
                return id;
            });
        }

        // 取某位置所在的导线节点；若该位置不是导线但邻接导线，返回邻接导线节点
        java.util.function.Function<BlockPos, Integer> nodeAt = (pos) -> {
            if (isWire(level.getBlockState(pos))) {
                Integer id = nodeOfRoot.get(uf.find(key(pos)));
                if (id != null) return id;
            }
            return -1;
        };

        // 3) 每个元件 → 一条支路，端子绑定到相邻导线节点
        for (BlockPos p : blocks) {
            BlockState st = level.getBlockState(p);
            if (isWire(st)) continue; // 导线已作为节点，不当作支路
            Block b = st.getBlock();
            CircuitElement e = elementFor(level, p, st);
            if (e == null) continue;

            // 严格按方块朝向绑定端子：正极(红=FACING 面)→pos，负极(黑=对面)→neg。
            // 非 +/- 面的导线一律忽略（错误接面 = 不导通，也不会误烧坏）。
            Direction plusDir = (st.hasProperty(CircuitBlock.FACING))
                    ? st.getValue(CircuitBlock.FACING) : null;
            int na = -1, nb = -1;
            if (plusDir != null) {
                na = nodeAt.apply(p.relative(plusDir));
                nb = nodeAt.apply(p.relative(plusDir.getOpposite()));
            }
            // 电表（机械多方块）单独处理端子：见 bindMeterTerminals
            if (b instanceof MeterBlock mb && mb.multiblock && plusDir != null
                    && st.getValue(MeterBlock.PART) == 0) {
                int[] mt = bindMeterTerminals(level, p, st, plusDir, nodeAt);
                na = mt[0]; nb = mt[1];
            }
            // 导体（铜块/铁块）无 +/− 朝向：把它接在任意两个相邻导线节点之间，正常导通。
            if (e instanceof com.realcircuit.circuit.elements.ConductorElement) {
                na = -1; nb = -1;
                for (Direction d : DIRS) {
                    int n = nodeAt.apply(p.relative(d));
                    if (n < 0) continue;
                    if (na < 0) na = n;
                    else if (n != na) { nb = n; break; }
                }
            }
            // 某一面没接导线 → 该端悬空（给独立新节点），形成开路，无电流
            if (na < 0) na = newNode(graph);
            if (nb < 0) nb = newNode(graph);

            e.pos.nodeId = na;
            e.neg.nodeId = nb;
            graph.branches.add(new Branch(e, na, nb));
        }
        return graph;
    }

    private static int newNode(CircuitGraph g) {
        int id = g.nodes.size();
        g.nodes.add(new Node(id));
        return id;
    }

    /**
     * 机械电表多方块端子绑定（主控格 part=0 调用）：
     *  - 公共端(黑,−) = 左格(part0) 正面邻接的导线；
     *  - 小量程(红,+) = 中格(part1) 正面邻接的导线 → range=0（高精度、低超量程阈值）；
     *  - 大量程(红,+) = 右格(part2) 正面邻接的导线 → range=1（低精度、高超量程阈值）。
     *  同时只应接一个红色端；中格优先（小量程）。
     */
    private static int[] bindMeterTerminals(Level level, BlockPos master, BlockState st,
                                            Direction facing,
                                            java.util.function.Function<BlockPos, Integer> nodeAt) {
        Direction row = MeterBlock.rowDir(facing);
        BlockPos left = master;                       // 公共端 黑(−)
        BlockPos mid = master.relative(row);          // 红 小量程(+)
        BlockPos right = master.relative(row, 2);     // 红 大量程(+)
        int neg = nodeAt.apply(left.relative(facing));
        int smallPos = nodeAt.apply(mid.relative(facing));
        int largePos = nodeAt.apply(right.relative(facing));
        int range = (smallPos >= 0) ? 0 : 1;
        int pos = (smallPos >= 0) ? smallPos : largePos;
        // 记录当前生效量程，供超量程判定与显示精度使用
        if (level.getBlockEntity(master) instanceof com.realcircuit.blockentity.MeterBlockEntity me) {
            me.setRange(range);
        }
        return new int[]{pos, neg};
    }

    /** 根据方块生成对应电学元件。位置编码进 id 以便回写。 */
    private static CircuitElement elementFor(Level level, BlockPos p, BlockState s) {
        Block b = s.getBlock();
        String id = key(p);
        if (b instanceof ResistorBlock r) return r.damaged ? null : new ResistorElement(id, r.resistance);
        if (b instanceof BulbBlock bu) return bu.damaged ? null : new BulbElement(id, bu.ratedU, bu.ratedI);
        if (b instanceof VoltageSourceBlock v) return new VoltageSourceElement(id, v.voltage);
        if (b instanceof LedBlock) return new LedElement(id);
        if (b instanceof SwitchBlock sw) return new SwitchElement(id, !s.getValue(SwitchBlock.OPEN));
        if (b instanceof FuseBlock f) return f.damaged ? null : new FuseElement(id, 2.0);
        if (b instanceof MeterBlock m) {
            if (MeterBlock.isDamaged(s)) return null;            // 已损坏：不导通
            if (m.multiblock && s.getValue(MeterBlock.PART) != 0) return null; // 仅主控格参与电路
            return m.isAmmeter ? new AmmeterElement(id) : new VoltmeterElement(id);
        }
        if (b instanceof RheostatBlock rh) {
            double percent = 0.5;
            int connMask = 1 | 4;
            if (level.getBlockEntity(p) instanceof com.realcircuit.blockentity.RheostatBlockEntity re) {
                percent = re.percent;
                connMask = re.connMask;
            }
            return new RheostatElement(id, rh.maxR, percent,
                    (connMask & 1) != 0, (connMask & 2) != 0,
                    (connMask & 4) != 0, (connMask & 8) != 0);
        }
        if (b instanceof StudentPowerBlock) {
            double v = 6.0;
            if (level.getBlockEntity(p) instanceof com.realcircuit.blockentity.StudentPowerBlockEntity sp)
                v = sp.voltage;
            return new VoltageSourceElement(id, v);
        }
        // 原版方块行为（按需求文档赋予电阻/电源特性）
        String path = blockPath(b);
        if (path.equals("redstone_block")) return new VoltageSourceElement(id, REDSTONE_BLOCK_V);
        if (path.equals("iron_block")) return new ConductorElement(id, IRON_R);
        if (path.equals("copper_block")) return new ConductorElement(id, COPPER_R);
        return null;
    }

    /** 把求解结果应用到世界：过流/反接损坏替换 + 发光状态 + 短路效果。 */
    private static void applySolution(ServerLevel level, CircuitSolution sol) {
        // 预先收集所有电源位置：短路爆炸必须发生在电源上，而非开关/导线等其它元件
        java.util.List<BlockPos> sources = new java.util.ArrayList<>();
        for (CircuitElement e : sol.elements()) {
            if (e.isVoltageSource()) { BlockPos sp = posFromKey(e.id); if (sp != null) sources.add(sp); }
        }
        boolean anyShorted = false;

        for (CircuitElement e : sol.elements()) {
            BlockPos p = posFromKey(e.id);
            if (p == null) continue;
            BlockState s = level.getBlockState(p);
            double i = e.getCurrent();
            double abs = Math.abs(i);
            boolean connected = e.pos.nodeId >= 0 && e.neg.nodeId >= 0 && e.pos.nodeId != e.neg.nodeId;

            if (e instanceof ResistorElement && abs > 5.0) {
                replaceWithDamaged(level, p, s);
                spawnWhiteParticles(level, p);
                continue;
            }
            if (e instanceof BulbElement bu) {
                if (abs > bu.ratedI * 1.5) {
                    replaceWithDamaged(level, p, s);   // 过流烧坏
                    spawnWhiteParticles(level, p);
                } else if (connected && i < -1e-6) {
                    replaceWithDamaged(level, p, s);   // 反接（电源+ 接灯泡−）→ 错误连接烧坏
                    spawnWhiteParticles(level, p);
                } else {
                    setBulbLit(level, p, s, connected && i > 1e-4); // 正向导通才点亮
                }
                continue;
            }
            if (e instanceof FuseElement fu && abs > fu.ratedI) {
                replaceWithDamaged(level, p, s);
                spawnWhiteParticles(level, p);
            }
            if (e instanceof AmmeterElement || e instanceof VoltmeterElement) {
                handleMeterResult(level, p, s, e);
                continue;
            }
            // 短路判定（只置标志，不在此处爆炸——爆炸统一交给电源位置）：
            // ① 电压源两端等效电阻过小；② 任一支路电流远超阈值。
            if (!anyShorted) {
                if (e instanceof VoltageSourceElement) {
                    double req = equivalentResistance(sol.graph, e.pos.nodeId, e.neg.nodeId);
                    anyShorted = (req >= 0 && req < SHORT_RESISTANCE) || e.pos.nodeId == e.neg.nodeId;
                } else if (abs > SHORT_CURRENT) {
                    anyShorted = true;
                }
            }
        }

        // 短路效果统一作用于电源：先把电源替换为"损坏的电源"（停止输出，避免持续喷火耗能），
        // 再在电源位置触发起火+爆炸。
        if (anyShorted) {
            for (BlockPos sp : sources) {
                if (level.getBlockState(sp).getBlock() != com.realcircuit.registry.ModBlocks.POWER_DAMAGED) {
                    level.setBlock(sp, com.realcircuit.registry.ModBlocks.POWER_DAMAGED.defaultBlockState(), 3);
                }
                shortCircuit(level, sp);
            }
        }
    }

    private static void setBulbLit(ServerLevel level, BlockPos p, BlockState s, boolean lit) {
        if (s.hasProperty(BulbBlock.LIT) && s.getValue(BulbBlock.LIT) != lit) {
            level.setBlock(p, s.setValue(BulbBlock.LIT, lit), 3);
        }
    }

    /** 电表：把读数写入方块实体（供客户端浮窗）；机械款超量程则置为损坏。 */
    private static void handleMeterResult(ServerLevel level, BlockPos p, BlockState s, CircuitElement e) {
        if (!(level.getBlockEntity(p) instanceof com.realcircuit.blockentity.MeterBlockEntity be)) return;
        boolean ammeter = e instanceof AmmeterElement;
        double value = ammeter ? e.getCurrent() : e.getVoltage();
        be.setReading(value);

        Block b = s.getBlock();
        double limit = com.realcircuit.blockentity.MeterBlockEntity.rangeLimit(ammeter, be.range);
        if (b instanceof MeterBlock m && m.multiblock && Math.abs(value) > limit) {
            // 超量程损坏：三格全部标记 DAMAGED（不再导通）
            Direction facing = s.getValue(MeterBlock.FACING);
            Direction row = MeterBlock.rowDir(facing);
            for (int k = 0; k < 3; k++) {
                BlockPos pk = p.relative(row, k);
                BlockState sk = level.getBlockState(pk);
                if (sk.is(m) && !MeterBlock.isDamaged(sk)) {
                    level.setBlock(pk, sk.setValue(MeterBlock.DAMAGED, true), 3);
                }
            }
            spawnWhiteParticles(level, p);
        }
    }

    /** 短路判定阈值（A）：超过即视为短路。 */
    private static final double SHORT_CURRENT = 25.0;
    /** 短路电阻阈值（Ω）：电源回路等效电阻低于此值判定为短路。 */
    private static final double SHORT_RESISTANCE = 1.0;
    private static long lastShortMs = 0L;

    /**
     * 计算节点 na 与 nb 之间的等效电阻（Ω）：把除电压源外的所有支路电导做节点分析，
     * 固定 V(na)=1、V(nb)=0，解出其余节点电位，再求流出 na 的电流 I，R_eq = 1/I。
     * 无通路返回一个极大值（不判短路）；参数无效返回 -1。
     */
    private static double equivalentResistance(CircuitGraph g, int na, int nb) {
        int n = g.nodes.size();
        if (na < 0 || nb < 0 || na >= n || nb >= n) return -1;
        if (na == nb) return 0.0;

        int[] vid = new int[n];
        Arrays.fill(vid, -1);
        int vars = 0;
        for (int id = 0; id < n; id++) if (id != nb) vid[id] = vars++;
        if (vars == 0) return 0.0;

        double[][] A = new double[vars][vars];
        double[] rhs = new double[vars];

        // 1) 把所有非电压源支路的电导 stamp 进节点方程（电压源电导为 0，自动忽略）
        for (Branch b : g.branches) {
            double gv = b.element.getConductance();
            if (gv == 0) continue;
            int a = b.nodeA, c = b.nodeB;
            if (a == c) continue;
            if (a != nb) {
                A[vid[a]][vid[a]] += gv;
                if (c != nb) A[vid[a]][vid[c]] -= gv;
            }
            if (c != nb) {
                A[vid[c]][vid[c]] += gv;
                if (a != nb) A[vid[c]][vid[a]] -= gv;
            }
        }
        // 2) 钳制 na 电位 = 1
        Arrays.fill(A[vid[na]], 0.0);
        A[vid[na]][vid[na]] = 1.0;
        rhs[vid[na]] = 1.0;

        double[] x = gaussSolve(A, rhs);
        if (x == null) return Double.MAX_VALUE; // 奇异/无通路 → 不短路

        double[] pot = new double[n];
        pot[nb] = 0.0;
        for (int id = 0; id < n; id++) if (id != nb) pot[id] = x[vid[id]];
        pot[na] = 1.0;

        // 3) 流出 na 的总电流 = 经过 na 的各支路电流之和
        double current = 0.0;
        for (Branch b : g.branches) {
            double gv = b.element.getConductance();
            if (gv == 0) continue;
            if (b.nodeA == na) current += gv * (pot[na] - pot[b.nodeB]);
            else if (b.nodeB == na) current += gv * (pot[na] - pot[b.nodeA]);
        }
        if (current <= 1e-12) return Double.MAX_VALUE;
        return 1.0 / current;
    }

    /** 简单高斯消元（部分主元），奇异返回 null。 */
    private static double[] gaussSolve(double[][] A, double[] b) {
        int n = b.length;
        for (int col = 0; col < n; col++) {
            int pivot = col;
            double max = Math.abs(A[col][col]);
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(A[r][col]) > max) { max = Math.abs(A[r][col]); pivot = r; }
            }
            if (max < 1e-15) return null;
            if (pivot != col) {
                double[] t = A[col]; A[col] = A[pivot]; A[pivot] = t;
                double tb = b[col]; b[col] = b[pivot]; b[pivot] = tb;
            }
            double piv = A[col][col];
            for (int r = col + 1; r < n; r++) {
                double f = A[r][col] / piv;
                if (f == 0) continue;
                for (int c2 = col; c2 < n; c2++) A[r][c2] -= f * A[col][c2];
                b[r] -= f * b[col];
            }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double s = b[i];
            for (int j = i + 1; j < n; j++) s -= A[i][j] * x[j];
            x[i] = s / A[i][i];
        }
        return x;
    }

    private static void shortCircuit(ServerLevel level, BlockPos p) {
        long now = System.currentTimeMillis();
        if (now - lastShortMs < 800) return; // 限流，避免每帧重复爆炸刷屏
        lastShortMs = now;
        net.minecraft.util.RandomSource rnd = level.getRandom();

        // 1) 起火：在 1~2 格范围内随机生成 1~2 个火焰方块
        int fires = 1 + rnd.nextInt(2);
        for (int k = 0; k < fires; k++) {
            BlockPos fp = randomNearby(p, rnd, 1, 2);
            if (level.getBlockState(fp).canBeReplaced()) {
                level.setBlock(fp, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                30, 0.4, 0.4, 0.4, 0.05);
        // 爆炸（NONE：不破坏方块，仅音效/粒子/冲击，fire=true 引燃）
        level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                2.0F, true, Level.ExplosionInteraction.NONE);

        // 2) 模拟爆炸破坏：1~2 格内随机破坏 1~2 个方块
        int breaks = 1 + rnd.nextInt(2);
        for (int k = 0; k < breaks; k++) {
            BlockPos bp = randomNearby(p, rnd, 1, 2);
            BlockState bs = level.getBlockState(bp);
            if (!bs.isAir() && !bs.is(Blocks.BEDROCK) && bs.getDestroySpeed(level, bp) >= 0) {
                level.destroyBlock(bp, false);
            }
        }
    }

    /** 在以 p 为中心、切比雪夫距离 [minD,maxD] 范围内随机取一个位置。 */
    private static BlockPos randomNearby(BlockPos p, net.minecraft.util.RandomSource rnd, int minD, int maxD) {
        int r = minD + rnd.nextInt(Math.max(1, maxD - minD + 1));
        int dx = rnd.nextInt(2 * r + 1) - r;
        int dz = rnd.nextInt(2 * r + 1) - r;
        int dy = rnd.nextInt(2 * r + 1) - r;
        return p.offset(dx, dy, dz);
    }

    private static void replaceWithDamaged(ServerLevel level, BlockPos p, BlockState s) {
        String name = blockPath(s.getBlock());
        Block damaged = com.realcircuit.registry.ModBlocks.ALL.get(name + "_damaged");
        if (damaged != null) {
            BlockState ds = damaged.defaultBlockState();
            // 继承原方块的朝向，避免损坏后正负极被重置为固定朝北
            if (s.hasProperty(CircuitBlock.FACING) && ds.hasProperty(CircuitBlock.FACING)) {
                ds = ds.setValue(CircuitBlock.FACING, s.getValue(CircuitBlock.FACING));
            }
            level.setBlock(p, ds, 3);
        }
    }

    private static void spawnWhiteParticles(ServerLevel level, BlockPos p) {
        double x = p.getX() + 0.5, y = p.getY() + 0.5, z = p.getZ() + 0.5;
        // 白色球形爆裂粒子（CLOUD 为白色云球），强化损坏辨识度
        level.sendParticles(ParticleTypes.CLOUD, x, y, z, 24, 0.35, 0.35, 0.35, 0.12);
    }

    // ---------- 工具 ----------
    private static String blockPath(Block b) {
        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b);
        return id == null ? "" : id.getPath();
    }

    private static String key(BlockPos p) { return p.toShortString(); }

    private static BlockPos posFromKey(String k) {
        try {
            String[] xyz = k.split(",");
            return new BlockPos(Integer.parseInt(xyz[0].trim()),
                    Integer.parseInt(xyz[1].trim()), Integer.parseInt(xyz[2].trim()));
        } catch (Exception ex) {
            return null;
        }
    }

    /** 字符串键的并查集。 */
    private static final class UnionFind {
        private final Map<String, String> parent = new HashMap<>();
        void make(String x) { parent.putIfAbsent(x, x); }
        String find(String x) {
            make(x);
            String r = x;
            while (!parent.get(r).equals(r)) r = parent.get(r);
            // 路径压缩
            while (!parent.get(x).equals(r)) { String nxt = parent.get(x); parent.put(x, r); x = nxt; }
            return r;
        }
        void union(String a, String b) {
            String ra = find(a), rb = find(b);
            if (!ra.equals(rb)) parent.put(ra, rb);
        }
    }
}

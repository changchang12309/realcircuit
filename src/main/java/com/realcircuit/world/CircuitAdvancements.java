package com.realcircuit.world;

import com.realcircuit.RealCircuitMod;
import com.realcircuit.circuit.Branch;
import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.CircuitGraph;
import com.realcircuit.circuit.CircuitSolution;
import com.realcircuit.circuit.elements.BulbElement;
import com.realcircuit.circuit.elements.SwitchElement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成就判定与授予。
 * 成就 JSON 使用 minecraft:impossible 触发器，无法自行达成；
 * 由这里在电路事件发生时手动 award（新样式数据驱动成就 + 代码判定）。
 */
public final class CircuitAdvancements {

    private CircuitAdvancements() {}

    /** 手动授予成就。 */
    public static void award(ServerPlayer player, String path) {
        var server = player.level().getServer();
        if (server == null) return;
        AdvancementHolder holder = server.getAdvancements().get(RealCircuitMod.id(path));
        if (holder != null) player.getAdvancements().award(holder, "grant");
    }

    /** 玩家放置任意电路元件 → 「真实物理电路」入门成就。 */
    public static void onElementPlaced(Level level, Player placer) {
        if (level.isClientSide() || !(placer instanceof ServerPlayer sp)) return;
        award(sp, "root");
    }

    /** 每次求解后检查电路类成就（授予给离电路最近、24 格内的玩家）。 */
    public static void onSolved(ServerLevel level, BlockPos center, CircuitGraph graph,
                                CircuitSolution sol, boolean shorted, boolean hasWire) {
        ServerPlayer player = nearest(level, center, 24.0);
        if (player == null) return;

        if (shorted) award(player, "short_circuit");

        boolean hasSource = false;
        boolean hasSwitch = false;
        List<BulbElement> litBulbs = new ArrayList<>();
        Map<BulbElement, int[]> bulbNodes = new HashMap<>();
        for (Branch b : graph.branches) {
            CircuitElement e = b.element;
            if (e.isVoltageSource() && Math.abs(e.getCurrent()) > 1e-6) hasSource = true;
            if (e instanceof SwitchElement) hasSwitch = true;
            if (e instanceof BulbElement bu && b.nodeA != b.nodeB
                    && CircuitWorldManager.bulbLightTier(bu, e.getCurrent()) >= 1) {
                litBulbs.add(bu);
                bulbNodes.put(bu, new int[]{Math.min(b.nodeA, b.nodeB), Math.max(b.nodeA, b.nodeB)});
            }
        }
        if (!hasSource) return;

        // 简易电路图：电源 + 导线 + 开关 + 至少一个被点亮的小灯泡
        if (hasWire && hasSwitch && !litBulbs.isEmpty()) award(player, "simple_circuit");

        // 串联 / 并联：两个及以上发光灯泡的拓扑关系
        for (int i = 0; i < litBulbs.size(); i++) {
            for (int j = i + 1; j < litBulbs.size(); j++) {
                BulbElement b1 = litBulbs.get(i), b2 = litBulbs.get(j);
                int[] n1 = bulbNodes.get(b1), n2 = bulbNodes.get(b2);
                if (n1[0] == n2[0] && n1[1] == n2[1]) {
                    // 接在同一对节点之间 → 并联
                    award(player, "parallel_circuit");
                } else {
                    // 电流相同（容差 2%）但不在同一对节点 → 串联
                    double i1 = Math.abs(b1.getCurrent()), i2 = Math.abs(b2.getCurrent());
                    double tol = 0.02 * Math.max(Math.max(i1, i2), 1e-9);
                    if (Math.abs(i1 - i2) <= tol) award(player, "series_circuit");
                }
            }
        }
    }

    private static ServerPlayer nearest(ServerLevel level, BlockPos center, double maxDist) {
        ServerPlayer best = null;
        double bestD = maxDist * maxDist;
        for (ServerPlayer sp : level.players()) {
            double d = sp.distanceToSqr(center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5);
            if (d <= bestD) { bestD = d; best = sp; }
        }
        return best;
    }
}

package com.realcircuit.circuit;

import java.util.ArrayList;
import java.util.List;

/** 求解结果封装，供世界层读取每个元件的电压/电流并做保护判定。 */
public final class CircuitSolution {
    public final CircuitGraph graph;
    public final boolean solved;

    public CircuitSolution(CircuitGraph g) {
        this.graph = g;
        this.solved = true;
    }

    public List<CircuitElement> elements() {
        List<CircuitElement> out = new ArrayList<>();
        for (Branch b : graph.branches) out.add(b.element);
        return out;
    }

    /** 是否存在电源直接短路（电压源两端被近零电阻回路短接）。 */
    public boolean hasShortCircuit() {
        for (Branch b : graph.activeBranches()) {
            if (b.element.isVoltageSource()) {
                double r = 1.0 / Math.max(b.element.getConductance(), 1e-12);
                // 电压源支路电流若异常大（>1e4 A）视为短路
                if (Math.abs(b.element.getCurrent()) > 1e4) return true;
            }
        }
        return false;
    }
}

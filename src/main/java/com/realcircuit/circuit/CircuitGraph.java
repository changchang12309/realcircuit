package com.realcircuit.circuit;

import java.util.ArrayList;
import java.util.List;

/**
 * 电路图：节点列表 + 支路列表。端子通过并查集合并成节点（导线连通 = 同节点）。
 * 这是求解器的输入。由世界层从方块搭建而成。
 */
public class CircuitGraph {
    public final List<Node> nodes = new ArrayList<>();
    public final List<Branch> branches = new ArrayList<>();

    /** 仅保留两端子都已连接且元件导通的支路，返回有效支路列表（求解用）。 */
    public List<Branch> activeBranches() {
        List<Branch> out = new ArrayList<>();
        for (Branch b : branches) {
            if (b.nodeA >= 0 && b.nodeB >= 0 && b.element.isConducting()) out.add(b);
        }
        return out;
    }

    public int nodeCount() { return nodes.size(); }
}

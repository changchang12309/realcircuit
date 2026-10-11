package com.realcircuit.circuit;

import java.util.ArrayList;
import java.util.List;

/** 电气节点：多个端子通过导线连接到同一电位点。 */
public final class Node {
    public final int id;
    /** 连到该节点的端子（来自各元件） */
    public final List<Terminal> terminals = new ArrayList<>();
    /** 求解后的节点电位（相对参考地） */
    public double potential = 0;

    public Node(int id) { this.id = id; }
}

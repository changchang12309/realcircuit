package com.realcircuit.circuit;

/** 一条支路：元件的两个端子所连接的两个节点构成一条二端元件支路。 */
public final class Branch {
    public final CircuitElement element;
    public final int nodeA; // pos 端节点
    public final int nodeB; // neg 端节点

    public Branch(CircuitElement e, int nodeA, int nodeB) {
        this.element = e;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }
}

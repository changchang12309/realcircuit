package com.realcircuit.circuit;

/**
 * 元件的一个接线端子。连接到红石粉/导线时并入对应电气节点。
 * polarity: + / - / NONE（红石块无极性）
 */
public final class Terminal {
    public enum Polarity { POS, NEG, NONE }

    public final CircuitElement owner;
    public final Polarity polarity;
    public int nodeId = -1;   // 所连接的电气节点 id，-1 表示未连接

    public Terminal(CircuitElement owner, Polarity polarity) {
        this.owner = owner;
        this.polarity = polarity;
    }

    public boolean isConnected() {
        return nodeId >= 0;
    }
}

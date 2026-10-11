package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/**
 * 小灯泡：纯电阻（阻值由额定电压/电流算出 R=U/I），亮度 ∝ 实际电流。
 * 过流（>额定电流的一定倍数）由外层判定 → 替换为损坏方块。
 */
public class BulbElement extends CircuitElement {
    public final double ratedU;
    public final double ratedI;
    public final double resistance;
    public BulbElement(String id, double ratedU, double ratedI) {
        super(id, Terminal.Polarity.NONE, Terminal.Polarity.NONE);
        this.ratedU = ratedU;
        this.ratedI = ratedI;
        this.resistance = Math.max(ratedU / Math.max(ratedI, 1e-6), 1e-6);
    }
    @Override public double getConductance() { return 1.0 / resistance; }
}

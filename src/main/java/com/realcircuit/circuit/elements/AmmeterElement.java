package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/**
 * 电流表：理想串联（R≈0），测支路电流。带极性：电流须正进负出，
 * 接反时读数显示负数（电流方向相反）。超量程由外层判定损坏。
 */
public class AmmeterElement extends CircuitElement {
    public AmmeterElement(String id) {
        super(id, Terminal.Polarity.POS, Terminal.Polarity.NEG);
    }
    @Override public double getConductance() { return 1e5; } // 近似短路
}

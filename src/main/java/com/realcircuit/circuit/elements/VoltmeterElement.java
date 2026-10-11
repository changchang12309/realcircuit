package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/** 电压表：理想并联（R≈∞），测两端电压。带极性，接反显示负数。 */
public class VoltmeterElement extends CircuitElement {
    public VoltmeterElement(String id) {
        super(id, Terminal.Polarity.POS, Terminal.Polarity.NEG);
    }
    @Override public double getConductance() { return 1e-7; } // 近似开路
}

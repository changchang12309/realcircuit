package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/** 红石粉导线：极小电阻，近似等电位但保留有限电导避免奇异矩阵。 */
public class WireElement extends CircuitElement {
    public static final double WIRE_R = 0.05; // 欧
    public WireElement(String id) {
        super(id, Terminal.Polarity.NONE, Terminal.Polarity.NONE);
    }
    @Override public double getConductance() { return 1.0 / WIRE_R; }
}

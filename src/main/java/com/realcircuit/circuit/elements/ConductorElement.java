package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/**
 * 导体方块（铁块/铜块）：小电阻。铜块电阻随铜锈等级增大（锈值由外层传入）。
 */
public class ConductorElement extends CircuitElement {
    public final double resistance;
    public ConductorElement(String id, double r) {
        super(id, Terminal.Polarity.NONE, Terminal.Polarity.NONE);
        this.resistance = Math.max(r, 1e-4);
    }
    @Override public double getConductance() { return 1.0 / resistance; }
}

package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/** 单刀开关：闭合 R≈0 导通，断开不参与求解。 */
public class SwitchElement extends CircuitElement {
    public final boolean closed;
    public SwitchElement(String id, boolean closed) {
        super(id, Terminal.Polarity.NONE, Terminal.Polarity.NONE);
        this.closed = closed;
    }
    @Override public boolean isConducting() { return closed; }
    @Override public double getConductance() { return closed ? 1e4 : 0; }
}

package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/** 定值电阻：纯电导 G=1/R。损坏版由外层以"已熔断/断开"处理（不参与或 R→∞）。 */
public class ResistorElement extends CircuitElement {
    public final double resistance;
    public ResistorElement(String id, double r) {
        super(id, Terminal.Polarity.NONE, Terminal.Polarity.NONE);
        this.resistance = Math.max(r, 1e-6);
    }
    @Override public double getConductance() { return 1.0 / resistance; }
}

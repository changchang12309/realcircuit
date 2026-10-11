package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/**
 * 保险丝：小电阻，电流超过额定值即熔断（永久断开）。
 * 熔断后 isConducting=false，等效开路；外层据此替换为熔断损坏方块。
 */
public class FuseElement extends CircuitElement {
    public static final double R = 0.1;
    public final double ratedI;
    public boolean blown = false;
    public FuseElement(String id, double ratedI) {
        super(id, Terminal.Polarity.NONE, Terminal.Polarity.NONE);
        this.ratedI = ratedI;
    }
    @Override public boolean isConducting() { return !blown; }
    @Override public double getConductance() { return blown ? 0 : 1.0 / R; }
}

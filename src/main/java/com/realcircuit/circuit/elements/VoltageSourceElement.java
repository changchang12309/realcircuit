package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/**
 * 直流电压源（含学生电源、直流电源、红石块）。
 * 红石块电压固定 220V 且不可串联叠加——叠加逻辑在世界构建层处理，
 * 这里每个电源方块各自 stamp 一个电压源，串联时由拓扑自然叠加。
 * 极性：pos(+) → neg(-)，电压升为 V。
 */
public class VoltageSourceElement extends CircuitElement {
    public final double voltage;
    public VoltageSourceElement(String id, double v) {
        super(id, Terminal.Polarity.POS, Terminal.Polarity.NEG);
        this.voltage = v;
    }
    @Override public boolean isVoltageSource() { return true; }
    @Override public double getSourceVoltage() { return voltage; }
}

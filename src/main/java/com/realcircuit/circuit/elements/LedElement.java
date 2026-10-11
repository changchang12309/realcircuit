package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/**
 * 发光二极管 LED：简化两段模型（用户硬性规定，不用迭代）。
 *  - 正向（+ 端电位高于 - 端，且满足 Vf 条件）：等效为 Vf 电压源 + 内阻 Rf 串联，导通发光；
 *  - 反向：直接断路。
 * 由于不做迭代，导通状态由求解前的"电位预判"决定：先按开路求解一次预估极性，
 * 正向则在二次 stamp 时以 Vf+Rf 参与。具体由 DcSolver 处理两段。
 */
public class LedElement extends CircuitElement {
    public static final double VF = 2.0;   // 正向压降
    public static final double RF = 10.0;  // 内阻
    public boolean forwardBiased = false;  // 求解器预判后填写

    public LedElement(String id) {
        super(id, Terminal.Polarity.POS, Terminal.Polarity.NEG);
    }
    // 正向时当作 Vf 电压源 + Rf；这里给一个导通电导，电压源 stamp 由 solver 处理
    @Override public double getConductance() { return forwardBiased ? 1.0 / RF : 0; }
    @Override public boolean isVoltageSource() { return forwardBiased; }
    @Override public double getSourceVoltage() { return forwardBiased ? VF : 0; }
    @Override public boolean isConducting() { return forwardBiased; }
}

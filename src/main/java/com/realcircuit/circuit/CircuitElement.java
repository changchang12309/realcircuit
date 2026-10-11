package com.realcircuit.circuit;

import java.util.List;

/**
 * 电路元件的电学模型基类（与 MC 完全解耦的纯数据/逻辑层）。
 * 一个元件有 2 个端子，连接成一条支路(Branch)。
 *
 * 线性电阻类元件实现 getConductance()；
 * 电压源/电表类通过 stamp() 参与 MNA 矩阵；
 * 求解后通过 onSolved() 产生副作用（亮度、损坏判定由外层处理）。
 */
public abstract class CircuitElement {

    /** 元件在世界中的唯一标识（通常是方块位置的字符串编码），用于回写结果 */
    public final String id;
    public final Terminal pos;   // 正端（或 NONE）
    public final Terminal neg;   // 负端（或 NONE）

    protected double voltage = 0;   // 两端电压（pos 相对于 neg），求解后填写
    protected double current = 0;   // 从 pos 流向 neg 的电流，求解后填写

    public CircuitElement(String id, Terminal.Polarity p1, Terminal.Polarity p2) {
        this.id = id;
        this.pos = new Terminal(this, p1);
        this.neg = new Terminal(this, p2);
    }

    public List<Terminal> terminals() {
        return List.of(pos, neg);
    }

    public double getVoltage() { return voltage; }
    public double getCurrent() { return current; }
    public void setSolved(double v, double i) { this.voltage = v; this.current = i; }

    /** 该元件是否导通参与求解（断开的开关、反向LED、已熔断保险丝返回 false） */
    public boolean isConducting() { return true; }

    /** 线性电导 G=1/R（西门子）。电压源/理想电表返回 0 并改用 stamp()。 */
    public double getConductance() { return 0; }

    /** 是否为独立电压源 */
    public boolean isVoltageSource() { return false; }
    public double getSourceVoltage() { return 0; }

    /** 求解完成回调（可用于更新亮度等，本层只记录电学量） */
    public void onSolved() {}
}

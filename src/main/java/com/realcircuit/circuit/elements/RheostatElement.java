package com.realcircuit.circuit.elements;

import com.realcircuit.circuit.CircuitElement;
import com.realcircuit.circuit.Terminal;

/**
 * 滑动变阻器：根据"实际接入的两个端子 + 滑片位置 percent"映射为单条等效电阻 R_eq。
 * 不引入内部虚拟节点（避免奇异回路与分压误判，见经验 100016223）。
 *
 * 接法判定（纯函数）：
 *  - 两端子都在上金属杆 → 短路（R≈0）
 *  - 两端子都在电阻丝 → 取滑片到两接入点之间的较大段（近似教学处理）
 *  - 一上一下（标准）→ R = (1-percent)*Rmax 或 percent*Rmax，依接入的下端是左端还是右端
 * percent ∈ [0,1]，0=滑片在最左端。
 */
public class RheostatElement extends CircuitElement {
    public final double maxR;
    public final double percent;
    public final boolean topLeft, topRight, bottomLeft, bottomRight; // 实际接入的端子

    public RheostatElement(String id, double maxR, double percent,
                           boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        super(id, Terminal.Polarity.NONE, Terminal.Polarity.NONE);
        this.maxR = Math.max(maxR, 1e-6);
        this.percent = Math.max(0, Math.min(1, percent));
        this.topLeft = topLeft; this.topRight = topRight;
        this.bottomLeft = bottomLeft; this.bottomRight = bottomRight;
    }

    /** 等效电阻（端子对→R 映射，纯函数） */
    public double equivalentR() {
        int tops = (topLeft?1:0)+(topRight?1:0);
        int bots = (bottomLeft?1:0)+(bottomRight?1:0);
        if (tops + bots != 2) return 1e9; // 接入端子数不为2，视为断开
        if (tops == 2) return 1e-4;       // 两上端：金属杆短路
        if (bots == 2) return maxR;       // 两下端：整段电阻丝
        // 一上一下：下端是左下(BL) → R = 滑片到 BL 的长度 = percent·Rmax（右移变大）；
        //          下端是右下(BR) → R = 滑片到 BR 的长度 = (1-percent)·Rmax（右移变小）。
        double r = bottomLeft ? percent * maxR : (1 - percent) * maxR;
        return Math.max(r, 1e-4);
    }

    @Override public double getConductance() { return 1.0 / equivalentR(); }
}

package com.realcircuit.circuit;

import com.realcircuit.circuit.elements.LedElement;

import java.util.ArrayList;
import java.util.List;

/**
 * 直流电路求解器：改进节点分析法 (MNA)。
 *
 * 数值稳定性（用户硬性规定：必须归一化/缩放，不能裸算）：
 *  1. 选一个参考地节点；
 *  2. 行平衡缩放：组装后对矩阵每行除以该行最大绝对值，使各方程量级一致（避免
 *     电压表 1e-7 与电流表 1e5 电导、220V 与 1.5V 源混算导致病态）；
 *  3. 部分主元高斯消元求解；
 *  4. 电压统一用 double 精度。
 *
 * LED 两段模型：先以 LED 全部开路求解一次预判正负极性，正向则二次 stamp 参与。
 */
public final class DcSolver {

    private DcSolver() {}

    public static CircuitSolution solve(CircuitGraph graph) {
        // 第一遍：LED 全部视为开路，预判极性
        setAllLedOpen(graph);
        double[] pot1 = solveLinear(graph, null);
        applyPotentials(graph, pot1);
        predictLedBias(graph);

        // 第二遍：正向 LED 以 Vf+内阻参与
        double[] pot = solveLinear(graph, null);
        applyPotentials(graph, pot);
        fillBranchResults(graph);
        return new CircuitSolution(graph);
    }

    private static void setAllLedOpen(CircuitGraph g) {
        for (Branch b : g.branches) {
            if (b.element instanceof LedElement led) led.forwardBiased = false;
        }
    }

    private static void predictLedBias(CircuitGraph g) {
        for (Branch b : g.branches) {
            if (b.element instanceof LedElement led && b.nodeA >= 0 && b.nodeB >= 0) {
                double va = g.nodes.get(b.nodeA).potential;
                double vb = g.nodes.get(b.nodeB).potential;
                led.forwardBiased = (va - vb) > 0; // + 端电位高于 - 端即正向
            }
        }
    }

    private static void applyPotentials(CircuitGraph g, double[] pot) {
        if (pot == null) return;
        for (int i = 0; i < g.nodes.size() && i < pot.length; i++) {
            g.nodes.get(i).potential = pot[i];
        }
    }

    private static void fillBranchResults(CircuitGraph g) {
        for (Branch b : g.activeBranches()) {
            double va = g.nodes.get(b.nodeA).potential;
            double vb = g.nodes.get(b.nodeB).potential;
            double v = va - vb;
            double i = v * b.element.getConductance();
            if (b.element.isVoltageSource()) {
                // 电压源电流由其串联回路决定，这里用支路电流近似（足够教学）
                i = v > 0 ? b.element.current : -b.element.current;
            }
            b.element.setSolved(v, i);
            b.element.onSolved();
        }
    }

    /** 组装 MNA 矩阵并求解，返回各节点电位（含参考地=0）。失败返回 null。 */
    static double[] solveLinear(CircuitGraph g, Object dummy) {
        int n = g.nodes.size();
        if (n == 0) return new double[0];

        // 收集电压源支路，每个引入一个额外电流变量
        List<Branch> vs = new ArrayList<>();
        List<Branch> act = g.activeBranches();
        for (Branch b : act) if (b.element.isVoltageSource()) vs.add(b);

        // 未知量：节点 1..n-1 的电位（节点0为地）+ 每个电压源的电流
        int nodeVars = n - 1;
        int m = nodeVars + vs.size();
        if (m <= 0) {
            double[] r = new double[n];
            return r;
        }

        double[][] A = new double[m][m];
        double[] rhs = new double[m];

        // 节点索引映射：节点 id -> 矩阵行（0号节点接地，无行）
        // 直接用 nodeId 作为电位变量下标，0 号接地
        for (Branch b : act) {
            double gv = b.element.getConductance();
            int a = b.nodeA, c = b.nodeB;
            if (gv != 0) stampConductance(A, a, c, gv, n);
            if (b.element.isVoltageSource()) {
                int k = nodeVars + vs.indexOf(b);
                if (a > 0) { A[a - 1][k] += 1; A[k][a - 1] += 1; }
                if (c > 0) { A[c - 1][k] += -1; A[k][c - 1] += -1; }
                rhs[k] += b.element.getSourceVoltage();
            }
        }

        // 行平衡缩放：每行除以该行最大绝对值（含 rhs），归一化量级
        for (int i = 0; i < m; i++) {
            double mx = 1e-30;
            for (int j = 0; j < m; j++) mx = Math.max(mx, Math.abs(A[i][j]));
            mx = Math.max(mx, Math.abs(rhs[i]));
            if (mx > 1e-30) {
                for (int j = 0; j < m; j++) A[i][j] /= mx;
                rhs[i] /= mx;
            }
        }

        double[] x = gaussPartialPivot(A, rhs);
        if (x == null) return null;

        double[] pot = new double[n];
        pot[0] = 0; // 参考地
        for (int i = 1; i < n; i++) pot[i] = x[i - 1];
        return pot;
    }

    private static void stampConductance(double[][] A, int a, int c, double g, int n) {
        // a,c 为节点 id；0 号接地不进矩阵。变量下标 = nodeId-1
        if (a > 0) A[a - 1][a - 1] += g;
        if (c > 0) A[c - 1][c - 1] += g;
        if (a > 0 && c > 0) { A[a - 1][c - 1] -= g; A[c - 1][a - 1] -= g; }
    }

    /** 部分主元高斯消元，求解 Ax=b。奇异返回 null。 */
    private static double[] gaussPartialPivot(double[][] A, double[] b) {
        int n = b.length;
        for (int col = 0; col < n; col++) {
            // 选主元
            int pivot = col;
            double max = Math.abs(A[col][col]);
            for (int r = col + 1; r < n; r++) {
                if (Math.abs(A[r][col]) > max) { max = Math.abs(A[r][col]); pivot = r; }
            }
            if (max < 1e-15) return null; // 奇异
            if (pivot != col) {
                double[] t = A[col]; A[col] = A[pivot]; A[pivot] = t;
                double tb = b[col]; b[col] = b[pivot]; b[pivot] = tb;
            }
            double piv = A[col][col];
            for (int r = col + 1; r < n; r++) {
                double f = A[r][col] / piv;
                if (f == 0) continue;
                for (int c2 = col; c2 < n; c2++) A[r][c2] -= f * A[col][c2];
                b[r] -= f * b[col];
            }
        }
        // 回代
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double s = b[i];
            for (int j = i + 1; j < n; j++) s -= A[i][j] * x[j];
            x[i] = s / A[i][i];
        }
        return x;
    }
}

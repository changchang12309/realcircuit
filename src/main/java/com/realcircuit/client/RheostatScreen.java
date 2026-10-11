package com.realcircuit.client;

import com.realcircuit.network.RheostatUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 滑动变阻器调节界面：
 *  - 顶部 4 个接线柱按钮（左上/右上/左下/右下），点击选择"接入的两个端子"。
 *    已选两个后再点第三个 → 保留较早点击的、替换最后点击的（教学标准操作）。
 *  - 下方滑块调节滑片 percent。
 *  - 实时显示当前接法对应的等效电阻；改动即同步到服务端。
 * 端子索引：0=左上(TL) 1=右上(TR) 2=左下(BL) 3=右下(BR)。
 */
public class RheostatScreen extends Screen {
    private static final String[] TERM_NAMES = {"左上", "右上", "左下", "右下"};
    private static final int[] TERM_BIT = {1, 2, 4, 8};

    private final BlockPos pos;
    private final double maxR;
    private double percent;
    /** 按点击顺序保存的已选端子（容量 2）。 */
    private final List<Integer> picks = new ArrayList<>();

    private AbstractSliderButton slider;
    private final Button[] termButtons = new Button[4];
    /** 实时显示当前接线对应的等效电阻（可靠渲染的控件，每帧更新）。 */
    private EditBox rBox;

    public RheostatScreen(BlockPos pos, double maxR, double initialPercent, int connMask) {
        super(Component.literal("滑动变阻器"));
        this.pos = pos;
        this.maxR = maxR;
        this.percent = Math.max(0, Math.min(1, initialPercent));
        // 回显已保存的接线：恰好 2 个有效端子时恢复选择；否则保持全部未连接（初始态不自动连接）
        for (int i = 0; i < 4; i++) if ((connMask & TERM_BIT[i]) != 0) picks.add(i);
        if (picks.size() != 2) picks.clear();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        // 4 个接线柱按钮（2x2）
        int bw = 70, bh = 20;
        int by = 40, bx = cx - bw - 6;
        int[][] layout = {{bx, by}, {bx + bw + 12, by}, {bx, by + bh + 8}, {bx + bw + 12, by + bh + 8}};
        for (int i = 0; i < 4; i++) {
            final int t = i;
            termButtons[i] = Button.builder(Component.literal(termLabel(i)), b -> clickTerminal(t))
                    .bounds(layout[i][0], layout[i][1], bw, bh).build();
            addRenderableWidget(termButtons[i]);
        }
        // 滑片滑块
        slider = new AbstractSliderButton(cx - 100, this.height / 2 + 10, 200, 20,
                Component.literal(sliderLabel()), percent) {
            @Override protected void applyValue() {
                percent = this.value;
                setMessage(Component.literal(sliderLabel()));
                sync();
            }
            @Override protected void updateMessage() { setMessage(Component.literal(sliderLabel())); }
        };
        addRenderableWidget(slider);
        // 电阻显示框：只读、居中、每帧刷新为当前接法的等效电阻
        rBox = new EditBox(this.getFont(), cx - 100, this.height / 2 + 40, 200, 18,
                Component.literal("当前电阻"));
        rBox.setEditable(false);
        rBox.setValue(rText());
        addRenderableWidget(rBox);
    }

    /** 点击接线柱：选满两个后，替换最后点击的、保留较早的。 */
    private void clickTerminal(int t) {
        if (picks.size() < 2) {
            if (!picks.contains(t)) picks.add(t);
        } else {
            if (picks.contains(t)) return;          // 已是接入端子，忽略
            picks.remove(1);                         // 断开最后点击的
            picks.add(t);                            // 新端子与较早的接入
        }
        refreshTermLabels();
        sync();
    }

    private void refreshTermLabels() {
        for (int i = 0; i < 4; i++)
            if (termButtons[i] != null) termButtons[i].setMessage(Component.literal(termLabel(i)));
    }

    private String termLabel(int i) {
        return (picks.contains(i) ? "● " : "○ ") + TERM_NAMES[i];
    }

    private int connMask() {
        int m = 0;
        for (int i : picks) m |= TERM_BIT[i];
        return m;
    }

    private String sliderLabel() {
        return String.format("滑片 %.0f%%", percent * 100);
    }

    /** 当前接法的等效电阻（与 RheostatElement.equivalentR 保持一致，仅用于显示）。 */
    private String rText() {
        boolean tl = picks.contains(0), tr = picks.contains(1), bl = picks.contains(2), br = picks.contains(3);
        int tops = (tl ? 1 : 0) + (tr ? 1 : 0);
        int bots = (bl ? 1 : 0) + (br ? 1 : 0);
        if (tops + bots != 2) return "未接好（开路）";
        if (tops == 2) return "≈ 0 Ω（金属杆短路）";
        if (bots == 2) return String.format("= %.0f Ω（整段电阻丝）", maxR);
        double r = bl ? percent * maxR : (1 - percent) * maxR;
        return String.format("≈ %.1f Ω", Math.max(r, 1e-4));
    }

    /** 发到服务端写回方块实体并重新求解。 */
    private void sync() {
        ClientPlayNetworking.send(new RheostatUpdatePayload(pos, (float) percent, connMask()));
    }

    @Override
    public void removed() {
        sync();
        super.removed();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // 每帧刷新电阻显示框，确保随滑片/接线实时变化
        if (rBox != null) rBox.setValue(rText());
        // 26.2：super 已负责背景（含模糊），勿再手动 extractBackground
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.getFont(), this.getTitle(), this.width / 2, 14, 0xFFFFFF);
        g.centeredText(this.getFont(), "点击两个接线柱选择接入端；选满后点第三个 = 替换最后接入的",
                this.width / 2, 28, 0xA0A0A0);
        g.centeredText(this.getFont(), String.format("最大阻值 %.0f Ω —— 下方为当前接入电阻（随接线与滑片实时变化）", maxR),
                this.width / 2, this.height / 2 - 8, 0xFFE080);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

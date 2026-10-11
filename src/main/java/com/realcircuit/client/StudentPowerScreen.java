package com.realcircuit.client;

import com.realcircuit.network.StudentPowerUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * 学生电源调节界面。
 * 联动规则（用户确认）：
 *  - 输入框输入完小数后回车 → 滑动条同步到对应位置；
 *  - 滑动条滑动时 → 直接清空输入框。
 * 电压范围 0~220V，两位小数。
 *
 * 注：本类只负责客户端 UI 与数值联动；把最终电压写回服务端方块实体的网络同步，
 *     待进游戏联调时补齐（需 Fabric networking payload）。
 */
public class StudentPowerScreen extends Screen {
    private static final double MIN = 0.0, MAX = 220.0;

    private final BlockPos pos;
    private double voltage;      // 当前电压（伏）
    private EditBox input;       // 输入框
    private AbstractSliderButton slider;

    public StudentPowerScreen(BlockPos pos, double initialVoltage) {
        super(Component.literal("学生电源调节"));
        this.pos = pos;
        this.voltage = clamp(initialVoltage);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        // 滑动条：0~220，步进 1V（需求：滑动条 1V 步进）
        slider = new AbstractSliderButton(cx - 100, this.height / 2 - 10, 200, 20,
                Component.literal(String.format("%.1f V", voltage)),
                (voltage - MIN) / (MAX - MIN)) {
            @Override
            protected void applyValue() {
                // 步进 1V
                double raw = MIN + this.value * (MAX - MIN);
                voltage = Math.round(raw);
                updateMessage();
                // 滑动条滑动时 → 清空输入框
                if (input != null) input.setValue("");
                sync();
            }
            @Override
            protected void updateMessage() {
                setMessage(Component.literal(String.format("%.1f V", voltage)));
            }
        };
        addRenderableWidget(slider);

        // 输入框：允许两位小数（26.2 无 setFilter，提交时校验）
        input = new EditBox(this.getFont(), cx - 50, this.height / 2 + 20, 100, 18, Component.literal("电压"));
        input.setMaxLength(8);
        addRenderableWidget(input);
    }

    /** 输入框提交（回车）时：解析小数 → 同步电压。 */
    private void commitInput() {
        if (input == null) return;
        String s = input.getValue().trim();
        if (s.isEmpty()) return;
        try {
            double v = clamp(Double.parseDouble(s));
            this.voltage = v;
            // 同步滑动条显示
            if (slider != null) slider.setMessage(Component.literal(String.format("%.1f V", voltage)));
            sync();
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == 257 /* ENTER */ || e.key() == 335 /* NUMPAD_ENTER */) {
            commitInput();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public void removed() {
        commitInput(); // 关闭界面时也提交一次
        sync();
        super.removed();
    }

    /** 发到服务端写回方块实体（持久化），下次打开即显示最新值。 */
    private void sync() {
        ClientPlayNetworking.send(new StudentPowerUpdatePayload(pos, (float) voltage));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        // 26.2：super 已负责背景（含模糊），勿再手动 extractBackground，否则 "blur once per frame"
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(this.getFont(), this.getTitle(), this.width / 2, 20, 0xFFFFFF);
        g.centeredText(this.getFont(), "输入小数后回车 → 滑条同步；拖动滑条 → 清空输入",
                this.width / 2, this.height / 2 + 50, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static double clamp(double v) {
        return Math.max(MIN, Math.min(MAX, Math.round(v * 100) / 100.0));
    }
}

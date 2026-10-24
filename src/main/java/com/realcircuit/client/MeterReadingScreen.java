package com.realcircuit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 电表读数界面：右键电表弹出，用 EditBox 控件显示示数（保证渲染可见）。 */
public class MeterReadingScreen extends Screen {
    private final boolean ammeter;
    private final double reading;
    private final boolean damaged;
    private final int range;
    private EditBox valueBox;

    public MeterReadingScreen(boolean ammeter, double reading, boolean damaged, int range) {
        super(Component.literal(ammeter ? "电流表读数" : "电压表读数"));
        this.ammeter = ammeter;
        this.reading = reading;
        this.damaged = damaged;
        this.range = range;
    }

    private String rangeLabel() {
        if (ammeter) return range == 0 ? "小量程 0–0.6 A（高精度）" : "大量程 0–3 A";
        return range == 0 ? "小量程 0–3 V（高精度）" : "大量程 0–15 V";
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // 用输入框控件显示示数（控件渲染可靠，文字一定可见）
        valueBox = new EditBox(this.getFont(), cx - 90, cy - 10, 180, 24,
                Component.literal(damaged ? "已损坏" : "示数"));
        valueBox.setMaxLength(16);
        valueBox.setEditable(false);
        valueBox.setTextColor(0xFFFFFF); // 文字全部白色
        if (damaged) {
            valueBox.setValue("已损坏（超量程）");
        } else {
            String unit = ammeter ? " A" : " V";
            // 小量程高精度 3 位小数，大量程 2 位小数
            String fmt = range == 0 ? "%.3f" : "%.2f";
            valueBox.setValue(String.format(fmt, reading) + unit);
        }
        addRenderableWidget(valueBox);
        setInitialFocus(valueBox);

        // 量程标签
        EditBox rangeBox = new EditBox(this.getFont(), cx - 90, cy - 40, 180, 20,
                Component.literal("量程"));
        rangeBox.setMaxLength(32);
        rangeBox.setEditable(false);
        rangeBox.setTextColor(0xFFFFFF);
        rangeBox.setValue(damaged ? "—" : rangeLabel());
        addRenderableWidget(rangeBox);

        addRenderableWidget(Button.builder(Component.literal("关闭"), b -> this.onClose())
                .bounds(cx - 50, cy + 30, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        // 面板背景：纯黑（按钮为原版浅灰样式，文字均为白色）
        // 注意：必须先画底色再调 super，否则黑底会盖住 EditBox/按钮
        g.fill(cx - 120, this.height / 2 - 55, cx + 120, this.height / 2 + 65, 0xFF000000);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

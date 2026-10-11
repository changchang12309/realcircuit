package com.realcircuit;

import com.realcircuit.block.MeterBlock;
import com.realcircuit.block.RheostatBlock;
import com.realcircuit.block.StudentPowerBlock;
import com.realcircuit.blockentity.MeterBlockEntity;
import com.realcircuit.blockentity.RheostatBlockEntity;
import com.realcircuit.blockentity.StudentPowerBlockEntity;
import com.realcircuit.client.MeterReadingScreen;
import com.realcircuit.client.RheostatScreen;
import com.realcircuit.client.StudentPowerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 客户端入口：注册 Screen 打开逻辑、渲染等。 */
public class RealCircuitClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 右键滑动变阻器/学生电源 → 打开对应调节界面（客户端）
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide()) return InteractionResult.PASS;
            var state = level.getBlockState(hit.getBlockPos());
            var pos = hit.getBlockPos();
            if (state.getBlock() instanceof MeterBlock mb) {
                BlockPos master = pos;
                if (mb.multiblock) {
                    Direction facing = state.getValue(MeterBlock.FACING);
                    int part = state.getValue(MeterBlock.PART);
                    master = MeterBlock.masterPos(pos, facing, part);
                }
                double reading = 0.0;
                int range = 0;
                BlockEntity be = level.getBlockEntity(master);
                if (be instanceof MeterBlockEntity me) { reading = me.reading; range = me.range; }
                Minecraft.getInstance().setScreenAndShow(
                        new MeterReadingScreen(mb.isAmmeter, reading, MeterBlock.isDamaged(state), range));
                return InteractionResult.SUCCESS;
            }
            if (state.getBlock() instanceof StudentPowerBlock) {
                double v = 6.0;
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StudentPowerBlockEntity sp) v = sp.voltage;
                Minecraft.getInstance().setScreenAndShow(new StudentPowerScreen(pos, v));
                return InteractionResult.SUCCESS;
            }
            if (state.getBlock() instanceof RheostatBlock rh) {
                double pct = 0.5;
                int connMask = 0;
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof RheostatBlockEntity re) { pct = re.percent; connMask = re.connMask; }
                Minecraft.getInstance().setScreenAndShow(new RheostatScreen(pos, rh.maxR, pct, connMask));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        // 物品 tooltip 统一标注电阻参数
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            String r = resistanceOf(path);
            if (r != null) lines.add(Component.literal(r).withStyle(ChatFormatting.GRAY));
        });

        RealCircuitMod.LOGGER.info("[真实物理电路] 客户端初始化完成");
    }

    /** 根据物品 id 返回电阻标注文本；非本模组物品返回 null。 */
    private static String resistanceOf(String path) {
        if (path == null) return null;
        if (path.equals("copper_wire")) return "电阻：0Ω";
        if (path.startsWith("resistor_")) {
            if (path.endsWith("_damaged")) return "电阻：∞Ω（已损坏）";
            return "电阻：" + path.substring("resistor_".length()) + "Ω";
        }
        if (path.startsWith("rheostat_")) {
            return "电阻：0~" + path.substring("rheostat_".length()) + "Ω（可调）";
        }
        if (path.startsWith("ammeter_")) return "电阻：≈0Ω";
        if (path.startsWith("voltmeter_")) return "电阻：10MΩ";
        if (path.equals("led")) return "电阻：10Ω（正向导通时）";
        if (path.startsWith("power_dc_") || path.equals("power_student")) return "内阻：≈0Ω";
        if (path.equals("switch")) return "电阻：≈0Ω（闭合时）";
        if (path.equals("fuse")) return "电阻：0.1Ω";
        if (path.equals("fuse_damaged")) return "电阻：∞Ω（已熔断）";
        if (path.startsWith("bulb_")) {
            if (path.endsWith("_damaged")) return "电阻：∞Ω（已损坏）";
            if (path.equals("bulb_0_3v_0_2a")) return "电阻：1.5Ω";
            if (path.equals("bulb_2_5v_0_3a")) return "电阻：8.3Ω";
            if (path.equals("bulb_3_8v_0_3a")) return "电阻：12.7Ω";
        }
        return null;
    }
}

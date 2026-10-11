package com.realcircuit.registry;

import com.realcircuit.RealCircuitMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式分类：单一分类"【新的 MC 电路元件】"，包含全部元件。
 * 26.2：CreativeModeTab.builder(Row, int)，BuiltInRegistries.CREATIVE_MODE_TAB。
 */
public class ModTabs {
    public static CreativeModeTab CIRCUIT_TAB;

    public static void register() {
        CIRCUIT_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                RealCircuitMod.id("circuit"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.realcircuit.circuit"))
                        .icon(() -> new ItemStack(ModBlocks.RESISTOR.get(100).asItem()))
                        .displayItems((params, output) -> {
                            for (var b : ModBlocks.ALL.values()) output.accept(b.asItem());
                        })
                        .build());
    }
}

package com.realcircuit.registry;

/** 物品注册：每个方块配一个 BlockItem（在 RegistrationHelper 中完成）。 */
public class ModItems {
    public static void register() {
        RegistrationHelper.registerBlockItems(ModBlocks.ALL);
    }
}

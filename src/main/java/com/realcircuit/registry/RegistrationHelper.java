package com.realcircuit.registry;

import com.realcircuit.RealCircuitMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * 集中注册方块与对应物品。
 * 26.2 官方映射：Registry.register(BuiltInRegistries.BLOCK, id, block)；
 * Identifier.fromNamespaceAndPath(ns, path)。
 */
public final class RegistrationHelper {

    public static void registerBlocks(Map<String, Block> blocks) {
        for (Map.Entry<String, Block> e : blocks.entrySet()) {
            Identifier id = RealCircuitMod.id(e.getKey());
            Registry.register(BuiltInRegistries.BLOCK, id, e.getValue());
        }
    }

    public static void registerBlockItems(Map<String, Block> blocks) {
        for (Map.Entry<String, Block> e : blocks.entrySet()) {
            Identifier id = RealCircuitMod.id(e.getKey());
            // 26.2：物品模型由 minecraft:item_model 组件驱动，必须显式设置，否则物品栏/手持/掉落显示故障方块（且无日志报错）
            Item.Properties props = new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .component(DataComponents.ITEM_MODEL, id);
            Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(e.getValue(), props));
        }
    }

    public static <T> T register(String name, T obj, Registry<T> registry) {
        return Registry.register(registry, RealCircuitMod.id(name), obj);
    }
}

package com.realcircuit;

import com.realcircuit.blockentity.RheostatBlockEntity;
import com.realcircuit.blockentity.StudentPowerBlockEntity;
import com.realcircuit.network.RheostatUpdatePayload;
import com.realcircuit.network.StudentPowerUpdatePayload;
import com.realcircuit.registry.ModBlocks;
import com.realcircuit.registry.ModBlockEntities;
import com.realcircuit.registry.ModItems;
import com.realcircuit.registry.ModTabs;
import com.realcircuit.world.CircuitWorldManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主入口（服务端+客户端通用）。
 * 26.2 为无混淆版本，使用 Mojang 官方类名：net.minecraft.resources.ResourceLocation 等。
 */
public class RealCircuitMod implements ModInitializer {
    public static final String MOD_ID = "realcircuit";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModTabs.register();
        registerNetworking();
        // 任意方块被破坏后重新求解（含铜导线、 vanilla 导体等），保证电路实时响应
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClientSide()) CircuitWorldManager.solveAround(world, pos);
        });
        LOGGER.info("[真实物理电路] 元件与方块实体已注册");
    }

    /** 客户端→服务端：同步滑动变阻器的滑片与接线柱选择。 */
    private void registerNetworking() {
        PayloadTypeRegistry.serverboundPlay().register(
                RheostatUpdatePayload.TYPE, RheostatUpdatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RheostatUpdatePayload.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().execute(() -> {
                BlockEntity be = player.level().getBlockEntity(payload.pos());
                if (be instanceof RheostatBlockEntity rhe) {
                    rhe.percent = Math.max(0.0, Math.min(1.0, payload.percent()));
                    rhe.setConnMask(payload.connMask());
                    rhe.syncToClient();
                    CircuitWorldManager.solveAround(player.level(), payload.pos());
                }
            });
        });
        PayloadTypeRegistry.serverboundPlay().register(
                StudentPowerUpdatePayload.TYPE, StudentPowerUpdatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StudentPowerUpdatePayload.TYPE, (payload, ctx) -> {
            ServerPlayer player = ctx.player();
            ctx.server().execute(() -> {
                BlockEntity be = player.level().getBlockEntity(payload.pos());
                if (be instanceof StudentPowerBlockEntity sp) {
                    sp.setVoltage(payload.voltage());
                    CircuitWorldManager.solveAround(player.level(), payload.pos());
                }
            });
        });
    }
}

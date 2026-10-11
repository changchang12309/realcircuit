package com.realcircuit.registry;

import com.realcircuit.RealCircuitMod;
import com.realcircuit.block.RheostatBlock;
import com.realcircuit.block.StudentPowerBlock;
import com.realcircuit.block.MeterBlock;
import com.realcircuit.blockentity.RheostatBlockEntity;
import com.realcircuit.blockentity.StudentPowerBlockEntity;
import com.realcircuit.blockentity.MeterBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 方块实体类型注册。
 * 26.2：new BlockEntityType 会在构造时向注册表创建侵入式 holder，只能在冻结前做一次；
 * 因此注册为单例实例，方块实体直接引用，绝不能重复构造（否则 "can't create intrusive holders"）。
 */
public class ModBlockEntities {
    public static BlockEntityType<RheostatBlockEntity> RHEOSTAT;
    public static BlockEntityType<StudentPowerBlockEntity> STUDENT_POWER;
    public static BlockEntityType<MeterBlockEntity> METER;

    public static void register() {
        RHEOSTAT = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                RealCircuitMod.id("rheostat"),
                new BlockEntityType<>(
                        (pos, state) -> new RheostatBlockEntity(pos, state,
                                ((RheostatBlock) state.getBlock()).maxR),
                        Set.copyOf(ModBlocks.RHEOSTAT.values())));

        STUDENT_POWER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                RealCircuitMod.id("student_power"),
                new BlockEntityType<>(
                        StudentPowerBlockEntity::new,
                        Set.of(ModBlocks.POWER_STUDENT)));

        Set<net.minecraft.world.level.block.Block> meters = new LinkedHashSet<>();
        meters.add(ModBlocks.AMMETER_MECH);
        meters.add(ModBlocks.VOLTMETER_MECH);
        meters.add(ModBlocks.AMMETER_ELEC);
        meters.add(ModBlocks.VOLTMETER_ELEC);
        METER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                RealCircuitMod.id("meter"),
                new BlockEntityType<>(
                        (pos, state) -> new MeterBlockEntity(pos, state,
                                ((MeterBlock) state.getBlock()).isAmmeter),
                        meters));
    }
}

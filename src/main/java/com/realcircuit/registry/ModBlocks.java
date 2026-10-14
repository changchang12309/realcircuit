package com.realcircuit.registry;

import com.realcircuit.RealCircuitMod;
import com.realcircuit.block.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 方块注册。所有方块套用已生成的模型/材质（assets/realcircuit/models/block）。
 * 26.2：Properties 必须在构造 Block 前 setId，否则 effectiveDrops 抛 "Block id not set"。
 */
public class ModBlocks {
    public static final Map<String, Block> ALL = new LinkedHashMap<>();

    private static BlockBehaviour.Properties base(String name) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, RealCircuitMod.id(name)))
                .noLootTable();
    }

    private static BlockBehaviour.Properties normal(String name) {
        return base(name).mapColor(MapColor.METAL).strength(2.0f);
    }
    private static BlockBehaviour.Properties mech(String name) {
        return base(name).mapColor(MapColor.METAL).strength(3.0f).noOcclusion();
    }
    private static BlockBehaviour.Properties elec(String name) {
        return base(name).mapColor(MapColor.COLOR_BLACK).strength(2.0f);
    }
    private static BlockBehaviour.Properties bulb(String name) {
        // 光照等级随亮度档位变化：0/1/2/3 档 → 0/5/10/15
        return normal(name).lightLevel(s -> s.hasProperty(com.realcircuit.block.BulbBlock.LIGHT)
                ? s.getValue(com.realcircuit.block.BulbBlock.LIGHT) * 5 : 0);
    }

    // 定值电阻（8 档）+ 损坏版
    public static final Map<Integer, Block> RESISTOR = regResistors();
    public static final Map<Integer, Block> RESISTOR_DAMAGED = regResistorsDamaged();

    // 滑动变阻器（4 档，带方块实体）
    public static final Map<Integer, Block> RHEOSTAT = regRheostats();

    // 电表
    public static final Block AMMETER_MECH = reg("ammeter_mechanical", new MeterBlock(mech("ammeter_mechanical"), true, false));
    public static final Block VOLTMETER_MECH = reg("voltmeter_mechanical", new MeterBlock(mech("voltmeter_mechanical"), false, false));
    public static final Block AMMETER_ELEC = reg("ammeter_electronic", new MeterBlock(elec("ammeter_electronic"), true, true));
    public static final Block VOLTMETER_ELEC = reg("voltmeter_electronic", new MeterBlock(elec("voltmeter_electronic"), false, true));

    // 铜导线（红石粉式可连接细长条，零电阻导线节点）
    public static final Block COPPER_WIRE = reg("copper_wire",
            new CopperWireBlock(base("copper_wire").noOcclusion().strength(0.0f)));

    public static final Block LED = reg("led", new LedBlock(normal("led")));

    // 直流电源（5 档）
    public static final Map<String, Block> POWER_DC = regPowerDc();
    public static final Block POWER_STUDENT = reg("power_student", new StudentPowerBlock(normal("power_student")));
    // 短路烧毁后的电源：惰性方块（不参与求解、不再输出电能），仅作损坏表现
    public static final Block POWER_DAMAGED = reg("power_damaged", new Block(normal("power_damaged")));

    public static final Block SWITCH = reg("switch", new SwitchBlock(normal("switch")));
    public static final Block FUSE = reg("fuse", new FuseBlock(normal("fuse"), false));
    public static final Block FUSE_DAMAGED = reg("fuse_damaged", new FuseBlock(normal("fuse_damaged"), true));

    // 小灯泡（3 规格）+ 损坏版
    public static final Map<String, Block> BULB = regBulbs();
    public static final Map<String, Block> BULB_DAMAGED = regBulbsDamaged();

    private static Map<Integer, Block> regResistors() {
        Map<Integer, Block> m = new LinkedHashMap<>();
        for (int r : new int[]{5, 10, 20, 50, 100, 200, 500, 1000}) {
            String n = "resistor_" + r;
            m.put(r, reg(n, new ResistorBlock(normal(n), r, false)));
        }
        return m;
    }
    private static Map<Integer, Block> regResistorsDamaged() {
        Map<Integer, Block> m = new LinkedHashMap<>();
        for (int r : new int[]{5, 10, 20, 50, 100, 200, 500, 1000}) {
            String n = "resistor_" + r + "_damaged";
            m.put(r, reg(n, new ResistorBlock(normal(n), r, true)));
        }
        return m;
    }
    private static Map<Integer, Block> regRheostats() {
        Map<Integer, Block> m = new LinkedHashMap<>();
        for (int r : new int[]{20, 50, 100, 200}) {
            String n = "rheostat_" + r;
            m.put(r, reg(n, new RheostatBlock(normal(n), r)));
        }
        return m;
    }
    private static Map<String, Block> regPowerDc() {
        Map<String, Block> m = new LinkedHashMap<>();
        String[][] vs = {{"1_5v", "1.5"}, {"3v", "3"}, {"6v", "6"}, {"9v", "9"}, {"12v", "12"}};
        for (String[] v : vs) {
            String n = "power_dc_" + v[0];
            m.put(v[0], reg(n, new VoltageSourceBlock(normal(n), Double.parseDouble(v[1]))));
        }
        return m;
    }
    private static Map<String, Block> regBulbs() {
        Map<String, Block> m = new LinkedHashMap<>();
        String[][] bs = {{"0_3v_0_2a", "0.3", "0.2"}, {"2_5v_0_3a", "2.5", "0.3"}, {"3_8v_0_3a", "3.8", "0.3"}};
        for (String[] b : bs) {
            String n = "bulb_" + b[0];
            m.put(b[0], reg(n, new BulbBlock(bulb(n), Double.parseDouble(b[1]), Double.parseDouble(b[2]), false)));
        }
        return m;
    }
    private static Map<String, Block> regBulbsDamaged() {
        Map<String, Block> m = new LinkedHashMap<>();
        String[][] bs = {{"0_3v_0_2a", "0.3", "0.2"}, {"2_5v_0_3a", "2.5", "0.3"}, {"3_8v_0_3a", "3.8", "0.3"}};
        for (String[] b : bs) {
            String n = "bulb_" + b[0] + "_damaged";
            m.put(b[0], reg(n, new BulbBlock(bulb(n), Double.parseDouble(b[1]), Double.parseDouble(b[2]), true)));
        }
        return m;
    }

    private static Block reg(String name, Block block) {
        ALL.put(name, block);
        return block;
    }

    public static void register() {
        com.realcircuit.registry.RegistrationHelper.registerBlocks(ALL);
    }
}

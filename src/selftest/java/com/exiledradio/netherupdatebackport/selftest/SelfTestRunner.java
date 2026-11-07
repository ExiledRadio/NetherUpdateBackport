package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Runs the headless self-tests on a dedicated dev server, then stops it.
 *
 * <pre>    ./gradlew runServer -Pselftest=all      (or a stream name: kelp, coral, ...)</pre>
 *
 * <p>Waits a second after the server starts, runs each requested test, logs one line per failure and
 * {@code SELFTEST DONE pass=N fail=M}, and writes for {@code tools/check_assets.py}:
 * <ul>
 *   <li>{@code eclipse/selftest-states.txt}: every valid state of every block of this mod, as
 *       {@code registry<TAB>variant<TAB>kind}, where variant is vanilla's state string (properties
 *       sorted by name) and kind is {@code variants} or {@code multipart};</li>
 *   <li>{@code eclipse/selftest-items.txt}: every item model request and the item's translation key.</li>
 * </ul>
 * This class and its tests are compiled in only with {@code -Pselftest} and never reach a release jar.
 */
public final class SelfTestRunner {

    /** Every stream's test. A stream adds its own line. */
    private static final SelfTest[] TESTS = {
            new ScaffoldSelfTest(),
            new StoneSelfTest(),
            new BlocksSelfTest(),
            new FungusSelfTest(),
            new SoulSelfTest(),
            new NetheriteSelfTest(),
            new HoglinSelfTest(),
            new StriderSelfTest(),
            new OwnedSelfTest(),
            new ChangesSelfTest(),
            new MobsSelfTest(),
            new BiomeSelfTest(),
            new MusicSelfTest(),
    };

    private final List<String> wanted;
    private int ticks;

    private SelfTestRunner(List<String> wanted) {
        this.wanted = wanted;
    }

    public static void start(String which) {
        NetherUpdateBackport.LOGGER.info("SELFTEST requested: {}", which);
        MinecraftForge.EVENT_BUS.register(new SelfTestRunner(Arrays.asList(which.split(","))));
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ++this.ticks != 20) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = server.getWorld(0);
        Check check = new Check(world);
        try {
            dumpStates(server.getFile("selftest-states.txt"));
            dumpItems(server.getFile("selftest-items.txt"));
        } catch (Exception e) {
            check.fail("dump: " + e);
        }
        for (SelfTest test : TESTS) {
            if (this.wanted.contains("all") || this.wanted.contains(test.name()) || test instanceof ScaffoldSelfTest) {
                check.section(test.name());
                try {
                    test.run(check);
                } catch (Throwable t) {
                    NetherUpdateBackport.LOGGER.error("SELFTEST {} threw", test.name(), t);
                    check.fail(test.name() + " threw " + t);
                }
            }
        }
        NetherUpdateBackport.LOGGER.info("SELFTEST DONE pass={} fail={}", check.passed(), check.failed());
        try {
            PrintWriter out = new PrintWriter(server.getFile("selftest-report.txt"), "UTF-8");
            out.println("pass=" + check.passed() + " fail=" + check.failed());
            for (String line : check.failures()) {
                out.println(line);
            }
            out.close();
        } catch (Exception ignored) {
        }
        server.initiateShutdown();
    }

    private static void dumpStates(File file) throws Exception {
        PrintWriter out = new PrintWriter(file, "UTF-8");
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!NetherUpdateBackport.MOD_ID.equals(block.getRegistryName().getNamespace())) {
                continue;
            }
            String kind = Content.multipartBlocks().contains(block) ? "multipart" : "variants";
            for (IBlockState state : block.getBlockState().getValidStates()) {
                out.println(block.getRegistryName() + "\t" + variant(state) + "\t" + kind);
            }
        }
        out.close();
    }

    /** Vanilla's DefaultStateMapper string: properties sorted by name, name=value joined by commas. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static String variant(IBlockState state) {
        Map<String, String> sorted = new TreeMap<String, String>();
        String[] ignored = Content.ignoredProperties().get(state.getBlock());
        java.util.List<String> skip = ignored == null ? java.util.Collections.<String>emptyList() : Arrays.asList(ignored);
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            IProperty p = e.getKey();
            if (skip.contains(p.getName())) {
                continue;
            }
            sorted.put(p.getName(), p.getName((Comparable) e.getValue()));
        }
        if (sorted.isEmpty()) {
            return "normal";
        }
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (b.length() > 0) {
                b.append(',');
            }
            b.append(e.getKey()).append('=').append(e.getValue());
        }
        return b.toString();
    }

    private static void dumpItems(File file) throws Exception {
        PrintWriter out = new PrintWriter(file, "UTF-8");
        List<Item> seen = new ArrayList<Item>();
        for (Map.Entry<Item, Map<Integer, String>> e : Content.itemModels().entrySet()) {
            seen.add(e.getKey());
            for (Map.Entry<Integer, String> m : e.getValue().entrySet()) {
                ItemStack stack = new ItemStack(e.getKey(), 1, m.getKey());
                out.println(e.getKey().getRegistryName() + "\t" + m.getKey() + "\t" + m.getValue() + "\t"
                        + e.getKey().getTranslationKey(stack) + ".name");
            }
        }
        for (Item item : ForgeRegistries.ITEMS) {
            if (NetherUpdateBackport.MOD_ID.equals(item.getRegistryName().getNamespace()) && !seen.contains(item)) {
                out.println(item.getRegistryName() + "\t-\tNO_MODEL_REQUEST\t-");
            }
        }
        out.close();
    }
}

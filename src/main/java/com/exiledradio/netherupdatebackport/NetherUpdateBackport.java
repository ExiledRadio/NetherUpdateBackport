package com.exiledradio.netherupdatebackport;

import com.exiledradio.netherupdatebackport.network.NetworkHandler;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Nether Update Backport: Minecraft 1.16's nether blocks, mobs and items for 1.12.2.
 *
 * <h2>Layout</h2>
 * One package per stream of content (see {@code docs/PLAN.md}): each registers its own blocks, items,
 * entities and sounds from its own {@code @Mod.EventBusSubscriber} class, behind its own config
 * category's {@code enabled} switch. {@code common} holds what several streams share. This class only
 * calls each stream's lifecycle hooks, one line per stream.
 *
 * <h2>Behavior source</h2>
 * Current Java Edition as minecraft.wiki describes it, with 1.14.4's decompiled code for the numbers the
 * wiki leaves out (1.16.5's for 1.16 content). Where today's game differs from 1.16, today's wins.
 *
 * <h2>Name</h2>
 * The display name is not final. It is substituted from {@code gradle.properties} ({@code mod_name}) at
 * build time; never write it anywhere else. The mod id is permanent.
 */
@Mod(
        modid = NetherUpdateBackport.MOD_ID,
        name = NetherUpdateBackport.MOD_NAME,
        version = NetherUpdateBackport.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:backportcore@[1.2.0,);required-after:fluidlogged_api@[3.3.3,)"
)
public final class NetherUpdateBackport {

    public static final String MOD_ID = "netherupdatebackport";
    public static final String MOD_NAME = "@MOD_NAME@";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.Instance(MOD_ID)
    public static NetherUpdateBackport instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NetworkHandler.init();
        com.exiledradio.netherupdatebackport.common.Triggers.init();
        com.exiledradio.netherupdatebackport.owned.OwnedContent.preInit();
        com.exiledradio.netherupdatebackport.mobs.MobsContent.preInit();
        com.exiledradio.netherupdatebackport.structure.StructureContent.preInit();
        net.minecraftforge.fml.common.network.NetworkRegistry.INSTANCE.registerGuiHandler(this,
                new com.exiledradio.netherupdatebackport.common.GuiHandler());

        // Client-only setup lives in a class a dedicated server never loads. The call names no client
        // type, so this class verifies on a server - a guarded method here would not.
        if (event.getSide().isClient()) {
            com.exiledradio.netherupdatebackport.client.ClientSetup.init();
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        com.exiledradio.netherupdatebackport.common.Compat.init();
        com.exiledradio.netherupdatebackport.common.Smelting.init();
        com.exiledradio.netherupdatebackport.blocks.BlocksContent.init();
        com.exiledradio.netherupdatebackport.netherite.NetheriteContent.init();
        com.exiledradio.netherupdatebackport.changes.ChangesContent.init();
        com.exiledradio.netherupdatebackport.piglin.PiglinContent.init();
        com.exiledradio.netherupdatebackport.structure.StructureContent.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        com.exiledradio.netherupdatebackport.strider.StriderContent.postInit();
        com.exiledradio.netherupdatebackport.biome.BiomeContent.postInit();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
    }

    /**
     * Starts the headless self-test when a test build asks for one ({@code -Pselftest=<stream>}).
     *
     * <p>Reached by name, so a release jar - which has no test classes - never links against them.
     */
    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        String selftest = System.getProperty(MOD_ID + ".selftest");
        if (selftest == null || selftest.isEmpty()) {
            return;
        }
        try {
            Class.forName("com.exiledradio.netherupdatebackport.selftest.SelfTestRunner")
                    .getMethod("start", String.class).invoke(null, selftest);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("SELFTEST DONE pass=0 fail=1: '{}' requested but the test classes are not in this build.", selftest, e);
            // A test run must always end, or it holds its port and its log files.

            net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance().initiateShutdown();
        }
    }
}

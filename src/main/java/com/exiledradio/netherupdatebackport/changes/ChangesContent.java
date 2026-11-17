package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;

import net.minecraft.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Stream C: 1.16's smaller changes to things 1.12.2 already has.
 *
 * <ul>
 *   <li>Hoes are a real tool ({@link HoeTool}).</li>
 *   <li>Fire Resistance lets you see farther in lava: client only, {@code changes.client.LavaFog}.</li>
 *   <li>Knockback resistance scales knockback, Mending repairs only damaged items, the nether-side portal search
 *       is 16 blocks, flowing lava pushes, the totem gives Fire Resistance: patches P5 to P9, hooks in
 *       {@link ChangeHooks}.</li>
 *   <li>Redstone dust's dot and cross ({@link RedstoneDot}, patch P11).</li>
 *   <li>Fishing treasure only in open water, lily pads in the junk ({@link Fishing}).</li>
 *   <li>1.16's sounds for netherrack, nether bricks, quartz ore, bone blocks, soul sand and nether wart
 *       ({@link BlockSounds}).</li>
 * </ul>
 * 1.16's nether-tab advancements all belong to a stream (see {@code docs/streams/changes.md}); vanilla's own
 * nether advancements did not change in 1.16, so none is added here.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class ChangesContent {

    private static boolean hoes;

    private ChangesContent() {
    }

    public static boolean enabled() {
        return ModConfig.changes.enabled;
    }

    public static void preInit() {
        if (!enabled()) {
            return;
        }
        if (ModConfig.changes.blockSounds) {
            BlockSounds.preInit();
        }
        Fishing.init();
        MinecraftForge.EVENT_BUS.register(new Fishing());
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        // Always registered with the stream, so a dot in a world survives the setting being switched off (it then
        // turns back into vanilla's wire when anything next to it changes).
        RedstoneDot.dot = Content.block(event.getRegistry(), "redstone_dot", new RedstoneDot(), null);
        RedstoneDot.dot.setTranslationKey("redstoneDust");
        Content.multipart(RedstoneDot.dot);
    }

    /** From init: every mod's blocks and items are registered by then. */
    public static void init() {
        if (!enabled()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new RedstoneDot.Use());
        if (ModConfig.changes.blockSounds) {
            BlockSounds.init();
        }
        if (!ModConfig.changes.hoes) {
            return;
        }
        HoeTool.init();
        MinecraftForge.EVENT_BUS.register(new HoeTool());
        hoes = true;
    }

    /** @return true if hoes were made a tool at startup */
    public static boolean hoes() {
        return hoes;
    }
}

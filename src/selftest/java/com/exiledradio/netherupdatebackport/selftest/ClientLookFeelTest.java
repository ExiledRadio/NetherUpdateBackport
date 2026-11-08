package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.presentation.DebugKeys;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.PanoramaRenderer;
import com.exiledradio.backportcore.presentation.client.TitleSplashes;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.asm.LookTransformer;
import com.exiledradio.netherupdatebackport.lookfeel.client.LookAndFeelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;

/**
 * Stream Lf's client check, for {@code gradlew runClient -Pselftest=client}: at the title screen it checks what
 * Backport Core shows (newest version, panorama, overlay, splash pool, loading revision, F3 keys), the lang
 * overrides and patches P3 and P4, and the Singleplayer button, all in code (nothing reaches the window). It logs
 * {@code LOOKFEEL ...} lines and {@code CLIENT SELFTEST DONE pass=N fail=M}, then closes the game.
 *
 * <p>With other backports installed beside this one (the family's combined check) it also checks that their
 * splashes are in the pool and that 1.16 still wins.
 */
@SideOnly(Side.CLIENT)
public final class ClientLookFeelTest {

    private int ticks;
    private int pass;
    private int fail;
    private boolean done;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientLookFeelTest());
        NetherUpdateBackport.LOGGER.info("CLIENT SELFTEST registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
        } else {
            this.fail++;
            NetherUpdateBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || this.done || !(mc.currentScreen instanceof GuiMainMenu) || ++this.ticks < 400) {
            return;
        }
        this.done = true;
        try {
            run(mc);
        } catch (Throwable t) {
            NetherUpdateBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
        }
        NetherUpdateBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    private void run(Minecraft mc) {
        GameVersion newest = Presentation.newest();
        LoadingRevision loading = Presentation.loadingRevision();
        ResourceLocation[] faces = PanoramaRenderer.faces();
        List<String> pool = TitleSplashes.lastPool;
        StringBuilder installed = new StringBuilder();
        for (com.exiledradio.backportcore.presentation.Contribution c : Presentation.installed()) {
            installed.append(c.version()).append(c.owners()).append(' ');
        }
        NetherUpdateBackport.LOGGER.info("LOOKFEEL installed: {}", installed.toString().trim());
        NetherUpdateBackport.LOGGER.info("LOOKFEEL newest={} panorama={} ({}) overlay={} loading={} splashes={} panoramaFrames={}",
                newest, PanoramaRenderer.choice(), faces == null ? null : faces[0], Presentation.panoramaOverlay(), loading,
                pool == null ? -1 : pool.size(), PanoramaRenderer.frames);

        ok(newest.isAtLeast("1.16"), "the newest version is 1.16 or later: " + newest);
        ok(PanoramaRenderer.modern(), "the title screen is drawn the 1.13+ way");
        ok(faces != null && faces[0].getNamespace().equals(NetherUpdateBackport.MOD_ID), "1.16's nether panorama is shown: "
                + (faces == null ? null : faces[0]));
        ok(PanoramaRenderer.frames > 20, "the panorama was drawn: " + PanoramaRenderer.frames + " frames");
        ok(Presentation.panoramaOverlay() != null && Presentation.panoramaOverlay().getNamespace().equals(NetherUpdateBackport.MOD_ID),
                "1.16's clear overlay is used: " + Presentation.panoramaOverlay());
        ok(loading == LoadingRevision.DATA_1_16, "the world-loading screen is 1.16's: " + loading);
        ok(mc.loadingScreen != null && mc.loadingScreen.getClass().getName().endsWith("WorldLoadingScreen"),
                "Core's loading screen is installed: " + (mc.loadingScreen == null ? null : mc.loadingScreen.getClass().getName()));

        // Splashes: every installed version's additions are in, its removals out (the union).
        ok(pool != null, "the title screen drew from Core's splash pool");
        if (pool != null) {
            splash(pool, "1.16", new String[] {"Moderately attractive!", "Black lives matter!", "Zoglin!?"}, new String[] {"Sexy!", "Popping tags!"});
            splash(pool, "1.15", new String[] {"Buzzy Bees!", "In case it isn't obvious, foxes aren't players."}, new String[0]);
            splash(pool, "1.14", new String[] {"Rainbow turtle?", "Ping the human!", "{PLAYER} IS YOU"}, new String[] {"Made by Notch!", "Woo, minecraftforum!"});
            splash(pool, "1.13", new String[] {"All rumors are true!", "Thank you for the fish!"}, new String[0]);
            ok(pool.contains("Hello Internet!") || pool.contains("Made by Jeb!"), "1.12.2's own splashes stay in the pool");
        }

        // F3: 1.16's two keys are in the key map with this mod's actions.
        Map<Integer, DebugKeys.Key> keys = DebugKeys.map(Presentation.installed());
        DebugKeys.Key f4 = keys.get(Keyboard.KEY_F4);
        DebugKeys.Key n = keys.get(Keyboard.KEY_N);
        ok(DebugKeys.changed(Presentation.installed()), "the F3 key map is changed, so patch P7 routes F3 through it");
        ok(f4 != null && f4.action != null && "debug.gamemodes.help".equals(f4.helpKey), "F3+F4 opens the game mode switcher: " + f4);
        ok(n != null && n.action != null && "debug.creative_spectator.help".equals(n.helpKey), "F3+N is 1.16's: " + n);
        NetherUpdateBackport.LOGGER.info("LOOKFEEL F3 keys: {}", keys.values());

        // P3 and P4 ran on the classes the client loaded.
        ok(LookTransformer.appliedP3, "P3 (Distortion Effects) patched EntityRenderer");
        ok(LookTransformer.appliedP4, "P4 (Entity Distance) patched RenderGlobal");

        // Lang: this mod's text wins over vanilla 1.12.2's for the keys 1.16 reworded.
        String[][] lang = {
                {"options.entityDistanceScaling", "Entity Distance"},
                {"options.screenEffectScale", "Distortion Effects"},
                {"deathScreen.title", "You Died!"},
                {"key.swapHands", "Swap Item With Offhand"},
                {"itemGroup.hotbar", "Saved Hotbars"},
                {"debug.gamemodes.help", "F3 + F4 = Open game mode switcher"},
        };
        for (String[] kv : lang) {
            ok(kv[1].equals(I18n.format(kv[0])), "I18n " + kv[0] + " = \"" + kv[1] + "\", got \"" + I18n.format(kv[0]) + "\"");
        }

        // Textures this stream draws.
        for (String tex : new String[] {"textures/gui/container/gamemode_switcher.png", "textures/misc/nausea.png"}) {
            try {
                mc.getResourceManager().getResource(new ResourceLocation(NetherUpdateBackport.MOD_ID, tex)).close();
                ok(true, tex);
            } catch (Exception e) {
                ok(false, tex + " loads: " + e);
            }
        }

        // Singleplayer: Create New World when there is no world, the world list otherwise.
        GuiScreen menu = mc.currentScreen;
        boolean empty = LookAndFeelClient.noWorlds(mc);
        mc.displayGuiScreen(new GuiWorldSelection(menu));
        ok(empty ? mc.currentScreen instanceof GuiCreateWorld : mc.currentScreen instanceof GuiWorldSelection,
                "Singleplayer with " + (empty ? "no world" : "worlds") + " opens " + (mc.currentScreen == null ? null : mc.currentScreen.getClass().getSimpleName()));
        mc.displayGuiScreen(menu);
    }

    private void splash(List<String> pool, String version, String[] adds, String[] removes) {
        if (Presentation.contribution(version) == null) {
            return;
        }
        for (String s : adds) {
            ok(pool.contains(s), version + "'s splash \"" + s + "\" is in the pool");
        }
        for (String s : removes) {
            ok(!pool.contains(s), version + " removed \"" + s + "\"");
        }
        NetherUpdateBackport.LOGGER.info("LOOKFEEL splashes of {} checked", version);
    }
}

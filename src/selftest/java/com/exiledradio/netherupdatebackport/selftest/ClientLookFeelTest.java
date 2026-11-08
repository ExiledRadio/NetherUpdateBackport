package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.presentation.DebugKeys;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.PanoramaRenderer;
import com.exiledradio.backportcore.presentation.client.TitleSplashes;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.asm.LookTransformer;
import com.exiledradio.netherupdatebackport.lookfeel.LookAndFeel;
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

        // Lang: 1.16's wording of vanilla's keys (Core's vanilla text) and this mod's own.
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

        // Core's single slots (Core 1.2.0's finished presentation module), the newest version's.
        try {
            String pack = mc.getResourceManager().getResource(new ResourceLocation("textures/gui/title/minecraft.png")).getResourcePackName();
            ok("backportcore:vanilla_by_version".equals(pack), "the title logo is served by Core's vanilla-asset pack: " + pack);
        } catch (Exception e) {
            ok(false, "the title logo loads: " + e);
        }
        ok(Presentation.asset("textures/gui/title/mojangstudios.png") != null, "a Mojang Studios logo is registered");
        byte[] composed = com.exiledradio.backportcore.presentation.client.VanillaAssetPack.composedLang("en_us");
        ok(composed != null && composed.length > 100, "Core composes vanilla text for en_us from " + Presentation.vanillaTexts() + ": "
                + (composed == null ? "null" : composed.length + " bytes") + "; class loader sees the file: "
                + (com.exiledradio.backportcore.presentation.client.VanillaAssetPack.class.getClassLoader()
                        .getResource("assets/netherupdatebackport/vanilla_text/en_us.lang")));
        ok(com.exiledradio.backportcore.presentation.VanillaRevisions.startup() == com.exiledradio.backportcore.presentation.VanillaRevisions.Startup.STUDIOS_1_16,
                "the startup screen is 1.16's");
        ok(com.exiledradio.backportcore.presentation.client.StartupScreen.lastDrawn == com.exiledradio.backportcore.presentation.VanillaRevisions.Startup.STUDIOS_1_16
                && com.exiledradio.backportcore.presentation.client.StartupScreen.frames > 0,
                "the startup screen drew 1.16's revision as loading ended: " + com.exiledradio.backportcore.presentation.client.StartupScreen.frames);
        ok("Caution: Third-Party Online Play".equals(I18n.format("multiplayerWarning.header")), "the multiplayer warning is 1.16's: "
                + I18n.format("multiplayerWarning.header"));
        ok("Saved Hotbars".equals(I18n.format("itemGroup.hotbar")), "vanilla text comes through Core (itemGroup.hotbar)");
        ok(com.exiledradio.backportcore.presentation.client.GameMusic.names().contains("1.16 nether biomes, gentler switching"),
                "1.16's music step is registered with Core: " + com.exiledradio.backportcore.presentation.client.GameMusic.names());
        ok(mc.getMusicTicker() instanceof com.exiledradio.backportcore.presentation.client.GameMusic.CoreTicker,
                "Core's music ticker plays: " + mc.getMusicTicker().getClass().getName());
        ok(com.exiledradio.backportcore.presentation.client.DebugText.steps().names(newest).contains("1.16 spawn counts, targeted block coordinates"),
                "1.16's F3 text step is registered: " + com.exiledradio.backportcore.presentation.client.DebugText.steps().names(newest));
        ok(com.exiledradio.backportcore.presentation.VanillaRevisions.whiteButtonText(), "1.15's white button text holds at 1.16");
        try {
            com.exiledradio.backportcore.presentation.client.OptionsListScreen access =
                    com.exiledradio.backportcore.presentation.client.OptionsListScreen.accessibility(mc.currentScreen);
            mc.displayGuiScreen(access);
            @SuppressWarnings("unchecked")
            List<net.minecraft.client.gui.GuiButton> buttons = (List<net.minecraft.client.gui.GuiButton>)
                    net.minecraftforge.fml.relauncher.ReflectionHelper.findField(GuiScreen.class, "buttonList", "field_146292_n").get(access);
            boolean guide = false;
            int distortion = 0;
            for (net.minecraft.client.gui.GuiButton b : buttons) {
                guide |= "Accessibility Guide".equals(b.displayString);
                if (b instanceof com.exiledradio.backportcore.presentation.client.OptionWidgets.OptionButton
                        && ((com.exiledradio.backportcore.presentation.client.OptionWidgets.OptionButton) b).option() == LookAndFeel.DISTORTION) {
                    distortion++;
                }
            }
            ok(guide, "1.16.4's Accessibility Guide is on Accessibility Settings");
            ok(distortion == 1, "Distortion Effects shows once on Accessibility Settings");
            mc.displayGuiScreen(new GuiMainMenu());
        } catch (Exception e) {
            ok(false, "Accessibility Settings opens: " + e);
        }

        // The family's combined check: with the 1.13, 1.14 and 1.15 backports beside this one, 1.16 wins each single
        // slot and the lists are the union.
        boolean others = net.minecraftforge.fml.common.Loader.isModLoaded("updateaquaticbackport")
                && net.minecraftforge.fml.common.Loader.isModLoaded("villagerbackport") && net.minecraftforge.fml.common.Loader.isModLoaded("buzzybeesbackport");
        NetherUpdateBackport.LOGGER.info("LOOKFEEL combined: others installed {}; assets {}; vanilla text {}; music {}; F3 text {}",
                others, Presentation.assets(), Presentation.vanillaTexts(), com.exiledradio.backportcore.presentation.client.GameMusic.names(),
                com.exiledradio.backportcore.presentation.client.DebugText.steps().names(newest));
        if (others) {
            String id = NetherUpdateBackport.MOD_ID;
            ok(id.equals(Presentation.asset("textures/gui/widgets.png").getNamespace()), "combined: widgets.png is 1.16's (over 1.15's)");
            ok(id.equals(Presentation.asset("textures/gui/checkbox.png").getNamespace()), "combined: checkbox.png is 1.16's (over 1.15.2's)");
            ok(id.equals(Presentation.titleAccessibilityIcon().getNamespace()), "combined: the title Accessibility icon is 1.16's (over 1.14's and 1.15's)");
            ok("buzzybeesbackport".equals(Presentation.asset("textures/gui/title/mojang.png").getNamespace()),
                    "combined: mojang.png is the newest that ships it (1.15.2's), though 1.16 draws mojangstudios.png");
            ok("Caution: Third-Party Online Play".equals(I18n.format("multiplayerWarning.header")), "combined: the warning's 1.16 wording beats 1.15.2's");
            ok("Chat Text Opacity".equals(I18n.format("options.chat.opacity")), "combined: 1.14's vanilla text is there too");
            ok("Left Button".equals(I18n.format("key.mouse.left")), "combined: 1.13's vanilla text is there too");
            ok("Open in Browser".equals(I18n.format("chat.link.open")), "combined: 1.15's vanilla text is there too");
            ok(Presentation.vanillaTexts().size() == 4, "combined: four versions' vanilla text, oldest first: " + Presentation.vanillaTexts());
            List<String> music = com.exiledradio.backportcore.presentation.client.GameMusic.names();
            ok(music.indexOf("1.13 under water") >= 0 && music.indexOf("1.13 under water") < music.indexOf("1.16 nether biomes, gentler switching"),
                    "combined: the music steps are 1.13's then 1.16's: " + music);
            List<String> f3 = com.exiledradio.backportcore.presentation.client.DebugText.steps().names(newest);
            ok(f3.equals(java.util.Arrays.asList("1.13 targeted block, fluid and entity", "1.14.2 server chunk cache", "1.15 no chunk updates",
                    "1.16 spawn counts, targeted block coordinates")), "combined: the F3 text steps run in version order: " + f3);
            ok(com.exiledradio.backportcore.presentation.client.GameMusic.underWater() != null, "combined: 1.16's music rules see 1.13's underwater event");
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

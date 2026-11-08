package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.PresentationOption;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.asm.LookTransformer;
import com.exiledradio.netherupdatebackport.lookfeel.LookAndFeel;
import com.exiledradio.netherupdatebackport.lookfeel.LookHooks;
import net.minecraft.util.ResourceLocation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Stream Lf: 1.16's look and feel registered with Backport Core, its assets, patches P3 and P4, and the lang overrides. */
public final class LookFeelSelfTest implements SelfTest {

    private static final String ID = NetherUpdateBackport.MOD_ID;

    @Override
    public String name() {
        return "lookfeel";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        panorama(c);
        splashes(c);
        options(c);
        hooks(c);
        patches(c);
        lang(c);
    }

    // ------------------------------------------------------------------------------------------

    private static void registration(Check c) {
        Contribution v116 = Presentation.contribution("1.16");
        Contribution v1162 = Presentation.contribution("1.16.2");
        c.ok(v116 != null && v116.owners().contains(ID), "1.16 is registered by " + ID + ": " + v116);
        c.ok(v1162 != null && v1162.owners().contains(ID), "1.16.2 is registered by " + ID + ": " + v1162);
        // On its own (this dev server) 1.16.2 is the newest version: 1.16 wins every single-slot piece.
        c.eq(GameVersion.parse("1.16.2"), Presentation.newest(), "the newest installed version");
        c.ok(Presentation.isAtLeast("1.16"), "isAtLeast(1.16)");
        c.eq(LoadingRevision.DATA_1_16, Presentation.loadingRevision(), "the world-loading screen revision");
    }

    private static void panorama(Check c) throws IOException {
        List<Presentation.Panorama> all = Presentation.panoramas();
        Presentation.Panorama last = all.isEmpty() ? null : all.get(all.size() - 1);
        if (!c.ok(last != null && last.version.equals(GameVersion.parse("1.16")), "the newest panorama is 1.16's: " + all.size() + " panoramas")) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            ResourceLocation face = last.faces[i];
            c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_" + i + ".png"), face, "panorama face " + i);
            int[] size = pngSize(face);
            c.ok(size != null && size[0] == 1024 && size[1] == 1024, "face " + i + " is 1.16.5's 1024x1024 image: " + Arrays.toString(size));
        }
        ResourceLocation overlay = Presentation.panoramaOverlay();
        c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"), overlay, "the panorama overlay");
        int[] size = overlay == null ? null : pngSize(overlay);
        c.ok(size != null && size[0] == 1 && size[1] == 1, "1.16.5's overlay is one clear pixel: " + Arrays.toString(size));
        for (String tex : new String[] {"textures/gui/container/gamemode_switcher.png", "textures/misc/nausea.png"}) {
            c.ok(pngSize(new ResourceLocation(ID, tex)) != null, tex + " is in the jar");
        }
    }

    private static final String[] ADDED = {"Moderately attractive!", "Wash your hands!", "Music by Lena Raine!", "Zoglin!?",
            "Black lives matter!", "Don’t touch your face!", "Stand up for equality in your community!"};
    private static final String[] REMOVED = {"Sexy!", "Undocumented!", "Popping tags!", "Huge tracts of land!",
            "Getting ready to show!", "Getting ready to speak!"};

    private static void splashes(Check c) throws IOException {
        List<String> added = lines(new ResourceLocation(ID, "texts/splashes_added.txt"));
        List<String> removed = lines(new ResourceLocation(ID, "texts/splashes_removed.txt"));
        c.eq(41, added.size(), "1.16.5 adds this many splashes to 1.15.2's");
        c.eq(14, removed.size(), "1.16.5 removes this many");
        List<String> base = lines("/assets/minecraft/texts/splashes.txt");
        c.ok(base.size() > 300, "1.12.2's splashes.txt is readable: " + base.size());
        List<String> pool = Splashes.pool(base, Presentation.installed(), f -> lines(f));
        for (String s : ADDED) {
            c.ok(pool.contains(s), "the splash pool has 1.16's \"" + s + "\"");
        }
        for (String s : REMOVED) {
            c.ok(base.contains(s) && !pool.contains(s), "the splash pool lost \"" + s + "\"");
        }
        Set<String> expected = new LinkedHashSet<String>(base);
        expected.removeAll(removed);
        expected.addAll(added);
        c.eq(expected.size(), pool.size(), "pool size: 1.12.2's set less 1.16's removals plus its additions");
    }

    private static void options(Check c) {
        Map<String, PresentationOption> byId = new HashMap<String, PresentationOption>();
        for (PresentationOption o : Presentation.options()) {
            byId.put(o.id(), o);
        }
        Object[][] want = {
                {LookAndFeel.ENTITY_DISTANCE, OptionScreen.VIDEO, "1.16", "options.entityDistanceScaling"},
                {LookAndFeel.DISTORTION_VIDEO, OptionScreen.VIDEO, "1.16.2", "options.screenEffectScale"},
                {LookAndFeel.FOV_EFFECTS_VIDEO, OptionScreen.VIDEO, "1.16.2", "options.fovEffectScale"},
                {LookAndFeel.DISTORTION, OptionScreen.ACCESSIBILITY, "1.16.2", "options.screenEffectScale"},
                {LookAndFeel.FOV_EFFECTS, OptionScreen.ACCESSIBILITY, "1.16.2", "options.fovEffectScale"},
        };
        for (Object[] w : want) {
            PresentationOption o = (PresentationOption) w[0];
            c.ok(byId.get(o.id()) == o, o.id() + " is registered");
            c.eq(w[1], o.screen(), o.id() + "'s screen");
            c.eq(GameVersion.parse((String) w[2]), o.since(), o.id() + "'s since");
            c.eq(w[3], o.labelKey(), o.id() + "'s label");
            c.ok(o.isShown(), o.id() + " shows at the newest version");
        }
        c.eq(0.5, LookAndFeel.ENTITY_DISTANCE.min(), "Entity Distance min");
        c.eq(5.0, LookAndFeel.ENTITY_DISTANCE.max(), "Entity Distance max");
        c.eq(1.0, LookAndFeel.ENTITY_DISTANCE.defaultValue(), "Entity Distance default");
        c.eq(1.25, LookAndFeel.ENTITY_DISTANCE.snap(1.3), "Entity Distance moves in steps of 25%");
        c.eq(1.0, LookAndFeel.DISTORTION.defaultValue(), "Distortion Effects default");
        c.eq(1.0, LookAndFeel.FOV_EFFECTS.defaultValue(), "FOV Effects default");

        List<String> video = ids(Presentation.optionsOn(OptionScreen.VIDEO));
        int d = video.indexOf(LookAndFeel.DISTORTION_VIDEO.id());
        int e = video.indexOf(LookAndFeel.ENTITY_DISTANCE.id());
        int f = video.indexOf(LookAndFeel.FOV_EFFECTS_VIDEO.id());
        c.ok(d >= 0 && e >= 0 && f >= 0, "Video Settings shows all three: " + video);
        c.eq("ENTITY_SHADOWS", LookAndFeel.DISTORTION_VIDEO.after(), "Distortion Effects follows Entity Shadows");
        c.eq(LookAndFeel.DISTORTION_VIDEO.id(), LookAndFeel.ENTITY_DISTANCE.after(), "Entity Distance follows Distortion Effects");
        c.eq(LookAndFeel.ENTITY_DISTANCE.id(), LookAndFeel.FOV_EFFECTS_VIDEO.after(), "FOV Effects follows Entity Distance");
        List<String> access = ids(Presentation.optionsOn(OptionScreen.ACCESSIBILITY));
        int ad = access.indexOf(LookAndFeel.DISTORTION.id());
        int af = access.indexOf(LookAndFeel.FOV_EFFECTS.id());
        c.ok(ad >= 0 && af == ad + 1, "Accessibility Settings shows Distortion Effects then FOV Effects: " + access);

        // The two buttons of each setting move together.
        LookAndFeel.DISTORTION.set(0.5);
        c.eq(0.5, LookAndFeel.DISTORTION_VIDEO.get(), "Distortion Effects on Video follows Accessibility");
        LookAndFeel.FOV_EFFECTS_VIDEO.set(0.25);
        c.eq(0.25, LookAndFeel.FOV_EFFECTS.get(), "FOV Effects on Accessibility follows Video");
        c.eq(0.5F, LookAndFeel.fovEffectScale(), "FOV Effects at 25% scales by its root, 0.5 (1.16.2's sqrt)");
        LookAndFeel.DISTORTION.reset();
        LookAndFeel.FOV_EFFECTS.reset();
        c.eq(1.0, LookAndFeel.DISTORTION_VIDEO.get(), "reset reaches the mirror");
        c.eq(1.0, LookAndFeel.FOV_EFFECTS_VIDEO.get(), "reset reaches the mirror (FOV)");
    }

    private static List<String> ids(List<PresentationOption> options) {
        List<String> out = new ArrayList<String>();
        for (PresentationOption o : options) {
            out.add(o.id());
        }
        return out;
    }

    private static void hooks(Check c) {
        c.eq(2.0F, LookHooks.portalWobble(2.0F), "full Distortion Effects keeps the wobble");
        LookAndFeel.DISTORTION.set(0.5);
        c.eq(0.5F, LookHooks.portalWobble(2.0F), "Distortion Effects at 50% scales the wobble by its square");
        LookAndFeel.DISTORTION.set(0.0);
        c.eq(0.0F, LookHooks.portalWobble(2.0F), "Distortion Effects off removes it");
        LookAndFeel.DISTORTION.reset();
        c.eq(1.5, LookHooks.entityDistance(1.5), "Entity Distance at 100% keeps vanilla's weight");
        LookAndFeel.ENTITY_DISTANCE.set(2.0);
        c.eq(3.0, LookHooks.entityDistance(1.5), "Entity Distance at 200% doubles it");
        LookAndFeel.ENTITY_DISTANCE.reset();
    }

    /** P3 and P4 against the dev jar's MCP-named classes and Forge's SRG-named jar, then a stack check. */
    private static void patches(Check c) throws IOException {
        String[][] targets = {
                {"net.minecraft.client.renderer.EntityRenderer", "portalWobble", "setupCameraTransform", "func_78479_a"},
                {"net.minecraft.client.renderer.RenderGlobal", "entityDistance", "setupTerrain", "func_174970_a"},
        };
        for (String[] t : targets) {
            byte[] dev = classBytes(t[0]);
            check(c, "dev " + t[0], dev == null ? null : LookTransformer.patchForTest(t[0], dev), t);
        }
        File srg = new File(System.getProperty("user.home"),
                ".gradle/caches/minecraft/net/minecraftforge/forge/1.12.2-14.23.5.2847/stable/39/forge-1.12.2-14.23.5.2847-srgBin.jar");
        if (!c.ok(srg.isFile(), "Forge's SRG jar is in the Gradle cache: " + srg)) {
            return;
        }
        ZipFile zip = new ZipFile(srg);
        try {
            for (String[] t : targets) {
                ZipEntry entry = zip.getEntry(t[0].replace('.', '/') + ".class");
                byte[] bytes = entry == null ? null : read(zip.getInputStream(entry));
                check(c, "SRG " + t[0], bytes == null ? null : LookTransformer.patchForTest(t[0], bytes), t);
            }
        } finally {
            zip.close();
        }
    }

    private static void check(Check c, String what, byte[] patched, String[] t) {
        if (!c.ok(patched != null, what + " was read")) {
            return;
        }
        ClassNode node = new ClassNode();
        new ClassReader(patched).accept(node, 0);
        int hooks = 0;
        for (MethodNode m : node.methods) {
            int here = 0;
            for (org.objectweb.asm.tree.AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof org.objectweb.asm.tree.MethodInsnNode && t[1].equals(((org.objectweb.asm.tree.MethodInsnNode) i).name)) {
                    here++;
                }
            }
            if (here > 0) {
                c.ok(m.name.equals(t[2]) || m.name.equals(t[3]), what + ": the hook is in " + t[2] + ", found in " + m.name);
                hooks += here;
            }
            try {
                new Analyzer<org.objectweb.asm.tree.analysis.BasicValue>(new BasicVerifier()).analyze(node.name, m);
            } catch (Exception e) {
                c.fail(what + "." + m.name + " fails ASM's stack check: " + e);
            }
        }
        c.eq(1, hooks, what + ": calls to LookHooks." + t[1]);
    }

    private static final String[][] LANG = {
            {"options.entityDistanceScaling", "Entity Distance"},
            {"options.screenEffectScale", "Distortion Effects"},
            {"options.fovEffectScale", "FOV Effects"},
            {"debug.gamemodes.help", "F3 + F4 = Open game mode switcher"},
            {"deathScreen.title", "You Died!"},
            {"key.swapHands", "Swap Item With Offhand"},
            {"itemGroup.hotbar", "Saved Hotbars"},
            {"multiplayer.status.client_out_of_date", "Incompatible version!"},
            {"debug.creative_spectator.help", "F3 + N = Cycle previous gamemode <-> spectator"},
    };

    private static final String[] LANGUAGES = {"de_de", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it", "ja_jp",
            "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"};

    private static void lang(Check c) throws IOException {
        Map<String, String> en = langFile("en_us");
        for (String[] kv : LANG) {
            c.eq(kv[1], en.get(kv[0]), "en_us " + kv[0]);
        }
        for (String code : LANGUAGES) {
            Map<String, String> l = langFile(code);
            int have = 0;
            for (String[] kv : LANG) {
                if (l.get(kv[0]) != null && !l.get(kv[0]).isEmpty()) {
                    have++;
                }
            }
            c.eq(LANG.length, have, code + " has the 1.16 option, F3 and reworded vanilla keys");
        }
    }

    // ------------------------------------------------------------------------------------------

    private static Map<String, String> langFile(String code) throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        for (String line : lines(new ResourceLocation(ID, "lang/" + code + ".lang"))) {
            int eq = line.indexOf('=');
            if (eq > 0 && !line.startsWith("#")) {
                out.put(line.substring(0, eq), line.substring(eq + 1));
            }
        }
        return out;
    }

    private static List<String> lines(ResourceLocation rl) throws IOException {
        return lines("/assets/" + rl.getNamespace() + "/" + rl.getPath());
    }

    private static List<String> lines(String path) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("not on the classpath: " + path);
        }
        List<String> out = new ArrayList<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    out.add(line);
                }
            }
        } finally {
            r.close();
        }
        return out;
    }

    private static int[] pngSize(ResourceLocation rl) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream("/assets/" + rl.getNamespace() + "/" + rl.getPath());
        if (in == null) {
            return null;
        }
        byte[] b = read(in);
        if (b.length < 24 || b[1] != 'P' || b[2] != 'N' || b[3] != 'G') {
            return null;
        }
        return new int[] {readInt(b, 16), readInt(b, 20)};
    }

    private static int readInt(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | (b[at + 3] & 0xFF);
    }

    private static byte[] classBytes(String name) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream("/" + name.replace('.', '/') + ".class");
        return in == null ? null : read(in);
    }

    private static byte[] read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}

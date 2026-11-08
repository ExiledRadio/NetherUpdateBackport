package com.exiledradio.netherupdatebackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.SliderOption;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import net.minecraft.util.ResourceLocation;

/**
 * Stream Lf: 1.16's look and feel, the changes 1.16 made beyond its content. Backport Core owns every piece that
 * only one version can have at a time (the panorama, the splash list, the loading screen, the options screens,
 * the F3 keys) and shows the newest installed version's; this class hands Core 1.16's pieces.
 *
 * <ul>
 *   <li>1.16 (20w14a): the nether panorama and 1.16.5's {@code panorama_overlay.png} (a clear pixel: the title
 *       screen has no gradient over the panorama from 1.16 on);</li>
 *   <li>the splash changes between 1.15.2's and 1.16.5's {@code splashes.txt} (41 added, 14 removed, "Sexy!"
 *       becoming "Moderately attractive!" among them);</li>
 *   <li>Entity Distance (20w17a) in Video Settings; Distortion Effects and FOV Effects (1.16.2-pre1) in Video and
 *       Accessibility Settings;</li>
 *   <li>the F3 keys (client side, {@code lookfeel.client.LookAndFeelClient}): F3+F4's game mode switcher and
 *       F3+N's spectator and previous mode;</li>
 *   <li>the world-loading screen: 1.16's "Reading world data" screen comes from Core's table for 1.16, so
 *       nothing is registered for it.</li>
 * </ul>
 * Menu music did not change in 1.16 (sounds.json's {@code music.menu} is menu1 to menu4 in 1.12.2 and 1.16.5).
 * Vanilla text 1.16 reworded (Saved Hotbars, Swap Item With Offhand, "You Died!" ...) is in the lang file.
 */
public final class LookAndFeel {

    private static final String ID = NetherUpdateBackport.MOD_ID;

    /** 1.16's Entity Distance, 50% to 500% in steps of 25%: scales how far away entities are drawn. */
    public static final SliderOption ENTITY_DISTANCE = new SliderOption(ID + ":entity_distance_scaling",
            "options.entityDistanceScaling", OptionScreen.VIDEO, 0.5, 5.0, 0.25, 1.0);

    /**
     * 1.16.2's Distortion Effects (Accessibility Settings), 0 to 100%: scales the nether portal and nausea wobble
     * by its square; below 100% a green nausea overlay shows instead, at the strength the wobble lost.
     */
    public static final SliderOption DISTORTION = new SliderOption(ID + ":screen_effect_scale",
            "options.screenEffectScale", OptionScreen.ACCESSIBILITY, 0.0, 1.0, 0.0, 1.0);

    /** The same setting on Video Settings, where vanilla also has it. Kept equal to {@link #DISTORTION}. */
    public static final SliderOption DISTORTION_VIDEO = new SliderOption(ID + ":screen_effect_scale_video",
            "options.screenEffectScale", OptionScreen.VIDEO, 0.0, 1.0, 0.0, 1.0);

    /**
     * 1.16.2's FOV Effects (Accessibility Settings), 0 to 100%. As vanilla's, the slider shows the square of the
     * scale it applies: the field of view's changes from speed, sprinting and bows are scaled by the root of the
     * slider's value ({@link #fovEffectScale()}).
     */
    public static final SliderOption FOV_EFFECTS = new SliderOption(ID + ":fov_effect_scale",
            "options.fovEffectScale", OptionScreen.ACCESSIBILITY, 0.0, 1.0, 0.0, 1.0);

    /** The same setting on Video Settings. Kept equal to {@link #FOV_EFFECTS}. */
    public static final SliderOption FOV_EFFECTS_VIDEO = new SliderOption(ID + ":fov_effect_scale_video",
            "options.fovEffectScale", OptionScreen.VIDEO, 0.0, 1.0, 0.0, 1.0);

    private static boolean registered;

    private LookAndFeel() {
    }

    /** Pre-init, both sides: hands Core 1.16's pieces. */
    public static void preInit() {
        if (registered) {
            return;
        }
        registered = true;
        Contribution v116 = Presentation.version("1.16");
        v116.panorama(ID + ":textures/gui/title/background/panorama_")
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"))
                .addSplashes(new ResourceLocation(ID, "texts/splashes_added.txt"))
                .removeSplashes(new ResourceLocation(ID, "texts/splashes_removed.txt"));
        // Video Settings in 1.16.5: ... Entity Shadows, Distortion Effects, Entity Distance, FOV Effects.
        v116.option(ENTITY_DISTANCE.after(DISTORTION_VIDEO.id()));

        Contribution v1162 = Presentation.version("1.16.2");
        v1162.option(DISTORTION_VIDEO.after("ENTITY_SHADOWS"))
                .option(FOV_EFFECTS_VIDEO.after(ENTITY_DISTANCE.id()))
                // Accessibility Settings in 1.16.5 ends with Distortion Effects, then FOV Effects.
                .option(DISTORTION)
                .option(FOV_EFFECTS);
        mirror(DISTORTION, DISTORTION_VIDEO);
        mirror(FOV_EFFECTS, FOV_EFFECTS_VIDEO);
    }

    /** Two buttons for one setting: a change on either screen moves the other. */
    private static void mirror(final SliderOption a, final SliderOption b) {
        a.onChange(v -> b.set(v));
        b.onChange(v -> a.set(v));
    }

    /** @return the scale 1.16.2 applies to field-of-view changes: the root of the FOV Effects slider */
    public static float fovEffectScale() {
        return (float) Math.sqrt(FOV_EFFECTS.get());
    }

    /** @return Distortion Effects, 0 to 1 */
    public static float screenEffectScale() {
        return (float) DISTORTION.get();
    }
}

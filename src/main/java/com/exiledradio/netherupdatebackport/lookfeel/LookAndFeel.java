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
 * Also, from 1.16.5's jar, as Core's single slots: the title logo and the Mojang Studios startup screen (20w21a),
 * 1.15's button texture (unchanged in 1.16) and the check box, the title screen's Accessibility icon, the
 * multiplayer warning's 1.16 wording, and vanilla's own text as 1.16.5 words it ({@code vanilla_text/}: Saved
 * Hotbars, Swap Item With Offhand, "You Died!" ...). 1.16.4 registers too, for its Accessibility Guide button.
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


    /**
     * 1.16.2's FOV Effects (Accessibility Settings), 0 to 100%. As vanilla's, the slider shows the square of the
     * scale it applies: the field of view's changes from speed, sprinting and bows are scaled by the root of the
     * slider's value ({@link #fovEffectScale()}).
     */
    public static final SliderOption FOV_EFFECTS = new SliderOption(ID + ":fov_effect_scale",
            "options.fovEffectScale", OptionScreen.ACCESSIBILITY, 0.0, 1.0, 0.0, 1.0);


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
                .removeSplashes(new ResourceLocation(ID, "texts/splashes_removed.txt"))
                .titleLogo(new ResourceLocation(ID, "textures/gui/title/minecraft.png"))
                .startupLogo("textures/gui/title/mojangstudios.png", new ResourceLocation(ID, "textures/gui/title/mojangstudios.png"))
                .replaceAsset("textures/gui/widgets.png", new ResourceLocation(ID, "textures/gui/widgets.png"))
                .replaceAsset("textures/gui/checkbox.png", new ResourceLocation(ID, "textures/gui/checkbox.png"))
                .titleAccessibilityIcon(new ResourceLocation(ID, "textures/gui/accessibility.png"))
                .multiplayerWarning("multiplayerWarning.header", "multiplayerWarning.message", "multiplayerWarning.check")
                .vanillaText(new ResourceLocation(ID, "vanilla_text"));
        // Video Settings in 1.16.5: ... Entity Shadows, Distortion Effects, Entity Distance, FOV Effects.
        v116.option(ENTITY_DISTANCE.after(DISTORTION.id()));

        // 1.16.2-pre1: both on Accessibility Settings (where 1.16.5's list ends with them) and on Video Settings.
        // At zero they read OFF (1.16.5's genericValueLabel(OPTION_OFF)); each has its tooltip.
        DISTORTION.zeroKey("options.off");
        DISTORTION.tooltip("options.screenEffectScale.tooltip");
        DISTORTION.alsoOn(OptionScreen.VIDEO);
        FOV_EFFECTS.zeroKey("options.off");
        FOV_EFFECTS.tooltip("options.fovEffectScale.tooltip");
        FOV_EFFECTS.alsoOn(OptionScreen.VIDEO);
        Contribution v1162 = Presentation.version("1.16.2");
        v1162.option(DISTORTION.after("ENTITY_SHADOWS"))
                .option(FOV_EFFECTS.after(ENTITY_DISTANCE.id()));

        // 1.16.4: the Accessibility Guide button on Accessibility Settings (Core's), and "Incompatible version!".
        Presentation.version("1.16.4");
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

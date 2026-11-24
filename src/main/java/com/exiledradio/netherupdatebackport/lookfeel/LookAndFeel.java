package com.exiledradio.netherupdatebackport.lookfeel;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import net.minecraft.util.ResourceLocation;

/**
 * Stream Lf: 1.16's look and feel, the changes 1.16 made beyond its content. Backport Core owns every piece that
 * only one version can have at a time and shows the newest installed version's. Since Backport Core 1.4.7 Core
 * also owns 1.16's own pieces that are not tied to this mod's content (its {@code presentation.versions.Look116}:
 * Entity Distance, Distortion Effects, FOV Effects and their patches, F3 + F4 and F3 + N, the F3 text, the
 * Create New World redirect, the title and startup logos, the button, check box and Accessibility textures, the
 * multiplayer warning, the splashes and vanilla's text as 1.16.5 words it), so a later backport installed alone
 * still has them. What stays here:
 *
 * <ul>
 *   <li>the declarations that 1.16, 1.16.2 and 1.16.4 are installed;</li>
 *   <li>1.16 (20w14a): the nether panorama and 1.16.5's {@code panorama_overlay.png} (a clear pixel: the title
 *       screen has no gradient over the panorama from 1.16 on);</li>
 *   <li>the music (stream Mu's {@code music.MusicContent}).</li>
 * </ul>
 * The world-loading screen ("Reading world data") comes from Core's table for 1.16.
 */
public final class LookAndFeel {

    private static final String ID = NetherUpdateBackport.MOD_ID;

    private static boolean registered;

    private LookAndFeel() {
    }

    /** Pre-init, both sides: declares 1.16 and hands Core its panorama. */
    public static void preInit() {
        if (registered) {
            return;
        }
        registered = true;
        Presentation.version("1.16")
                .panorama(ID + ":textures/gui/title/background/panorama_")
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"));
        // 1.16.2: Distortion Effects and FOV Effects (Core's).
        Presentation.version("1.16.2");
        // 1.16.4: the Accessibility Guide button on Accessibility Settings (Core's), and "Incompatible version!".
        Presentation.version("1.16.4");
    }
}

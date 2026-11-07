package com.exiledradio.netherupdatebackport;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Configuration: one category per stream, each with an {@code enabled} switch.
 *
 * <h2>Rules for streams</h2>
 * <ul>
 *   <li>A stream edits only inside its own category class.</li>
 *   <li>Category classes extend {@code Object} directly. Forge 1.12 crashes at startup ("Unknown type")
 *       on a category class that extends another one.</li>
 *   <li>{@code enabled} is read once, during registration, and needs a restart: switching a stream off
 *       means its blocks and items are never registered, so a world that has them loses them.</li>
 *   <li>A changed default never reaches an installed game (Forge keeps the value in the file), so a
 *       default that must change gets a new key.</li>
 *   <li>Every field has {@code @Config.LangKey("config.netherupdatebackport.<category>.<field>")}, with a
 *       name line and a {@code .tooltip} line in {@code en_us.lang}.</li>
 * </ul>
 */
@Config(modid = NetherUpdateBackport.MOD_ID, name = NetherUpdateBackport.MOD_ID)
public final class ModConfig {

    @Config.LangKey("config.netherupdatebackport.stone")
    @Config.Comment("Blackstone, polished blackstone and its bricks, gilded blackstone, basalt and polished basalt.")
    public static final Stone stone = new Stone();

    @Config.LangKey("config.netherupdatebackport.blocks")
    @Config.Comment("Chains, crying obsidian, targets, respawn anchors, lodestones, nether gold ore, quartz bricks and the new nether bricks.")
    public static final Blocks blocks = new Blocks();

    @Config.LangKey("config.netherupdatebackport.fungus")
    @Config.Comment("Crimson and warped nylium, fungi, roots, vines, wart blocks, shroomlight and both wood sets.")
    public static final Fungus fungus = new Fungus();

    @Config.LangKey("config.netherupdatebackport.soul")
    @Config.Comment("Soul soil, soul fire, soul torches, soul lanterns, soul campfires and Soul Speed.")
    public static final Soul soul = new Soul();

    @Config.LangKey("config.netherupdatebackport.netherite")
    @Config.Comment("Ancient debris, netherite scrap and ingots, the netherite block, netherite tools and armor.")
    public static final Netherite netherite = new Netherite();

    @Config.LangKey("config.netherupdatebackport.hoglins")
    @Config.Comment("Hoglins and zoglins.")
    public static final Hoglins hoglins = new Hoglins();

    @Config.LangKey("config.netherupdatebackport.piglins")
    @Config.Comment("Piglins, piglin brutes, bartering and the Snout banner pattern.")
    public static final Piglins piglins = new Piglins();

    @Config.LangKey("config.netherupdatebackport.striders")
    @Config.Comment("Striders and the warped fungus on a stick.")
    public static final Striders striders = new Striders();

    @Config.LangKey("config.netherupdatebackport.mobs")
    @Config.Comment("Zombie pigmen become zombified piglins, and 1.16's other changes to existing mobs.")
    public static final Mobs mobs = new Mobs();

    @Config.LangKey("config.netherupdatebackport.music")
    @Config.Comment("The Pigstep music disc, 1.16's nether music and its ambient sounds.")
    public static final Music music = new Music();

    @Config.LangKey("config.netherupdatebackport.changes")
    @Config.Comment("Smaller 1.16 changes to existing blocks and items.")
    public static final Changes changes = new Changes();

    @Config.LangKey("config.netherupdatebackport.biomes")
    @Config.Comment("The crimson forest, warped forest, soul sand valley and basalt deltas.")
    public static final Biomes biomes = new Biomes();

    @Config.LangKey("config.netherupdatebackport.structures")
    @Config.Comment("Bastion remnants, nether fossils and ruined portals.")
    public static final Structures structures = new Structures();

    @Config.LangKey("config.netherupdatebackport.compat")
    @Config.Comment("How this mod behaves alongside other mods.")
    public static final Compat compat = new Compat();

    // ------------------------------------------------------------------------------------------
    // Categories. Each stream owns exactly one.
    // ------------------------------------------------------------------------------------------

    public static final class Stone {
        @Config.RequiresMcRestart
        @Config.Comment("Blackstone and basalt and everything made from them. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.stone.enabled")
        public boolean enabled = true;

        @Config.Comment("Blackstone generates in blobs in the nether, in newly generated chunks.")
        @Config.LangKey("config.netherupdatebackport.stone.generate")
        public boolean generate = true;

        @Config.Comment("Lava over soul soil next to blue ice turns into basalt.")
        @Config.LangKey("config.netherupdatebackport.stone.basaltGenerator")
        public boolean basaltGenerator = true;
    }

    public static final class Blocks {
        @Config.RequiresMcRestart
        @Config.Comment("The smaller 1.16 blocks. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.blocks.enabled")
        public boolean enabled = true;

        @Config.Comment("Nether gold ore generates in the nether, in newly generated chunks.")
        @Config.LangKey("config.netherupdatebackport.blocks.generateGold")
        public boolean generateGold = true;
    }

    public static final class Fungus {
        @Config.RequiresMcRestart
        @Config.Comment("The crimson and warped plants and woods. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.fungus.enabled")
        public boolean enabled = true;
    }

    public static final class Soul {
        @Config.RequiresMcRestart
        @Config.Comment("Soul soil, soul fire and its light sources, and Soul Speed. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.soul.enabled")
        public boolean enabled = true;

        @Config.Comment("Fire lit on soul sand or soul soil burns as soul fire: blue, light 10, 2 damage, never spreading.")
        @Config.LangKey("config.netherupdatebackport.soul.soulFire")
        public boolean soulFire = true;

        @Config.RequiresMcRestart
        @Config.Comment("The soul campfire. Off, it is not registered.")
        @Config.LangKey("config.netherupdatebackport.soul.campfireEnabled")
        public boolean campfireEnabled = true;

        @Config.Comment("How long one item takes to cook on a soul campfire, in ticks. 1.16's is 600 (30 seconds).")
        @Config.RangeInt(min = 1, max = 72000)
        @Config.LangKey("config.netherupdatebackport.soul.campfireCookTime")
        public int campfireCookTime = 600;

        @Config.Comment({"What the soul campfire cooks besides food a furnace cooks into food, as 'modid:item' or",
                "'modid:item=modid:result', with ':meta' to pick one variant. Not read when the campfire's own mod is",
                "installed: the soul campfire then cooks exactly what that campfire cooks."})
        @Config.LangKey("config.netherupdatebackport.soul.campfireExtraFoods")
        public String[] campfireExtraFoods = {};

        @Config.Comment("Items the soul campfire refuses, as 'modid:item' or 'modid:item:meta'.")
        @Config.LangKey("config.netherupdatebackport.soul.campfireExcludedFoods")
        public String[] campfireExcludedFoods = {};
    }

    public static final class Netherite {
        @Config.RequiresMcRestart
        @Config.Comment("Netherite and ancient debris. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.netherite.enabled")
        public boolean enabled = true;

        @Config.Comment("Ancient debris generates in the nether, in newly generated chunks.")
        @Config.LangKey("config.netherupdatebackport.netherite.generate")
        public boolean generate = true;

    }

    public static final class Hoglins {
        @Config.RequiresMcRestart
        @Config.Comment("Hoglins and zoglins. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.hoglins.enabled")
        public boolean enabled = true;

    }

    public static final class Piglins {
        @Config.RequiresMcRestart
        @Config.Comment("Piglins and piglin brutes. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.piglins.enabled")
        public boolean enabled = true;
    }

    public static final class Striders {
        @Config.RequiresMcRestart
        @Config.Comment("Striders and the warped fungus on a stick. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.striders.enabled")
        public boolean enabled = true;

        @Config.RequiresMcRestart
        @Config.Comment("Striders spawn on lava across the nether, as in 1.16.")
        @Config.LangKey("config.netherupdatebackport.striders.spawn")
        public boolean spawn = true;
    }

    public static final class Mobs {
        @Config.RequiresMcRestart
        @Config.Comment("1.16's changes to existing mobs.")
        @Config.LangKey("config.netherupdatebackport.mobs.enabled")
        public boolean enabled = true;
    }

    public static final class Music {
        @Config.RequiresMcRestart
        @Config.Comment("The Pigstep disc and 1.16's nether music and sounds. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.music.enabled")
        public boolean enabled = true;
    }

    public static final class Changes {
        @Config.RequiresMcRestart
        @Config.Comment("Smaller 1.16 changes to existing blocks and items.")
        @Config.LangKey("config.netherupdatebackport.changes.enabled")
        public boolean enabled = true;
    }

    public static final class Biomes {
        @Config.RequiresMcRestart
        @Config.Comment("The four new nether biomes in newly generated nether chunks. Not built yet.")
        @Config.LangKey("config.netherupdatebackport.biomes.enabled")
        public boolean enabled = true;
    }

    public static final class Structures {
        @Config.RequiresMcRestart
        @Config.Comment("Bastion remnants, nether fossils and ruined portals in newly generated chunks. Not built yet.")
        @Config.LangKey("config.netherupdatebackport.structures.enabled")
        public boolean enabled = true;
    }

    public static final class Compat {
        @Config.RequiresMcRestart
        @Config.Comment({"When another installed mod adds the same 1.16 block: OURS hides the other mod's, THEIRS hides",
                "ours, BOTH keeps both."})
        @Config.LangKey("config.netherupdatebackport.compat.futureMcDuplicates")
        public String futureMcDuplicates = "OURS";
    }

    // ------------------------------------------------------------------------------------------

    /** Saves edits made in the in-game config screen. */
    @Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (NetherUpdateBackport.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(NetherUpdateBackport.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }

    private ModConfig() {
    }
}

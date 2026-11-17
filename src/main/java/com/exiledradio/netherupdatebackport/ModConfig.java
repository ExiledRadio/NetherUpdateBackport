package com.exiledradio.netherupdatebackport;

import com.exiledradio.backportcore.settings.Setting;
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

    @Setting.Feature(icon = "netherupdatebackport:blackstone")
    @Config.LangKey("config.netherupdatebackport.stone")
    @Config.Comment("Blackstone, polished blackstone and its bricks, gilded blackstone, basalt and polished basalt.")
    public static final Stone stone = new Stone();

    @Setting.Feature(icon = "netherupdatebackport:chain")
    @Config.LangKey("config.netherupdatebackport.blocks")
    @Config.Comment("Chains, crying obsidian, targets, respawn anchors, lodestones, nether gold ore, quartz bricks and the new nether bricks.")
    public static final Blocks blocks = new Blocks();

    @Setting.Feature(icon = "netherupdatebackport:crimson_fungus")
    @Config.LangKey("config.netherupdatebackport.fungus")
    @Config.Comment("Crimson and warped nylium, fungi, roots, vines, wart blocks, shroomlight and both wood sets.")
    public static final Fungus fungus = new Fungus();

    @Setting.Feature(icon = "netherupdatebackport:soul_lantern")
    @Config.LangKey("config.netherupdatebackport.soul")
    @Config.Comment("Soul soil, soul fire, soul torches, soul lanterns, soul campfires and Soul Speed.")
    public static final Soul soul = new Soul();

    @Setting.Feature(icon = "netherupdatebackport:netherite_ingot")
    @Config.LangKey("config.netherupdatebackport.netherite")
    @Config.Comment("Ancient debris, netherite scrap and ingots, the netherite block, netherite tools and armor.")
    public static final Netherite netherite = new Netherite();

    @Setting.Feature(icon = "minecraft:porkchop")
    @Config.LangKey("config.netherupdatebackport.hoglins")
    @Config.Comment("Hoglins and zoglins.")
    public static final Hoglins hoglins = new Hoglins();

    @Setting.Feature(icon = "netherupdatebackport:piglin_banner_pattern")
    @Config.LangKey("config.netherupdatebackport.piglins")
    @Config.Comment("Piglins, piglin brutes, bartering and the Snout banner pattern.")
    public static final Piglins piglins = new Piglins();

    @Setting.Feature(icon = "netherupdatebackport:warped_fungus_on_a_stick")
    @Config.LangKey("config.netherupdatebackport.striders")
    @Config.Comment("Striders and the warped fungus on a stick.")
    public static final Striders striders = new Striders();

    @Setting.Feature(icon = "minecraft:gold_nugget")
    @Config.LangKey("config.netherupdatebackport.mobs")
    @Config.Comment("Zombie pigmen become zombified piglins, and 1.16's other changes to existing mobs.")
    public static final Mobs mobs = new Mobs();

    @Setting.Feature(icon = "netherupdatebackport:music_disc_pigstep")
    @Config.LangKey("config.netherupdatebackport.music")
    @Config.Comment("The Pigstep music disc, 1.16's nether music and its ambient sounds.")
    public static final Music music = new Music();

    @Setting.Feature(icon = "minecraft:iron_hoe")
    @Config.LangKey("config.netherupdatebackport.changes")
    @Config.Comment("Smaller 1.16 changes to existing blocks and items.")
    public static final Changes changes = new Changes();

    @Setting.Feature(icon = "netherupdatebackport:crimson_nylium")
    @Config.LangKey("config.netherupdatebackport.biomes")
    @Config.Comment("The crimson forest, warped forest, soul sand valley and basalt deltas.")
    public static final Biomes biomes = new Biomes();

    @Setting.Feature(icon = "netherupdatebackport:gilded_blackstone")
    @Config.LangKey("config.netherupdatebackport.structures")
    @Config.Comment("Bastion remnants, nether fossils and ruined portals.")
    public static final Structures structures = new Structures();

    @Setting.Feature(icon = "minecraft:comparator")
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
        @Setting.NewChunks
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
        @Setting.NewChunks
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
        @Config.RequiresMcRestart
        @Setting.ListOf(Setting.Kind.ITEM)
        @Config.LangKey("config.netherupdatebackport.soul.campfireExtraFoods")
        public String[] campfireExtraFoods = {};

        @Config.Comment("Items the soul campfire refuses, as 'modid:item' or 'modid:item:meta'.")
        @Config.RequiresMcRestart
        @Setting.ListOf(Setting.Kind.ITEM)
        @Config.LangKey("config.netherupdatebackport.soul.campfireExcludedFoods")
        public String[] campfireExcludedFoods = {};
    }

    public static final class Netherite {
        @Config.RequiresMcRestart
        @Config.Comment("Netherite and ancient debris. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.netherite.enabled")
        public boolean enabled = true;

        @Config.Comment("Ancient debris generates in the nether, in newly generated chunks.")
        @Setting.NewChunks
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

        @Config.RequiresMcRestart
        @Config.Comment("Piglins spawn in the nether wastes, as in 1.16. Brutes spawn only in bastions.")
        @Config.LangKey("config.netherupdatebackport.piglins.spawn")
        public boolean spawn = true;
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

        @Config.Comment({"Wolves, endermen, polar bears and iron golems stay angry at whoever hurt them for 20 to 39 seconds",
                "after losing them, and forgive a player who dies. Bees forgive too."})
        @Config.LangKey("config.netherupdatebackport.mobs.neutralAnger")
        public boolean neutralAnger = true;

        @Config.Comment("A mob riding a boat, a minecart or another mob never despawns.")
        @Config.LangKey("config.netherupdatebackport.mobs.ridersStay")
        public boolean ridersStay = true;

        @Config.Comment({"Worlds get 1.16's forgiveDeadPlayers (on) and universalAnger (off) game rules, which neutral mobs,",
                "zombified piglins and piglins follow. Off, both stay as their defaults."})
        @Config.LangKey("config.netherupdatebackport.mobs.angerGameRules")
        public boolean angerGameRules = true;

        @Config.Comment("Wither skeletons attack piglins and piglin brutes.")
        @Config.LangKey("config.netherupdatebackport.mobs.witherSkeletonsTargetPiglins")
        public boolean witherSkeletonsTargetPiglins = true;
    }

    public static final class Music {
        @Config.RequiresMcRestart
        @Config.Comment("The Pigstep disc and 1.16's nether music and sounds. Off, nothing is registered.")
        @Config.LangKey("config.netherupdatebackport.music.enabled")
        public boolean enabled = true;

        // A new key (not 0.1.0's pigstepInFortressChests, which defaulted on), so an installed config starts off too.
        @Config.Comment({"Pigstep can also be found in nether fortress chests (the chance a bastion's chests have, 5 in 89).",
                "1.16 has it in bastions only; turn this on if bastions are off."})
        @Config.LangKey("config.netherupdatebackport.music.pigstepInFortresses")
        public boolean pigstepInFortresses = false;

        @Config.RequiresMcRestart
        @Config.Comment({"1.16's music: each nether biome has its own tracks, and music no longer stops when you switch game",
                "modes or walk into another biome. Client only."})
        @Setting.Client
        @Config.LangKey("config.netherupdatebackport.music.biomeMusic")
        public boolean biomeMusic = true;

        @Config.Comment("1.16's nether ambience: each nether biome's sound loop, mood sounds and additions. Client only.")
        @Setting.Client
        @Config.LangKey("config.netherupdatebackport.music.biomeAmbience")
        public boolean biomeAmbience = true;
    }

    public static final class Changes {
        @Config.RequiresMcRestart
        @Config.Comment("Smaller 1.16 changes to existing blocks and items.")
        @Config.LangKey("config.netherupdatebackport.changes.enabled")
        public boolean enabled = true;

        @Config.RequiresMcRestart
        @Config.Comment({"Hoes are the tool for leaves, hay bales, wart blocks, sponges, dried kelp blocks, targets and shroomlights,",
                "and break them faster. Like other tools, a hoe then wears 1 durability per block it breaks and 2 per hit."})
        @Config.LangKey("config.netherupdatebackport.changes.hoes")
        public boolean hoes = true;

        @Config.Comment("In lava you see 5 blocks with Fire Resistance and 1 without.")
        @Setting.Client
        @Config.LangKey("config.netherupdatebackport.changes.lavaFog")
        public boolean lavaFog = true;

        @Config.Comment("Knockback resistance takes off its share of every knockback, instead of a chance to take none.")
        @Config.LangKey("config.netherupdatebackport.changes.knockbackResistance")
        public boolean knockbackResistance = true;

        @Config.Comment("Mending repairs only damaged items with Mending, so no XP goes to an undamaged one.")
        @Config.LangKey("config.netherupdatebackport.changes.mending")
        public boolean mending = true;

        @Config.Comment("A portal into the nether links to a portal within 16 blocks there, not 128. The overworld keeps 128.")
        @Config.LangKey("config.netherupdatebackport.changes.portalSearch")
        public boolean portalSearch = true;

        @Config.Comment("Flowing lava pushes mobs, players and items, as flowing water does (faster in the nether).")
        @Config.LangKey("config.netherupdatebackport.changes.lavaPushes")
        public boolean lavaPushes = true;

        @Config.Comment({"Lone redstone dust is a cross that powers all four sides; use it to make a dot that powers only",
                "the block below. Dust powers the sides it points into."})
        @Config.LangKey("config.netherupdatebackport.changes.redstoneDot")
        public boolean redstoneDot = true;

        @Config.RequiresWorldRestart
        @Config.Comment("Fishing treasure only in open water (5x4x5 of water or air around the bobber); lily pads are junk.")
        @Config.LangKey("config.netherupdatebackport.changes.fishing")
        public boolean fishing = true;

        @Config.Comment("A totem of undying also gives Fire Resistance for 40 seconds.")
        @Config.LangKey("config.netherupdatebackport.changes.totemFireResistance")
        public boolean totemFireResistance = true;

        @Config.RequiresMcRestart
        @Config.Comment("Netherrack, nether bricks, nether wart and its block, bone blocks, quartz ore and soul sand sound as in 1.16.")
        @Config.LangKey("config.netherupdatebackport.changes.blockSounds")
        public boolean blockSounds = true;

        @Config.Comment("A wither can be built on soul soil as well as soul sand, or on a mix of both.")
        @Config.LangKey("config.netherupdatebackport.changes.witherSoulSoil")
        public boolean witherSoulSoil = true;

        @Config.Comment("Mobs keep their paths away from magma blocks next to them, as they do from fire.")
        @Config.LangKey("config.netherupdatebackport.changes.magmaPathing")
        public boolean magmaPathing = true;

        @Config.RequiresMcRestart
        @Config.Comment({"Dispensers put saddles on pigs, striders and tamed horses, armor on horses, carpets on llamas and chests",
                "on llamas, donkeys and mules, and shear mooshrooms and snow golems."})
        @Config.LangKey("config.netherupdatebackport.changes.dispensers")
        public boolean dispensers = true;

        @Config.RequiresMcRestart
        @Config.Comment("Endermen pick up crimson and warped nylium, fungi and roots, and no longer netherrack.")
        @Config.LangKey("config.netherupdatebackport.changes.endermanBlocks")
        public boolean endermanBlocks = true;

        @Config.Comment("Hoes take Efficiency, Fortune and Silk Touch, at the enchanting table and the anvil.")
        @Config.LangKey("config.netherupdatebackport.changes.hoeEnchanting")
        public boolean hoeEnchanting = true;

        @Config.Comment("Compasses and lodestone compasses take Curse of Vanishing.")
        @Config.LangKey("config.netherupdatebackport.changes.compassVanishing")
        public boolean compassVanishing = true;

        @Config.Comment({"Soul sand valleys and warped forests limit how close together their mobs spawn (1.16's spawn costs), so",
                "they hold fewer at once."})
        @Config.LangKey("config.netherupdatebackport.changes.spawnCosts")
        public boolean spawnCosts = true;

        @Config.Comment({"The cave sound plays after you spend long enough in the dark, as the nether biomes' mood sounds do, instead",
                "of at random. Client only."})
        @Setting.Client
        @Config.LangKey("config.netherupdatebackport.changes.caveMood")
        public boolean caveMood = true;

        @Config.Comment("Parrots imitate nearby mobs less often, on their own and on your shoulder.")
        @Config.LangKey("config.netherupdatebackport.changes.parrotImitation")
        public boolean parrotImitation = true;

        @Config.Comment("Splash and lingering water bottles hurt snow golems, as they hurt endermen and blazes.")
        @Config.LangKey("config.netherupdatebackport.changes.snowGolemWater")
        public boolean snowGolemWater = true;

        @Config.Comment("Skeleton horses and zombie horses can be put on a lead.")
        @Config.LangKey("config.netherupdatebackport.changes.undeadHorseLeash")
        public boolean undeadHorseLeash = true;

        @Config.RequiresMcRestart
        @Config.Comment("Pistons and sticky pistons take longer to break (hardness and blast resistance 1.5) and a pickaxe breaks them faster.")
        @Config.LangKey("config.netherupdatebackport.changes.pistonHardness")
        public boolean pistonHardness = true;

        @Config.Comment("Walls connect to glass panes and iron bars, and they to walls.")
        @Config.LangKey("config.netherupdatebackport.changes.wallsPanes")
        public boolean wallsPanes = true;

        @Config.Comment("Breaking a furnace gives the experience for what is in its output slot.")
        @Config.LangKey("config.netherupdatebackport.changes.furnaceXp")
        public boolean furnaceXp = true;

        @Config.Comment("Climbing vines, weeping and twisting vines and scaffolding makes step sounds as ladders do; vines sound as in 1.16.")
        @Config.LangKey("config.netherupdatebackport.changes.vineClimbing")
        public boolean vineClimbing = true;

        @Config.Comment("Any burning projectile lights TNT it hits, not only burning arrows.")
        @Config.LangKey("config.netherupdatebackport.changes.burningProjectilesTnt")
        public boolean burningProjectilesTnt = true;

        @Config.Comment("An armor stand placed in tall grass or snow leaves the grass or snow where it is.")
        @Config.LangKey("config.netherupdatebackport.changes.armorStandPlacement")
        public boolean armorStandPlacement = true;

        @Config.Comment("The End's obsidian platform is rebuilt when any entity comes through a portal, and players arrive in its center.")
        @Config.LangKey("config.netherupdatebackport.changes.endPlatform")
        public boolean endPlatform = true;

        @Config.Comment("Farmland and grass paths suffocate, as soul sand already does.")
        @Config.LangKey("config.netherupdatebackport.changes.suffocation")
        public boolean suffocation = true;

        @Config.Comment("Ghasts are heard from 80 blocks away, not 160.")
        @Config.LangKey("config.netherupdatebackport.changes.ghastSoundRange")
        public boolean ghastSoundRange = true;

        @Config.Comment({"Putting out a soul campfire throws off the food on it. Off, or with Caves & Cliffs Backport installed",
                "(1.17 brought the food back), it stays."})
        @Config.LangKey("config.netherupdatebackport.changes.campfireDropsFood")
        public boolean campfireDropsFood = true;

        @Config.Comment("Wither roses can be planted on soul soil, and a wither's kill can grow one there.")
        @Config.LangKey("config.netherupdatebackport.changes.witherRoseSoulSoil")
        public boolean witherRoseSoulSoil = true;
    }

    public static final class Biomes {
        @Config.RequiresMcRestart
        @Config.Comment({"The crimson forest, warped forest, soul sand valley and basalt deltas, in nether chunks generated from now",
                "on, laid out as in 1.16 for the world's seed. Chunks generated before stay nether wastes. Off, nothing is registered."})
        @Setting.NewChunks
        @Config.LangKey("config.netherupdatebackport.biomes.enabled")
        public boolean enabled = true;

        @Config.RequiresWorldRestart
        @Config.Comment({"How much room each nether biome takes, as 'biome=number': 1 is 1.16's (and keeps the map the same as",
                "1.16's for the seed), more grows a biome, less shrinks it, 0 removes it. Biomes are nether_wastes,",
                "crimson_forest, warped_forest, soul_sand_valley and basalt_deltas."})
        @Setting.NewChunks
        @Config.LangKey("config.netherupdatebackport.biomes.weights")
        public String[] weights = {"nether_wastes=1", "crimson_forest=1", "warped_forest=1", "soul_sand_valley=1", "basalt_deltas=1"};

        @Config.RequiresMcRestart
        @Config.Comment({"When another mod has already replaced the nether (with biomes of its own), the 1.16 biomes step aside.",
                "On, they replace that mod's nether instead."})
        @Setting.NewChunks
        @Config.LangKey("config.netherupdatebackport.biomes.overrideOtherMods")
        public boolean overrideOtherMods = false;
    }

    public static final class Structures {
        @Config.RequiresMcRestart
        @Config.Comment({"Bastion remnants, ruined portals and nether fossils in chunks generated from now on, where 1.16 puts them",
                "for the world's seed. Off, none of them generate and nether fortresses stay where 1.12.2 puts them."})
        @Setting.NewChunks
        @Config.LangKey("config.netherupdatebackport.structures.enabled")
        public boolean enabled = true;

        @Config.RequiresMcRestart
        @Config.Comment({"Bastion remnants in the nether. As in 1.16 they take nether fortress slots: each spot of the grid has a",
                "bastion or a fortress, so fortresses move to 1.16's places. Off, fortresses stay where 1.12.2 puts them."})
        @Setting.NewChunks
        @Config.LangKey("config.netherupdatebackport.structures.bastions")
        public boolean bastions = true;

        @Config.RequiresMcRestart
        @Config.Comment("Ruined portals in the overworld and the nether.")
        @Setting.NewChunks
        @Config.LangKey("config.netherupdatebackport.structures.ruinedPortals")
        public boolean ruinedPortals = true;

        @Config.RequiresMcRestart
        @Config.Comment("Nether fossils in soul sand valleys (needs the nether biomes).")
        @Setting.NewChunks
        @Config.LangKey("config.netherupdatebackport.structures.netherFossils")
        public boolean netherFossils = true;
    }

    public static final class Compat {
        @Config.RequiresMcRestart
        @Config.Comment({"When another installed mod adds the same 1.16 blocks and items: OURS hides the other mod's,",
                "THEIRS hides ours, BOTH hides nothing. Only the visible ancient debris generates."})
        @Config.LangKey("config.netherupdatebackport.compat.duplicates")
        public com.exiledradio.backportcore.duplicate.DuplicateBlocks.Keep duplicates = com.exiledradio.backportcore.duplicate.DuplicateBlocks.Keep.OURS;

        @Config.Comment("Convert blocks and items left behind by another mod that added them, when that mod is removed from a world.")
        @Config.LangKey("config.netherupdatebackport.compat.convertRemoved")
        public boolean convertRemoved = true;
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

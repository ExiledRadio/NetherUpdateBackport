package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.asm.JukeboxTransformer;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.music.ItemMusicDisc;
import com.exiledradio.netherupdatebackport.music.MusicChoice;
import com.exiledradio.netherupdatebackport.music.MusicContent;
import com.exiledradio.netherupdatebackport.music.MusicPlayer;
import com.exiledradio.netherupdatebackport.music.NetherAmbience;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/** Stream M: the Pigstep disc, the music rules and the nether ambience's numbers. */
public final class MusicSelfTest implements SelfTest {

    @Override
    public String name() {
        return "music";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        jukebox(c);
        loot(c);
        choice(c);
        player(c);
        ambience(c);
        resources(c);
    }

    private static SoundEvent sound(String name) {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(NetherUpdateBackport.MOD_ID, name));
    }

    // ------------------------------------------------------------------------------------------

    private static void registration(Check c) {
        for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
            c.ok(sound("music.nether." + kind.id) != null, "music.nether." + kind.id + " is registered");
            for (String part : new String[] {"loop", "mood", "additions"}) {
                c.ok(sound("ambient." + kind.id + "." + part) != null, "ambient." + kind.id + "." + part + " is registered");
            }
        }
        SoundEvent disc = sound("music_disc.pigstep");
        c.ok(disc != null, "music_disc.pigstep is registered");
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(NetherUpdateBackport.MOD_ID, "music_disc_pigstep"));
        if (!c.ok(item instanceof ItemMusicDisc && item == MusicContent.pigstep, "Pigstep is registered as a music disc")) {
            return;
        }
        ItemMusicDisc pigstep = (ItemMusicDisc) item;
        c.ok(pigstep instanceof ItemRecord, "Pigstep is a 1.12.2 record");
        c.eq(1, pigstep.getItemStackLimit(new ItemStack(pigstep)), "Pigstep stacks to");
        c.eq(EnumRarity.RARE, pigstep.getRarity(new ItemStack(pigstep)), "Pigstep's rarity");
        c.ok(pigstep.music() == disc, "Pigstep plays music_disc.pigstep");
        c.eq(13, pigstep.comparatorSignal(), "Pigstep's comparator signal");
        c.eq("item.netherupdatebackport.music_disc_pigstep", pigstep.getTranslationKey(), "Pigstep's translation key");
    }

    // ------------------------------------------------------------------------------------------

    private static void jukebox(Check c) throws Exception {
        c.ok(JukeboxTransformer.applied, "patch P2 is applied to BlockJukebox");
        if (MusicContent.pigstep == null) {
            return;
        }
        BlockPos base = c.area();
        FakePlayer player = c.player("music", base);

        // Pigstep through the record's own right click, as a player puts it in.
        BlockPos box = base.add(2, 0, 2);
        c.world.setBlockState(box, Blocks.JUKEBOX.getDefaultState(), 3);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(MusicContent.pigstep));
        EnumActionResult result = MusicContent.pigstep.onItemUse(player, c.world, box, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        c.eq(EnumActionResult.SUCCESS, result, "Pigstep goes into a jukebox");
        c.ok(c.world.getBlockState(box).getValue(BlockJukebox.HAS_RECORD), "the jukebox has a record");
        c.ok(player.getHeldItemMainhand().isEmpty(), "the disc left the player's hand");
        TileEntity tile = c.world.getTileEntity(box);
        c.ok(tile instanceof BlockJukebox.TileEntityJukebox
                && ((BlockJukebox.TileEntityJukebox) tile).getRecord().getItem() == MusicContent.pigstep, "the jukebox holds Pigstep");
        c.eq(13, c.world.getBlockState(box).getComparatorInputOverride(c.world, box), "a jukebox with Pigstep gives");
        c.eq(13, comparator(c, box), "a comparator reading Pigstep outputs");

        // Vanilla's discs keep vanilla's signals.
        Item[] discs = {Items.RECORD_13, Items.RECORD_CAT, Items.RECORD_WAIT};
        int[] signals = {1, 2, 12};
        for (int i = 0; i < discs.length; i++) {
            BlockPos at = base.add(2 + 4 * (i + 1), 0, 2);
            c.world.setBlockState(at, Blocks.JUKEBOX.getDefaultState(), 3);
            ((BlockJukebox) Blocks.JUKEBOX).insertRecord(c.world, at, c.world.getBlockState(at), new ItemStack(discs[i]));
            c.eq(signals[i], c.world.getBlockState(at).getComparatorInputOverride(c.world, at), discs[i].getRegistryName() + " in a jukebox gives");
            c.eq(signals[i], comparator(c, at), "a comparator reading " + discs[i].getRegistryName() + " outputs");
        }
        BlockPos empty = base.add(2, 0, 8);
        c.world.setBlockState(empty, Blocks.JUKEBOX.getDefaultState(), 3);
        c.eq(0, c.world.getBlockState(empty).getComparatorInputOverride(c.world, empty), "an empty jukebox gives");
    }

    /** A comparator east of the block, facing it, updated once: its output signal. */
    private static int comparator(Check c, BlockPos source) {
        BlockPos at = source.east();
        IBlockState state = Blocks.UNPOWERED_COMPARATOR.getDefaultState().withProperty(BlockRedstoneComparator.FACING, EnumFacing.WEST);
        c.world.setBlockState(at, state, 2 | 16);
        Blocks.UNPOWERED_COMPARATOR.updateTick(c.world, at, c.world.getBlockState(at), c.world.rand);
        TileEntity tile = c.world.getTileEntity(at);
        return tile instanceof TileEntityComparator ? ((TileEntityComparator) tile).getOutputSignal() : -1;
    }

    // ------------------------------------------------------------------------------------------

    private static void loot(Check c) {
        LootTable table = c.world.getLootTableManager().getLootTableFromLocation(LootTableList.CHESTS_NETHER_BRIDGE);
        if (!c.ok(table.getPool(MusicContent.PIGSTEP_POOL) != null, "fortress chests have the Pigstep pool")) {
            return;
        }
        Random random = new Random(1234L);
        int n = 20000;
        int found = 0;
        for (int i = 0; i < n; i++) {
            for (ItemStack stack : table.generateLootForPools(random, new LootContext.Builder(c.world).build())) {
                if (stack.getItem() == MusicContent.pigstep) {
                    found++;
                }
            }
        }
        double share = (double) found / n;
        c.ok(share > 0.048D && share < 0.064D, "Pigstep is in " + share + " of fortress chests (5/89 = 0.0562)");
        c.ok(c.world.getLootTableManager().getLootTableFromLocation(LootTableList.CHESTS_SIMPLE_DUNGEON)
                .getPool(MusicContent.PIGSTEP_POOL) == null, "dungeon chests have no Pigstep pool");
    }

    // ------------------------------------------------------------------------------------------

    private static void track(Check c, MusicChoice.Track t, String event, int min, int max, boolean replace, String what) {
        if (!c.ok(t != null, what + " exists")) {
            return;
        }
        c.eq(event, String.valueOf(t.name()), what + "'s event");
        c.eq(min, t.minDelay, what + "'s min delay");
        c.eq(max, t.maxDelay, what + "'s max delay");
        c.eq(replace, t.replaceCurrent, what + " replaces other music");
    }

    private static void choice(Check c) {
        track(c, MusicChoice.MENU, "minecraft:music.menu", 20, 600, true, "menu");
        track(c, MusicChoice.CREATIVE, "minecraft:music.creative", 12000, 24000, false, "creative");
        track(c, MusicChoice.GAME, "minecraft:music.game", 12000, 24000, false, "game");
        track(c, MusicChoice.CREDITS, "minecraft:music.credits", 0, 0, true, "credits");
        track(c, MusicChoice.END, "minecraft:music.end", 6000, 24000, true, "end");
        track(c, MusicChoice.END_BOSS, "minecraft:music.dragon", 0, 0, true, "end boss");
        for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
            track(c, MusicChoice.nether(kind), "netherupdatebackport:music.nether." + kind.id, 12000, 24000, false, kind.id);
        }

        MusicChoice.Dimension nether = MusicChoice.Dimension.NETHER;
        MusicChoice.Dimension other = MusicChoice.Dimension.OTHER;
        MusicChoice.Dimension end = MusicChoice.Dimension.END;
        c.ok(MusicChoice.inWorld(null, other, false, false, null) == MusicChoice.GAME, "overworld survival: game music");
        c.ok(MusicChoice.inWorld(null, other, false, true, null) == MusicChoice.CREATIVE, "overworld creative flying: creative music");
        c.ok(MusicChoice.inWorld(null, end, false, true, null) == MusicChoice.END, "the End: end music, creative or not");
        c.ok(MusicChoice.inWorld(null, end, true, false, null) == MusicChoice.END_BOSS, "the End with the dragon: boss music");
        for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
            c.ok(MusicChoice.inWorld(null, nether, false, false, kind) == MusicChoice.nether(kind), kind.id + ": its own music");
            c.ok(MusicChoice.inWorld(null, nether, false, true, kind) == MusicChoice.nether(kind), kind.id + " in creative: still its own music");
        }
        c.ok(MusicChoice.inWorld(null, nether, false, false, null) == MusicChoice.nether(NetherBiomes.Kind.NETHER_WASTES),
                "another mod's nether biome: nether wastes music");
        c.ok(MusicChoice.inWorld(null, other, false, false, NetherBiomes.Kind.NETHER_WASTES) == MusicChoice.nether(NetherBiomes.Kind.NETHER_WASTES),
                "a nether biome in another dimension: its music");
        c.ok(MusicChoice.inWorld(null, other, false, true, NetherBiomes.Kind.NETHER_WASTES) == MusicChoice.CREATIVE,
                "a nether biome in another dimension, creative flying: creative music");
        MusicChoice.Track own = MusicChoice.legacy(SoundEvents.MUSIC_END, 1, 2);
        c.ok(MusicChoice.inWorld(own, nether, false, false, NetherBiomes.Kind.CRIMSON_FOREST) == own, "a dimension's own music wins");
        c.ok(own.replaceCurrent, "a dimension's own music replaces, as in 1.12.2");
    }

    // ------------------------------------------------------------------------------------------

    /** A speaker that plays nothing: tracks "play" until {@link #end} and an empty event never plays. */
    private static final class FakeSpeaker implements MusicPlayer.Speaker {
        final List<SoundEvent> started = new ArrayList<SoundEvent>();
        final Set<Object> playing = new HashSet<Object>();
        int stops;
        SoundEvent empty;

        @Override
        public Object play(SoundEvent event) {
            Object handle = new Object();
            this.started.add(event);
            if (event != this.empty) {
                this.playing.add(handle);
            }
            return handle;
        }

        @Override
        public void stop(Object handle) {
            this.playing.remove(handle);
            this.stops++;
        }

        @Override
        public boolean isPlaying(Object handle) {
            return this.playing.contains(handle);
        }

        void end() {
            this.playing.clear();
        }
    }

    private static void player(Check c) {
        MusicChoice.Track crimson = MusicChoice.nether(NetherBiomes.Kind.CRIMSON_FOREST);
        MusicChoice.Track wastes = MusicChoice.nether(NetherBiomes.Kind.NETHER_WASTES);
        MusicChoice.Track warped = MusicChoice.nether(NetherBiomes.Kind.WARPED_FOREST);
        if (crimson == null || wastes == null || warped == null) {
            c.fail("nether tracks missing");
            return;
        }

        // The first track starts after 100 ticks, as 1.16's ticker.
        FakeSpeaker s = new FakeSpeaker();
        MusicPlayer p = new MusicPlayer(new Random(7L), s);
        int ticks = 0;
        while (s.started.isEmpty() && ticks < 1000) {
            p.tick(MusicChoice.GAME);
            ticks++;
        }
        c.eq(101, ticks, "ticks before the first track");
        c.ok(p.isPlaying(MusicChoice.GAME), "the game track is playing");

        // 20w17a: switching to creative does not cut the track off.
        for (int i = 0; i < 200; i++) {
            p.tick(MusicChoice.CREATIVE);
        }
        c.eq(0, s.stops, "switching to creative stopped the music");
        c.ok(p.isPlaying(MusicChoice.GAME) && s.started.size() == 1, "the survival track plays on in creative");

        // The track ends: the next waits 10 to 20 minutes.
        s.end();
        p.tick(MusicChoice.CREATIVE);
        c.ok(p.current() == null, "an ended track is let go");
        c.ok(p.delay() >= 11999 && p.delay() <= 23999, "the next track waits " + p.delay() + " ticks (12000-24000)");

        // The menu replaces: the playing track stops, the menu's starts within its delay.
        FakeSpeaker s2 = new FakeSpeaker();
        MusicPlayer p2 = new MusicPlayer(new Random(8L), s2);
        p2.start(MusicChoice.GAME);
        p2.tick(MusicChoice.GAME);
        int wait = 0;
        while (s2.started.size() < 2 && wait < 100) {
            p2.tick(MusicChoice.MENU);
            wait++;
        }
        c.eq(1, s2.stops, "the menu stopped the game track");
        c.ok(wait <= 11 && s2.started.size() == 2 && s2.started.get(1) == SoundEvents.MUSIC_MENU,
                "the menu music started after " + wait + " ticks (at most 11)");

        // Walking from one nether biome into another lets the track finish.
        FakeSpeaker s3 = new FakeSpeaker();
        MusicPlayer p3 = new MusicPlayer(new Random(9L), s3);
        p3.start(crimson);
        for (int i = 0; i < 200; i++) {
            p3.tick(i % 40 < 20 ? wastes : MusicChoice.GAME);
        }
        c.eq(0, s3.stops, "walking between nether biomes stopped the music");
        c.ok(p3.isPlaying(crimson), "the crimson forest track plays on in nether wastes");
        c.eq(24000, p3.delay(), "while a track plays the wait is held at the max delay");

        // A new dimension stops the music and adds 5 s to the wait.
        p3.stop();
        c.eq(1, s3.stops, "stop() stops the track");
        c.eq(24100, p3.delay(), "stop() adds 100 ticks to the wait");
        p3.tick(crimson);
        c.eq(23999, p3.delay(), "the next tick clamps the wait to the max delay");

        // Warped forest's music has no sounds: nothing plays and the wait starts again.
        FakeSpeaker s4 = new FakeSpeaker();
        s4.empty = warped.event;
        MusicPlayer p4 = new MusicPlayer(new Random(10L), s4);
        p4.start(warped);
        p4.tick(warped);
        c.ok(p4.current() == null, "warped forest's empty track is let go at once");
        c.ok(p4.delay() >= 11999 && p4.delay() <= 23999, "warped forest waits " + p4.delay() + " ticks for the next try");
    }

    // ------------------------------------------------------------------------------------------

    private static void ambience(Check c) {
        for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
            NetherAmbience.Settings s = NetherAmbience.of(kind);
            if (!c.ok(s != null, kind.id + " has ambience")) {
                continue;
            }
            String p = "netherupdatebackport:ambient." + kind.id + ".";
            c.eq(p + "loop", String.valueOf(s.loop.getRegistryName()), kind.id + "'s loop");
            c.eq(p + "mood", String.valueOf(s.mood.getRegistryName()), kind.id + "'s mood");
            c.eq(p + "additions", String.valueOf(s.additions.getRegistryName()), kind.id + "'s additions");
            c.eq(6000, s.moodTickDelay, kind.id + "'s mood delay");
            c.eq(8, s.moodSearchExtent, kind.id + "'s mood search extent");
            c.eq(2.0D, s.moodOffset, kind.id + "'s mood offset");
            c.eq(0.0111D, s.additionsChance, kind.id + "'s additions chance");
        }
        c.ok(NetherAmbience.of(null) == null, "no ambience outside the nether biomes");
        c.ok(Math.abs(NetherAmbience.mood(0.0F, 0, 0, 6000) - 1.0F / 6000.0F) < 1e-9F, "darkness raises the mood by 1/6000");
        c.ok(Math.abs(NetherAmbience.mood(0.5F, 0, 15, 6000) - (0.5F - 14.0F / 6000.0F)) < 1e-7F, "block light 15 lowers it by 14/6000");
        c.ok(Math.abs(NetherAmbience.mood(0.5F, 15, 0, 6000) - (0.5F - 0.001F)) < 1e-7F, "sky light 15 lowers it by 0.001");
        float m = 0.0F;
        int ticks = 0;
        while (m < 1.0F && ticks < 10000) {
            m = Math.max(NetherAmbience.mood(m, 0, 0, 6000), 0.0F);
            ticks++;
        }
        c.ok(ticks >= 5990 && ticks <= 6010, "a mood sound comes after " + ticks + " dark ticks (6000)");
    }

    // ------------------------------------------------------------------------------------------

    private static void resources(Check c) throws Exception {
        JsonObject sounds;
        try (InputStream in = MusicSelfTest.class.getResourceAsStream("/assets/netherupdatebackport/sounds.json")) {
            sounds = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
            c.ok(sounds.has("music.nether." + kind.id), "sounds.json has music.nether." + kind.id);
            for (String part : new String[] {"loop", "mood", "additions"}) {
                c.ok(sounds.has("ambient." + kind.id + "." + part), "sounds.json has ambient." + kind.id + "." + part);
            }
        }
        c.ok(sounds.has("music_disc.pigstep"), "sounds.json has music_disc.pigstep");
        c.eq(0, sounds.getAsJsonObject("music.nether.warped_forest").getAsJsonArray("sounds").size(), "warped forest's music sounds");
        checkTrack(c, sounds, "music.nether.nether_wastes", "netherupdatebackport:music/game/nether/nether_wastes/rubedo", 6);
        checkTrack(c, sounds, "music.nether.crimson_forest", "netherupdatebackport:music/game/nether/crimson_forest/chrysopoeia", 7);
        checkTrack(c, sounds, "music.nether.soul_sand_valley", "netherupdatebackport:music/game/nether/soulsand_valley/so_below", 7);
        checkTrack(c, sounds, "music.nether.basalt_deltas", "netherupdatebackport:music/game/nether/soulsand_valley/so_below", 7);
        JsonObject loop = sounds.getAsJsonObject("ambient.nether_wastes.loop").getAsJsonArray("sounds").get(0).getAsJsonObject();
        c.ok(loop.get("stream").getAsBoolean(), "the nether wastes loop streams");

        String lang;
        try (InputStream in = MusicSelfTest.class.getResourceAsStream("/assets/netherupdatebackport/lang/en_us.lang");
             Scanner scanner = new Scanner(in, "UTF-8")) {
            lang = scanner.useDelimiter("\\A").next();
        }
        c.ok(lang.contains("\nitem.netherupdatebackport.music_disc_pigstep.name=Music Disc\n"), "Pigstep is named Music Disc");
        c.ok(lang.contains("\nitem.record.netherupdatebackport.pigstep.desc=Lena Raine - Pigstep\n"), "Pigstep's description line");
    }

    /** The biome's own track at its weight and half volume, beside the four old nether tracks (1.12.2's files). */
    private static void checkTrack(Check c, JsonObject sounds, String event, String track, int weight) {
        JsonArray list = sounds.getAsJsonObject(event).getAsJsonArray("sounds");
        boolean own = false;
        int old = 0;
        for (JsonElement e : list) {
            JsonObject o = e.getAsJsonObject();
            String name = o.get("name").getAsString();
            if (name.equals(track)) {
                own = o.get("weight").getAsInt() == weight && o.get("volume").getAsFloat() == 0.5F && o.get("stream").getAsBoolean();
            } else if (name.startsWith("minecraft:music/game/nether/nether")) {
                old++;
            }
        }
        c.ok(own, event + " has " + track + " at weight " + weight + ", volume 0.5");
        c.eq(4, old, event + "'s old nether tracks (1.12.2's own files)");
    }
}

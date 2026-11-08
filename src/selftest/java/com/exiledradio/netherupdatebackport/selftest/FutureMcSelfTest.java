package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.duplicate.DuplicateBlocks;
import com.exiledradio.backportcore.migrate.FutureMcConvert;
import com.exiledradio.backportcore.migrate.FutureMcTable;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.common.FutureMcPairs;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.owned.OwnedContent;
import com.exiledradio.netherupdatebackport.soul.BlockSoulLantern;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

/** Stream Fm: the other mod's 1.16 blocks and items becoming ours, and duplicates. */
public final class FutureMcSelfTest implements SelfTest {

    @Override
    public String name() {
        return "futuremc";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(!Loader.isModLoaded(FutureMcTable.FUTURE_MC), "the test game does not have the other mod");
        c.eq(DuplicateBlocks.Keep.OURS, ModConfig.compat.duplicates, "compat.duplicates defaults to OURS");
        c.ok(ModConfig.compat.convertRemoved, "compat.convertRemoved defaults to on");

        // Every recorded block, with each metadata value it saves, turns into ours with nothing lost.
        String[] recorded = {"ancient_debris", "chain", "netherite_block", "soul_soil", "soul_fire_lantern", "soul_fire_torch",
                "lantern", "smithing_table", "stone_brick_wall", "mossy_stone_brick_wall"};
        for (String path : recorded) {
            c.ok(FutureMcTable.recorded(path), "their " + path + " is in the recorded table");
            FutureMcTable.Target target = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, path));
            if (!c.ok(target != null && "netherupdatebackport".equals(target.block.getRegistryName().getNamespace()),
                    "their " + path + " has a counterpart of ours")) {
                continue;
            }
            for (int meta = 0; meta < 16; meta++) {
                Map<String, String> props = FutureMcTable.recordedState(path, meta);
                if (props == null) {
                    continue;
                }
                FutureMcConvert.Converted converted = FutureMcConvert.state(target, props);
                c.ok(converted.dropped.isEmpty(), "their " + path + " meta " + meta + " " + props + " carries across: " + converted.dropped);
            }
        }
        String[][] expected = {{"ancient_debris", "ancient_debris"}, {"chain", "chain"}, {"netherite_block", "netherite_block"},
                {"soul_soil", "soul_soil"}, {"soul_fire_lantern", "soul_lantern"}, {"soul_fire_torch", "soul_torch"}};
        for (String[] pair : expected) {
            FutureMcTable.Target target = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, pair[0]));
            c.ok(target != null && target.block == Owners.ourBlock(pair[1]), "their " + pair[0] + " becomes our " + pair[1]);
        }
        // Owner copies: ours only while Village & Pillage is absent (as in this test game).
        c.ok(OwnedContent.copies(), "no Village & Pillage here");
        for (String path : new String[] {"lantern", "smithing_table", "stone_brick_wall", "mossy_stone_brick_wall"}) {
            FutureMcTable.Target target = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, path));
            c.ok(target != null && target.block == Owners.ourBlock(path), "their " + path + " becomes our copy");
        }

        // The states, one by one.
        FutureMcTable.Target lantern = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, "soul_fire_lantern"));
        if (lantern != null) {
            c.eq(false, FutureMcConvert.state(lantern, FutureMcTable.recordedState("soul_fire_lantern", 0)).state.getValue(BlockSoulLantern.HANGING),
                    "their standing soul lantern (meta 0) stands");
            c.eq(true, FutureMcConvert.state(lantern, FutureMcTable.recordedState("soul_fire_lantern", 1)).state.getValue(BlockSoulLantern.HANGING),
                    "their hanging soul lantern (meta 1) hangs");
        }
        FutureMcTable.Target torch = FutureMcTable.block(new ResourceLocation(FutureMcTable.FUTURE_MC, "soul_fire_torch"));
        if (torch != null) {
            EnumFacing[] facings = {null, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.UP};
            for (int meta = 1; meta <= 5; meta++) {
                IBlockState state = FutureMcConvert.state(torch, FutureMcTable.recordedState("soul_fire_torch", meta)).state;
                c.eq(facings[meta], state.getValue(BlockTorch.FACING), "their soul torch meta " + meta + " faces " + facings[meta]);
                c.eq(meta, state.getBlock().getMetaFromState(state), "and saves as the same metadata (both are 1.12.2 torches)");
            }
        }

        // Items: block items through their blocks, the rest by name.
        String[][] items = {{"soul_fire_torch", "soul_torch"}, {"soul_fire_lantern", "soul_lantern"}, {"chain", "chain"},
                {"ancient_debris", "ancient_debris"}, {"netherite_block", "netherite_block"}, {"soul_soil", "soul_soil"},
                {"netherite_ingot", "netherite_ingot"}, {"netherite_scrap", "netherite_scrap"}, {"netherite_sword", "netherite_sword"},
                {"netherite_pickaxe", "netherite_pickaxe"}, {"netherite_helmet", "netherite_helmet"}, {"netherite_boots", "netherite_boots"},
                {"record_pigstep", "music_disc_pigstep"}};
        for (String[] pair : items) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", FutureMcTable.FUTURE_MC + ":" + pair[0]);
            tag.setByte("Count", (byte) 1);
            tag.setShort("Damage", (short) ("netherite_sword".equals(pair[0]) ? 37 : 0));
            int changed = FutureMcConvert.item(tag);
            Item ours = Owners.ours(pair[1]);
            c.ok(changed == 1 && ours != null && ours.getRegistryName().toString().equals(tag.getString("id")),
                    "their item " + pair[0] + " becomes our " + pair[1] + " (" + tag.getString("id") + ")");
        }
        NBTTagCompound sword = new NBTTagCompound();
        sword.setString("id", "futuremc:netherite_sword");
        sword.setShort("Damage", (short) 37);
        FutureMcConvert.item(sword);
        c.eq((short) 37, sword.getShort("Damage"), "a worn sword keeps its wear");

        // Pairs: every one of ours exists, so with the other mod installed each would pair.
        for (String[] pair : FutureMcPairs.blockPairs()) {
            c.ok(Owners.ourBlock(pair[0]) != null, "our half of the block pair " + pair[0] + " / " + pair[1] + " exists");
        }
        for (String[] pair : FutureMcPairs.itemPairs()) {
            c.ok(Owners.ours(pair[0]) != null, "our half of the item pair " + pair[0] + " / " + pair[1] + " exists");
        }
        for (String[] pair : FutureMcPairs.blockPairs()) {
            Block ours = Owners.ourBlock(pair[0]);
            c.ok(ours != null && !DuplicateBlocks.isHidden(ours), "without the other mod our " + pair[0] + " is not hidden");
        }
        for (String[] pair : FutureMcPairs.itemPairs()) {
            c.ok(!DuplicateBlocks.hiddenItems().contains(Owners.ours(pair[0])), "without the other mod our " + pair[0] + " is not hidden");
        }

        // Their debris generator is taken out by class name; checked on a stand-in generator of our own.
        IWorldGenerator dummy = new StandInGenerator();
        GameRegistry.registerWorldGenerator(dummy, 0);
        c.ok(generators().contains(dummy), "a stand-in generator is registered");
        int removed = removeGenerators(StandInGenerator.class.getName());
        c.eq(1, removed, "one generator of that class is taken out");
        c.ok(!generators().contains(dummy), "and it is gone from Forge's list");
        c.eq(0, removeGenerators("thedarkcolour.futuremc.world.gen.feature.AncientDebrisWorldGen"),
                "without the other mod there is no debris generator of theirs to take out");
    }

    private static int removeGenerators(String className) throws Exception {
        // Backport Core's (CORE_REQUESTS 6), which FutureMcPairs registers the other mod's generator with.
        return DuplicateBlocks.removeGenerators(className);
    }

    @SuppressWarnings("unchecked")
    private static Collection<IWorldGenerator> generators() throws Exception {
        Field field = GameRegistry.class.getDeclaredField("worldGenerators");
        field.setAccessible(true);
        return (Collection<IWorldGenerator>) field.get(null);
    }

    /** Generates nothing; only its class name matters. */
    static final class StandInGenerator implements IWorldGenerator {
        @Override
        public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        }
    }
}

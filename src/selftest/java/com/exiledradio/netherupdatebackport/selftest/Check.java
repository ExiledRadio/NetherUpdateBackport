package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Assertions and a scratch area for the self-tests. */
public final class Check {

    public final WorldServer world;

    private final List<String> failures = new ArrayList<String>();
    private int passed;
    private String section = "";

    /** Each test area is a fresh 32x32 column, east of the last, high above the terrain. */
    private int nextArea;

    Check(WorldServer world) {
        this.world = world;
    }

    void section(String name) {
        this.section = name;
        NetherUpdateBackport.LOGGER.info("SELFTEST --- {} ---", name);
    }

    public boolean ok(boolean condition, String what) {
        if (condition) {
            this.passed++;
        } else {
            fail(what);
        }
        return condition;
    }

    public boolean eq(Object expected, Object actual, String what) {
        return ok(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }

    public void fail(String what) {
        String line = "[" + this.section + "] " + what;
        this.failures.add(line);
        NetherUpdateBackport.LOGGER.error("SELFTEST FAIL {}", line);
    }

    int passed() {
        return this.passed;
    }

    int failed() {
        return this.failures.size();
    }

    List<String> failures() {
        return this.failures;
    }

    /**
     * @return the corner of a new empty 32x32 area at y=100, cleared from y=60 to y=160 with a stone floor
     * at y=99 and its chunks loaded
     */
    public BlockPos area() {
        int x = 20000 + 48 * this.nextArea++;
        int z = 20000;
        BlockPos corner = new BlockPos(x, 100, z);
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int y = 60; y < 160; y++) {
                    BlockPos p = new BlockPos(x + dx, y, z + dz);
                    IBlockState want = y == 99 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState();
                    if (this.world.getBlockState(p) != want) {
                        this.world.setBlockState(p, want, 2 | 16);
                    }
                }
            }
        }
        return corner;
    }

    /** Fills a box (inclusive) with a state, without neighbor updates. */
    public void fill(BlockPos from, BlockPos to, IBlockState state) {
        for (BlockPos p : BlockPos.getAllInBox(from, to)) {
            this.world.setBlockState(p, state, 2 | 16);
        }
    }

    /** A fake player for this test, standing at a position, survival mode. */
    public FakePlayer player(String name, BlockPos at) {
        FakePlayer player = FakePlayerFactory.get(this.world,
                new GameProfile(UUID.nameUUIDFromBytes(("selftest:" + name).getBytes()), "[selftest " + name + "]"));
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.inventory.clear();
        return player;
    }

    public static boolean isFake(EntityPlayerMP player) {
        return player instanceof FakePlayer;
    }
}

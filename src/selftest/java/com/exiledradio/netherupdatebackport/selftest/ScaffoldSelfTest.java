package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.common.Triggers;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;

/** The shared code every stream builds on. */
public final class ScaffoldSelfTest implements SelfTest {

    @Override
    public String name() {
        return "scaffold";
    }

    @Override
    public void run(Check check) {
        BlockPos area = check.area();
        check.ok(check.world.isAirBlock(area.up()), "test area is clear");
        check.ok(Loader.isModLoaded("backportcore"), "Backport Core is loaded");
        check.ok(CriteriaTriggers.get(Triggers.ID) != null, "the event trigger is in vanilla's trigger table");
        check.ok(check.world.getMinecraftServer().getWorld(-1) != null, "the nether is loaded for nether tests");
    }
}

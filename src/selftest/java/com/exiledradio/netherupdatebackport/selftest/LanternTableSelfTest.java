package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.owned.OwnedContent;
import com.exiledradio.netherupdatebackport.soul.SoulContent;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;

/**
 * Lanterns and soul lanterns stand on furniture tables (MrCrayfish's Furniture Mod, cfm in eclipse/mods): its tables
 * answer UNDEFINED for every face, so a lantern goes by the table's full-height top.
 */
public final class LanternTableSelfTest implements SelfTest {

    @Override
    public String name() {
        return "lanterntable";
    }

    @Override
    public void run(Check c) {
        BlockPos post = c.area().up(2);
        Block[] lanterns = {OwnedContent.lantern, SoulContent.soulLantern};
        for (Block lantern : lanterns) {
            if (lantern == null) {
                continue;
            }
            String name = lantern.getRegistryName().toString();
            c.world.setBlockState(post, Blocks.STONE.getDefaultState(), 2 | 16);
            c.ok(lantern.canPlaceBlockAt(c.world, post.up()), "stone holds a " + name);
            c.world.setBlockState(post, Blocks.AIR.getDefaultState(), 2 | 16);
            c.ok(!lantern.canPlaceBlockAt(c.world, post.up()), "air does not hold a " + name);
            if (!Loader.isModLoaded("cfm")) {
                continue;
            }
            Object[][] below = {
                    {"cfm:table_oak", true}, {"cfm:desk_oak", true}, {"cfm:modern_table", true}, {"cfm:counter", true},
                    {"cfm:coffee_table_oak", false},
            };
            for (Object[] b : below) {
                Block block = Block.getBlockFromName((String) b[0]);
                if (block == null) {
                    c.fail(b[0] + " is not registered");
                    continue;
                }
                c.world.setBlockState(post, block.getDefaultState(), 2 | 16);
                c.eq(b[1], lantern.canPlaceBlockAt(c.world, post.up()), b[0] + " under a " + name);
            }
            c.world.setBlockState(post, Blocks.AIR.getDefaultState(), 2 | 16);
        }
        c.ok(OwnedContent.lantern != null || SoulContent.soulLantern != null, "a lantern to test");
    }
}

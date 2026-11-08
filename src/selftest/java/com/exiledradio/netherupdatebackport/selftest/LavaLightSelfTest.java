package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.Map;

/** The nether's lava sea is lit: the air just above open lava has block light 14 or more, as vanilla's. */
public final class LavaLightSelfTest implements SelfTest {

    @Override
    public String name() {
        return "lavalight";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer nether = c.world.getMinecraftServer().getWorld(-1);
        // A fresh area each run: the test world is kept.
        int cx = 3000 + (int) (System.currentTimeMillis() / 1000 % 2000) * 12;
        int cz = -3000;
        int size = 10;
        for (int x = -1; x <= size; x++) {
            for (int z = -1; z <= size; z++) {
                nether.getChunkProvider().provideChunk(cx + x, cz + z);
            }
        }
        // The game lights a new chunk's light sources on its first ticks (Chunk.onTick -> checkLight); do that here.
        int unlit = 0;
        for (int round = 0; round < 3; round++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    nether.getChunk(cx + x, cz + z).onTick(false);
                }
            }
        }
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                unlit += nether.getChunk(cx + x, cz + z).isLightPopulated() ? 0 : 1;
            }
        }
        NetherUpdateBackport.LOGGER.info("SELFTEST lavalight: {} of {} chunks not light-populated after 3 ticks", unlit, size * size);
        // Let the repair run as the game's ticks would.
        c.ok(com.exiledradio.netherupdatebackport.biome.NetherLightRepair.queued(nether) > 0, "new nether chunks are queued for the light repair");
        net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent end = new net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent(
                net.minecraftforge.fml.relauncher.Side.SERVER, net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END, nether);
        for (int i = 0; i < 200 && com.exiledradio.netherupdatebackport.biome.NetherLightRepair.queued(nether) > 0; i++) {
            int before = com.exiledradio.netherupdatebackport.biome.NetherLightRepair.queued(nether);
            com.exiledradio.netherupdatebackport.biome.NetherLightRepair.tick(end);
            if (com.exiledradio.netherupdatebackport.biome.NetherLightRepair.queued(nether) == before) {
                break; // the rest wait for an unloaded edge
            }
        }
        int open = 0;
        int dark = 0;
        int fixedByRelight = 0;
        Map<String, Integer> biomes = new HashMap<String, Integer>();
        StringBuilder samples = new StringBuilder();
        // One chunk in from the edge: the ring outside is generated but never decorated or lit.
        for (int x = 1; x < size - 1; x++) {
            for (int z = 1; z < size - 1; z++) {
                Chunk chunk = nether.getChunk(cx + x, cz + z);
                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int y = 20; y < 40; y++) {
                            BlockPos p = new BlockPos(chunk.x * 16 + bx, y, chunk.z * 16 + bz);
                            if (chunk.getBlockState(p).getMaterial() != Material.LAVA || !nether.isAirBlock(p.up())) {
                                continue;
                            }
                            open++;
                            int light = nether.getLightFor(EnumSkyBlock.BLOCK, p.up());
                            if (light < 13) {
                                dark++;
                                String b = nether.getBiome(p).getRegistryName().toString();
                                biomes.merge(b, 1, Integer::sum);
                                if (samples.length() < 1500) {
                                    samples.append(" ").append(p.getX()).append(",").append(p.getZ())
                                            .append(":lava").append(nether.getLightFor(EnumSkyBlock.BLOCK, p))
                                            .append("/").append(nether.getBlockState(p).getBlock().getRegistryName().getPath())
                                            .append(nether.getBlockState(p).getBlock().getMetaFromState(nether.getBlockState(p)))
                                            .append("/up2:").append(nether.getBlockState(p.up(2)).getBlock().getRegistryName().getPath())
                                            .append("/sky").append(nether.getLightFor(EnumSkyBlock.SKY, p.up()));
                                }
                                nether.checkLightFor(EnumSkyBlock.BLOCK, p.up());
                                if (nether.getLightFor(EnumSkyBlock.BLOCK, p.up()) >= 13) {
                                    fixedByRelight++;
                                }
                            }
                        }
                    }
                }
            }
        }
        NetherUpdateBackport.LOGGER.info("SELFTEST lavalight: open lava {} dark {} relit {} biomes {} samples{}", open, dark, fixedByRelight, biomes, samples);
        c.ok(open > 100, "the area has open lava: " + open);
        c.eq(0, dark, "no dark air over open lava (" + fixedByRelight + " of them light up after a relight; by biome " + biomes + ")");
    }
}

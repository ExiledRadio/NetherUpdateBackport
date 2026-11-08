package com.exiledradio.netherupdatebackport.biome;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lights what nether decoration placed, as current Java's lighting does.
 *
 * <h2>The bug this fixes</h2>
 * 1.12.2 has no block-light pass for chunks in a world without sky light: {@code Chunk.checkLight} does nothing in
 * the nether. A light source placed during decoration is lit only by the placement's own light update, which
 * {@code World.checkLightFor} skips when the 17 blocks around it are not all loaded. Decoration writes into the
 * chunk's neighbors (its 2x2 area at +8), so lava, fire and glowstone placed near an unloaded edge stay unlit for
 * good: dark patches over the lava sea, in the shape of whatever feature placed them (1.16's deltas, springs, fire
 * patches; vanilla 1.12.2's own lava springs leave a few too). Current Java lights every generated chunk.
 *
 * <h2>How</h2>
 * After each nether chunk is populated, the four chunks its decoration can write into are queued. At the end of a
 * tick, every queued chunk whose surroundings are loaded is scanned for light sources whose stored block light is
 * below their own light value; each is relit ({@code checkLightFor}, which spreads to its neighbors). A chunk that had
 * anything relit and was already sent to players is sent again, so they see the light. A chunk whose surroundings
 * are not loaded yet waits; one that unloads is dropped. Nether chunks loaded from a save are queued as well, so
 * worlds generated before this fix are repaired as they are visited.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class NetherLightRepair {

    private static final Map<World, Set<ChunkPos>> QUEUE = new HashMap<World, Set<ChunkPos>>();

    /** At most this many queued chunks are scanned per tick per world, so a burst of generation is spread out. */
    static final int PER_TICK = 16;

    private NetherLightRepair() {
    }

    @SubscribeEvent
    public static void populated(PopulateChunkEvent.Post event) {
        World world = event.getWorld();
        if (world.isRemote || world.provider.getDimensionType() != DimensionType.NETHER) {
            return;
        }
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                queue(world, new ChunkPos(event.getChunkX() + dx, event.getChunkZ() + dz));
            }
        }
    }

    /** A nether chunk loaded from a save is checked too, so chunks generated before this fix are repaired. */
    @SubscribeEvent
    public static void loaded(net.minecraftforge.event.world.ChunkEvent.Load event) {
        World world = event.getWorld();
        if (!world.isRemote && world.provider.getDimensionType() == DimensionType.NETHER && event.getChunk().isTerrainPopulated()) {
            queue(world, event.getChunk().getPos());
        }
    }

    static void queue(World world, ChunkPos pos) {
        Set<ChunkPos> set = QUEUE.get(world);
        if (set == null) {
            set = new LinkedHashSet<ChunkPos>();
            QUEUE.put(world, set);
        }
        set.add(pos);
    }

    @SubscribeEvent
    public static void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        Set<ChunkPos> set = QUEUE.get(event.world);
        if (set == null || set.isEmpty()) {
            return;
        }
        int done = 0;
        for (Iterator<ChunkPos> it = set.iterator(); it.hasNext() && done < PER_TICK; ) {
            ChunkPos pos = it.next();
            if (!event.world.isBlockLoaded(new BlockPos(pos.getXStart(), 0, pos.getZStart()))) {
                it.remove();
                continue;
            }
            BlockPos min = new BlockPos(pos.getXStart() - 17, 0, pos.getZStart() - 17);
            BlockPos max = new BlockPos(pos.getXEnd() + 17, 0, pos.getZEnd() + 17);
            if (!event.world.isAreaLoaded(min, max)) {
                continue;
            }
            it.remove();
            done++;
            if (repair(event.world, event.world.getChunk(pos.x, pos.z)) > 0) {
                resend((WorldServer) event.world, pos);
            }
        }
    }

    /** @return how many light sources were relit in the chunk */
    public static int repair(World world, Chunk chunk) {
        int fixed = 0;
        for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
            if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int emits = section.get(x, y, z).getLightValue();
                        if (emits <= 0) {
                            continue;
                        }
                        BlockPos at = new BlockPos((chunk.x << 4) + x, section.getYLocation() + y, (chunk.z << 4) + z);
                        if (section.getBlockLight(x, y, z) < emits) {
                            world.checkLightFor(EnumSkyBlock.BLOCK, at);
                            fixed++;
                        }
                        // A see-through neighbor darker than this source allows was left stale by a block taken away.
                        int lit = world.getLightFor(EnumSkyBlock.BLOCK, at);
                        for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.values()) {
                            BlockPos n = at.offset(f);
                            if (n.getY() >= 0 && n.getY() < 256 && world.getBlockState(n).getLightOpacity(world, n) == 0
                                    && world.getLightFor(EnumSkyBlock.BLOCK, n) < lit - 1) {
                                world.checkLightFor(EnumSkyBlock.BLOCK, n);
                                fixed++;
                            }
                        }
                    }
                }
            }
        }
        if (fixed > 0) {
            chunk.markDirty();
        }
        return fixed;
    }

    private static void resend(WorldServer world, ChunkPos pos) {
        PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(pos.x, pos.z);
        if (entry != null && entry.isSentToPlayers()) {
            entry.sendPacket(new SPacketChunkData(world.getChunk(pos.x, pos.z), 65535));
        }
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        QUEUE.remove(event.getWorld());
    }

    /** @return how many chunks are waiting, for the self-test */
    public static int queued(World world) {
        Set<ChunkPos> set = QUEUE.get(world);
        return set == null ? 0 : set.size();
    }
}

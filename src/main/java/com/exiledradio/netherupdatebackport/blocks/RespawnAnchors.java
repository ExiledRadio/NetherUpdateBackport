package com.exiledradio.netherupdatebackport.blocks;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Respawning at a respawn anchor.
 *
 * <h2>The one respawn point</h2>
 * 1.16 has a single respawn point per player, a bed or an anchor, whichever was set last. 1.12.2 keeps beds
 * only, per dimension, and never respawns anyone in the nether. So the anchor is kept beside vanilla's bed
 * spawn, in the player's persistent data ({@code netherupdatebackport:respawn_anchor}: x, y, z, dim):
 * <ul>
 *   <li>using a charged anchor in the nether records it;</li>
 *   <li>vanilla setting a spawn point (sleeping in a bed, {@code /spawnpoint}) forgets it, except the
 *       call vanilla makes while it rebuilds a respawning player, which is not the player choosing a bed;</li>
 *   <li>on respawning after death the player goes where vanilla puts them, then, if an anchor is recorded, to
 *       a free spot beside it in the nether, using one charge (1.16's {@code PlayerList.respawn});</li>
 *   <li>an anchor that is gone, empty or walled in is forgotten, with 1.16's message, and the player stays
 *       at vanilla's respawn point.</li>
 * </ul>
 * Leaving the End through its portal is not a death and never uses an anchor (as 1.16).
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class RespawnAnchors {

    public static final String KEY = NetherUpdateBackport.MOD_ID + ":respawn_anchor";

    /** 1.16's {@code RESPAWN_HORIZONTAL_OFFSETS}, then the same a block lower, a block higher, then straight above. */
    private static final int[][] OFFSETS;

    static {
        int[][] ring = {{0, -1}, {-1, 0}, {0, 1}, {1, 0}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
        OFFSETS = new int[ring.length * 3 + 1][];
        int i = 0;
        for (int dy : new int[] {0, -1, 1}) {
            for (int[] r : ring) {
                OFFSETS[i++] = new int[] {r[0], dy, r[1]};
            }
        }
        OFFSETS[i] = new int[] {0, 1, 0};
    }

    /** Players vanilla is rebuilding for a respawn: its own setSpawnPoint call must not forget the anchor. */
    private static final Set<UUID> RESPAWNING = new HashSet<UUID>();

    private RespawnAnchors() {
    }

    /**
     * Records an anchor as this player's respawn point.
     *
     * @return true if it was not already the one recorded (1.16 plays its sound and says so only then)
     */
    public static boolean set(EntityPlayerMP player, BlockPos pos) {
        BlockPos old = anchor(player);
        if (pos.equals(old) && dimension(player) == player.dimension) {
            return false;
        }
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", pos.getX());
        tag.setInteger("y", pos.getY());
        tag.setInteger("z", pos.getZ());
        tag.setInteger("dim", player.dimension);
        persisted(player).setTag(KEY, tag);
        player.sendStatusMessage(new TextComponentTranslation("block.minecraft.set_spawn"), true);
        return true;
    }

    /** @return the recorded anchor, or null */
    @Nullable
    public static BlockPos anchor(EntityPlayer player) {
        NBTTagCompound data = persisted(player);
        if (!data.hasKey(KEY)) {
            return null;
        }
        NBTTagCompound tag = data.getCompoundTag(KEY);
        return new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
    }

    static int dimension(EntityPlayer player) {
        return persisted(player).getCompoundTag(KEY).getInteger("dim");
    }

    public static void forget(EntityPlayer player) {
        persisted(player).removeTag(KEY);
    }

    private static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            RESPAWNING.add(event.getEntityPlayer().getUniqueID());
        }
    }

    @SubscribeEvent
    public static void spawnSet(PlayerSetSpawnEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || RESPAWNING.contains(player.getUniqueID()) || event.getNewSpawn() == null) {
            return;
        }
        forget(player);
    }

    @SubscribeEvent
    public static void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        boolean died = RESPAWNING.remove(event.player.getUniqueID());
        if (!(event.player instanceof EntityPlayerMP) || event.isEndConquered() || !died) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        BlockPos pos = anchor(player);
        if (pos == null) {
            return;
        }
        WorldServer world = player.getServer().getWorld(dimension(player));
        Vec3d spot = world == null ? null : useAnchor(world, pos);
        if (spot == null) {
            forget(player);
            player.sendStatusMessage(new TextComponentTranslation("block.minecraft.spawn.not_valid"), false);
            return;
        }
        send(player, world, spot);
    }

    /**
     * Uses one charge of the anchor at pos if it can take a player.
     *
     * @return where the player stands up, or null if the anchor is gone, empty or walled in
     */
    @Nullable
    public static Vec3d useAnchor(WorldServer world, BlockPos pos) {
        world.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() != BlocksContent.respawnAnchor || state.getValue(BlockRespawnAnchor.CHARGE) == 0
                || !BlockRespawnAnchor.canSetSpawn(world)) {
            return null;
        }
        Vec3d spot = standUp(world, pos, true);
        if (spot == null) {
            spot = standUp(world, pos, false);
        }
        if (spot != null) {
            world.setBlockState(pos, state.withProperty(BlockRespawnAnchor.CHARGE, state.getValue(BlockRespawnAnchor.CHARGE) - 1), 3);
        }
        return spot;
    }

    /** 1.16's {@code findStandUpPosition}: the first offset where a player fits, standing on something. */
    @Nullable
    static Vec3d standUp(World world, BlockPos anchor, boolean avoidDanger) {
        for (int[] o : OFFSETS) {
            BlockPos feet = anchor.add(o[0], o[1], o[2]);
            BlockPos below = feet.down();
            IBlockState floor = world.getBlockState(below);
            AxisAlignedBB floorBox = floor.getCollisionBoundingBox(world, below);
            if (floorBox == null || floorBox == net.minecraft.block.Block.NULL_AABB || floorBox.maxY < 1.0 - 1.0E-7) {
                continue;
            }
            if (avoidDanger && danger(world.getBlockState(below)) || danger(world.getBlockState(feet))) {
                continue;
            }
            AxisAlignedBB body = new AxisAlignedBB(feet.getX() + 0.2, feet.getY(), feet.getZ() + 0.2,
                    feet.getX() + 0.8, feet.getY() + 1.8, feet.getZ() + 0.8);
            if (world.getCollisionBoxes(null, body).isEmpty() && !world.containsAnyLiquid(body)) {
                return new Vec3d(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            }
        }
        return null;
    }

    private static boolean danger(IBlockState state) {
        return state.getMaterial() == Material.LAVA || state.getMaterial() == Material.FIRE
                || state.getBlock() == net.minecraft.init.Blocks.MAGMA || state.getBlock() == net.minecraft.init.Blocks.CACTUS;
    }

    private static void send(EntityPlayerMP player, WorldServer world, final Vec3d spot) {
        if (player.dimension != world.provider.getDimension()) {
            player.getServer().getPlayerList().transferPlayerToDimension(player, world.provider.getDimension(), new ITeleporter() {
                @Override
                public void placeEntity(World w, Entity entity, float yaw) {
                    entity.setLocationAndAngles(spot.x, spot.y, spot.z, entity.rotationYaw, entity.rotationPitch);
                    entity.motionX = entity.motionY = entity.motionZ = 0;
                }
            });
        }
        player.connection.setPlayerLocation(spot.x, spot.y, spot.z, player.rotationYaw, player.rotationPitch);
        player.connection.sendPacket(new SPacketSoundEffect(BlocksContent.ANCHOR_DEPLETE, SoundCategory.BLOCKS,
                spot.x, spot.y, spot.z, 1.0F, 1.0F));
    }
}

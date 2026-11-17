package com.exiledradio.netherupdatebackport.changes.client;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * 1.16's cave mood ({@code MoodSoundAmbience.LEGACY_CAVE_SETTINGS}, every overworld and End biome): darkness around
 * the player builds up a mood, and at its full the cave sound plays from beyond the dark spot. It replaces 1.12.2's
 * {@code WorldClient.playMoodSoundAndCheckLight}, which plays the sound at random dark spots every 5 to 15 minutes;
 * that one is held off by keeping its timer above zero.
 *
 * <p>In the nether biomes, while stream M's ambience runs, their own mood sounds play instead (as in 1.16) and this
 * stays out of the way.
 */
public final class CaveMood {

    /** {@code LEGACY_CAVE_SETTINGS}: 6000 ticks, 8 blocks, 2 blocks farther. */
    static final int TICK_DELAY = 6000;
    static final int SEARCH_EXTENT = 8;
    static final double OFFSET = 2.0D;

    private static final Field AMBIENCE_TICKS = ReflectionHelper.findField(WorldClient.class, "ambienceTicks", "field_184158_M");

    private float moodiness;

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc.world;
        EntityPlayer player = mc.player;
        if (world == null || player == null || mc.isGamePaused() || !ModConfig.changes.enabled || !ModConfig.changes.caveMood) {
            return;
        }
        try {
            if (AMBIENCE_TICKS.getInt(world) < 2) {
                AMBIENCE_TICKS.setInt(world, 12000);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        BlockPos at = new BlockPos(player.posX, player.posY, player.posZ);
        if (netherAmbience(world.getBiome(at))) {
            return;
        }
        Random random = world.rand;
        double eyeY = player.posY + player.getEyeHeight();
        int span = SEARCH_EXTENT * 2 + 1;
        BlockPos pos = new BlockPos(player.posX + random.nextInt(span) - SEARCH_EXTENT, eyeY + random.nextInt(span) - SEARCH_EXTENT,
                player.posZ + random.nextInt(span) - SEARCH_EXTENT);
        this.moodiness = mood(this.moodiness, world.getLightFor(EnumSkyBlock.SKY, pos), world.getLightFor(EnumSkyBlock.BLOCK, pos));
        if (this.moodiness >= 1.0F) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.5D;
            double z = pos.getZ() + 0.5D;
            double dx = x - player.posX;
            double dy = y - eyeY;
            double dz = z - player.posZ;
            double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            double far = distance + OFFSET;
            // 1.16's SimpleSound.forAmbientMood: linear attenuation, beyond the dark spot as seen from the player.
            mc.getSoundHandler().playSound(new PositionedSoundRecord(SoundEvents.AMBIENT_CAVE.getSoundName(), SoundCategory.AMBIENT, 1.0F, 1.0F,
                    false, 0, ISound.AttenuationType.LINEAR, (float) (player.posX + dx / distance * far), (float) (eyeY + dy / distance * far),
                    (float) (player.posZ + dz / distance * far)));
            this.moodiness = 0.0F;
        } else {
            this.moodiness = Math.max(this.moodiness, 0.0F);
        }
    }

    /** {@link SmallChanges#caveMood}, with 1.16's cave delay. */
    static float mood(float moodiness, int skyLight, int blockLight) {
        return com.exiledradio.netherupdatebackport.changes.SmallChanges.caveMood(moodiness, skyLight, blockLight, TICK_DELAY);
    }

    /** Whether stream M's nether ambience (with its own mood) plays in this biome. */
    static boolean netherAmbience(Biome biome) {
        if (!ModConfig.music.enabled || !ModConfig.music.biomeAmbience) {
            return false;
        }
        return biome == Biomes.HELL || biome.getRegistryName() != null && NetherUpdateBackport.MOD_ID.equals(biome.getRegistryName().getNamespace());
    }
}

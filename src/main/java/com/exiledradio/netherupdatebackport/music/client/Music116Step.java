package com.exiledradio.netherupdatebackport.music.client;

import com.exiledradio.backportcore.presentation.client.GameMusic;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.music.MusicChoice;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

/**
 * 1.16's music choice as Backport Core's "1.16" music step: 1.16.5's {@code Minecraft.getSituationalMusic} with
 * Forge 1.12.2's per-dimension music first, the nether biomes' tracks ({@link MusicChoice}), and 1.16's game music
 * that no longer cuts off another track (20w17a). Core's ticker (1.16.5's {@code MusicManager}) plays it; this class
 * only reads the situation from the client.
 */
final class Music116Step implements GameMusic.Step {

    @Override
    public void choose(GameMusic.Situation s) {
        s.choose(convert(situation(s)));
    }

    static GameMusic.Track convert(MusicChoice.Track t) {
        return new GameMusic.Track(t.event, t.minDelay, t.maxDelay, t.replaceCurrent);
    }

    static MusicChoice.Track situation(GameMusic.Situation s) {
        Minecraft mc = s.mc;
        if (mc.currentScreen instanceof GuiWinGame) {
            return MusicChoice.CREDITS;
        }
        if (mc.player == null || mc.world == null) {
            return MusicChoice.MENU;
        }
        MusicTicker.MusicType own = mc.world.provider.getMusicType();
        MusicChoice.Track ownTrack = own == null ? null : MusicChoice.legacy(own.getMusicLocation(), own.getMinDelay(), own.getMaxDelay());
        WorldProvider provider = mc.player.world.provider;
        MusicChoice.Dimension dimension = provider instanceof WorldProviderHell || provider.getDimensionType() == DimensionType.NETHER
                ? MusicChoice.Dimension.NETHER
                : provider instanceof WorldProviderEnd ? MusicChoice.Dimension.END : MusicChoice.Dimension.OTHER;
        // 1.16.5: under water in an ocean or a river (or while that music plays) the underwater music, outside the
        // End, before the creative and biome music. Only once a version that ships it is installed.
        SoundEvent underWater = GameMusic.underWater();
        if (ownTrack == null && dimension != MusicChoice.Dimension.END && underWater != null
                && (s.isPlaying(underWater) || underWater(mc))) {
            return MusicChoice.game(underWater);
        }
        boolean endBoss = dimension == MusicChoice.Dimension.END && mc.ingameGUI.getBossOverlay().shouldPlayEndBossMusic();
        boolean creativeFlying = mc.player.capabilities.isCreativeMode && mc.player.capabilities.allowFlying;
        NetherBiomes.Kind biome = NetherBiomes.at(mc.world, new BlockPos(mc.player));
        return MusicChoice.inWorld(ownTrack, dimension, endBoss, creativeFlying, biome);
    }

    /** 1.16.5's {@code player.isUnderWater()} in a biome of the ocean or river category. */
    static boolean underWater(Minecraft mc) {
        if (!mc.player.isInsideOfMaterial(Material.WATER)) {
            return false;
        }
        Biome biome = mc.world.getBiome(new BlockPos(mc.player));
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER);
    }
}

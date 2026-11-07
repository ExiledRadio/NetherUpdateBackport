package com.exiledradio.netherupdatebackport.music;

import com.exiledradio.netherupdatebackport.ModConfig;

import net.minecraft.block.BlockJukebox;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Called from patch P2 ({@code asm.JukeboxTransformer}, see {@code docs/PATCHES.md}): the head of
 * {@code BlockJukebox.getComparatorInputOverride}.
 */
public final class MusicHooks {

    private MusicHooks() {
    }

    /** @return true while the stream is on; off, the jukebox runs as vanilla's */
    public static boolean active() {
        return ModConfig.music.enabled;
    }

    /**
     * @return the comparator signal of a jukebox playing one of this mod's discs (Pigstep: 13, as 1.16), or -1 to
     * let vanilla's code answer. 1.12.2 counts the signal from item ids, which gives a modded disc a number in the
     * thousands.
     */
    public static int jukeboxSignal(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof BlockJukebox.TileEntityJukebox) {
            ItemStack record = ((BlockJukebox.TileEntityJukebox) tile).getRecord();
            if (record.getItem() instanceof ItemMusicDisc) {
                return ((ItemMusicDisc) record.getItem()).comparatorSignal();
            }
        }
        return -1;
    }
}

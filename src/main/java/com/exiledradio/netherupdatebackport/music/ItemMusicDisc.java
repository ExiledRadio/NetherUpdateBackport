package com.exiledradio.netherupdatebackport.music;

import net.minecraft.item.ItemRecord;
import net.minecraft.util.SoundEvent;

/**
 * A 1.16 music disc on 1.12.2's own record item, so the jukebox, the "Now Playing" line and the tooltip all work as
 * vanilla's. 1.16's {@code MusicDiscItem} adds the comparator signal, which 1.12.2 works out from the item id; patch
 * P2 ({@code asm.JukeboxTransformer}) reads it from here.
 *
 * <p>The description line is {@code item.record.<name>.desc}, as 1.12.2's records.
 */
public final class ItemMusicDisc extends ItemRecord {

    private final SoundEvent music;
    private final int signal;

    public ItemMusicDisc(String name, SoundEvent sound, int signal) {
        super(name, sound);
        this.music = sound;
        this.signal = signal;
    }

    /** @return the comparator signal from a jukebox playing this disc (1.16's {@code analogOutput}) */
    public int comparatorSignal() {
        return this.signal;
    }

    /** @return the disc's sound event (vanilla's getter is client-only) */
    public SoundEvent music() {
        return this.music;
    }
}

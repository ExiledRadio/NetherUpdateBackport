package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.block.SoundType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sound events and block sound types, for every stream.
 *
 * <h2>Why events are made early</h2>
 * Forge fires the block registry event before the sound event one, but a {@link SoundType} needs its
 * events when the block is built. So {@link #event} makes the event object at once (it is only a name)
 * and keeps it; every event made is registered when the sound registry event comes. A stream asks for
 * events from its block registry handler, or from a static field, and never registers them itself.
 *
 * <p>The sound files and {@code sounds.json} entries come from the stream's generator
 * ({@code tools/assets.py copy_sound}), under this mod's id with the vanilla event names.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class Sounds {

    private static final Map<String, SoundEvent> EVENTS = new LinkedHashMap<String, SoundEvent>();
    private static boolean registered;

    private Sounds() {
    }

    /** @return this mod's sound event with a vanilla event name, such as {@code block.basalt.break} */
    public static synchronized SoundEvent event(String name) {
        SoundEvent event = EVENTS.get(name);
        if (event == null) {
            if (registered) {
                throw new IllegalStateException("Sound event " + name + " asked for after sound registration");
            }
            ResourceLocation id = Content.id(name);
            event = new SoundEvent(id).setRegistryName(id);
            EVENTS.put(name, event);
        }
        return event;
    }

    /**
     * @return a block sound type from 1.16's five events {@code block.<group>.break/step/place/hit/fall},
     * volume and pitch 1 as every 1.16 group but a few
     */
    public static SoundType type(String group) {
        return type(group, 1.0F, 1.0F);
    }

    public static SoundType type(String group, float volume, float pitch) {
        String p = "block." + group + ".";
        return new SoundType(volume, pitch, event(p + "break"), event(p + "step"), event(p + "place"),
                event(p + "hit"), event(p + "fall"));
    }

    /** @return every event made so far, for the self-test */
    public static List<SoundEvent> all() {
        return new ArrayList<SoundEvent>(EVENTS.values());
    }

    @SubscribeEvent
    public static synchronized void register(RegistryEvent.Register<SoundEvent> event) {
        for (SoundEvent sound : EVENTS.values()) {
            event.getRegistry().register(sound);
        }
        registered = true;
    }
}

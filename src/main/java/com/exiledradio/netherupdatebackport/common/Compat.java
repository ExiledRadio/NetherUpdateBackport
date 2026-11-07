package com.exiledradio.netherupdatebackport.common;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Soft integrations with other mods of the family, none of them required.
 *
 * <p>Every call goes through reflection or registry names, so this mod never links against another
 * mod's classes and loads the same with or without it.
 *
 * <h2>Composting</h2>
 * The composter is a 1.14 block. When a mod that has one is installed, the items 1.14 lets you compost
 * from this update (kelp, dried kelp, seagrass, sea pickles) are added to it. Streams call
 * {@link #compost} while registering items; the calls are kept until {@link #init} hands them over.
 */
public final class Compat {

    private static final List<Object[]> COMPOST = new ArrayList<Object[]>();

    private Compat() {
    }

    /** Asks for an item to be compostable with this chance, if a composter exists in this game. */
    public static void compost(float chance, Item item) {
        if (item != null) {
            COMPOST.add(new Object[] {chance, item});
        }
    }

    /** Called from the mod's init, after every registry event. */
    public static void init() {
        if (Loader.isModLoaded(Owners.VILLAGE_AND_PILLAGE) && !COMPOST.isEmpty()) {
            try {
                Method register = Class.forName("com.exiledradio.villagerbackport.block.Compostables")
                        .getMethod("register", float.class, Item.class);
                for (Object[] entry : COMPOST) {
                    register.invoke(null, entry[0], entry[1]);
                }
                NetherUpdateBackport.LOGGER.info("Added {} items to the composter.", COMPOST.size());
            } catch (ReflectiveOperationException | RuntimeException e) {
                NetherUpdateBackport.LOGGER.warn("Could not add items to the composter; they stay uncompostable.", e);
            }
        }
    }
}

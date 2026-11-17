package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.16's sound types for nether blocks 1.12.2 already has (20w06a): {@code SoundType.NETHERRACK},
 * {@code NETHER_BRICKS}, {@code NETHER_ORE}, {@code BONE_BLOCK}, {@code SOUL_SAND}, {@code NETHER_WART} and
 * {@code WART_BLOCK}, set on vanilla's blocks.
 *
 * <p>1.12.2's stone slab is one block for all its kinds, so the nether brick slab keeps stone's sounds. 1.12.2's
 * seeds play no sound when planted, so nether wart's planting sound is not heard (its break sound is).
 */
public final class BlockSounds {

    private static final Field SOUND = ReflectionHelper.findField(Block.class, "blockSoundType", "field_149762_H");

    private static SoundType netherWart;
    private static final Map<Block, SoundType> TYPES = new LinkedHashMap<Block, SoundType>();

    private BlockSounds() {
    }

    private static SoundType netherrack;
    private static SoundType netherBricks;
    private static SoundType netherOre;
    private static SoundType boneBlock;
    private static SoundType soulSand;
    private static SoundType wartBlock;

    /** From pre-init: sound types need their events before the sound registry event. */
    static void preInit() {
        netherrack = Sounds.type("netherrack");
        netherBricks = Sounds.type("nether_bricks");
        netherOre = Sounds.type("nether_ore");
        boneBlock = Sounds.type("bone_block");
        soulSand = Sounds.type("soul_sand");
        wartBlock = Sounds.type("wart_block");
        netherWart = new SoundType(1.0F, 1.0F, Sounds.event("block.nether_wart.break"), SoundEvents.BLOCK_STONE_STEP,
                Sounds.event("item.nether_wart.plant"), SoundEvents.BLOCK_STONE_HIT, SoundEvents.BLOCK_STONE_FALL);
    }

    /** From init: every block is registered, and another mod's replacement of one is the block set. */
    static void init() {
        TYPES.put(Blocks.NETHERRACK, netherrack);
        TYPES.put(Blocks.NETHER_BRICK, netherBricks);
        TYPES.put(Blocks.NETHER_BRICK_FENCE, netherBricks);
        TYPES.put(Blocks.NETHER_BRICK_STAIRS, netherBricks);
        TYPES.put(Blocks.RED_NETHER_BRICK, netherBricks);
        TYPES.put(Blocks.QUARTZ_ORE, netherOre);
        TYPES.put(Blocks.BONE_BLOCK, boneBlock);
        TYPES.put(Blocks.SOUL_SAND, soulSand);
        TYPES.put(Blocks.NETHER_WART, netherWart);
        TYPES.put(Blocks.NETHER_WART_BLOCK, wartBlock);
        try {
            for (Map.Entry<Block, SoundType> e : TYPES.entrySet()) {
                SOUND.set(e.getKey(), e.getValue());
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** @return the sound type each block is given, for the self-test */
    public static Map<Block, SoundType> types() {
        return TYPES;
    }
}

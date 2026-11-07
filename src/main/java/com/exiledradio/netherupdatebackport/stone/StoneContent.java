package com.exiledradio.netherupdatebackport.stone;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Sounds;
import com.exiledradio.netherupdatebackport.common.shape.BlockNetherSlab;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream S: blackstone, polished blackstone and its bricks, gilded blackstone, basalt and polished basalt.
 *
 * <p>Numbers from 1.16.5's {@code Blocks}: strength (hardness, blast resistance) blackstone 1.5/6,
 * polished blackstone 2/6, its bricks, chiseled and cracked forms 1.5/6, blackstone and polished
 * blackstone brick slabs 2/6, basalt 1.25/4.2, plate and button 0.5. All need a pickaxe but the button.
 * Basalt and gilded blackstone have 1.16's own sound groups.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class StoneContent {

    public static final String[] NAMES = {
            "basalt", "polished_basalt",
            "blackstone", "blackstone_stairs", "blackstone_slab", "blackstone_double_slab", "blackstone_wall",
            "polished_blackstone", "polished_blackstone_stairs", "polished_blackstone_slab",
            "polished_blackstone_double_slab", "polished_blackstone_wall", "polished_blackstone_pressure_plate",
            "polished_blackstone_button",
            "polished_blackstone_bricks", "polished_blackstone_brick_stairs", "polished_blackstone_brick_slab",
            "polished_blackstone_brick_double_slab", "polished_blackstone_brick_wall",
            "cracked_polished_blackstone_bricks", "chiseled_polished_blackstone", "gilded_blackstone",
    };

    public static Block basalt;
    public static Block blackstone;

    /** Blocks that get an item, in creative-tab order. */
    private static final List<Block> WITH_ITEMS = new ArrayList<Block>();

    private StoneContent() {
    }

    public static boolean enabled() {
        return ModConfig.stone.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BlockOwnership.declare(NetherUpdateBackport.MOD_ID, NAMES);
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        SoundType basaltSound = Sounds.type("basalt");

        basalt = item(Content.block(r, "basalt", new Shapes.Pillar(Material.ROCK, MapColor.BLACK, 1.25F, 4.2F, basaltSound), "pickaxe"));
        item(Content.block(r, "polished_basalt", new Shapes.Pillar(Material.ROCK, MapColor.BLACK, 1.25F, 4.2F, basaltSound), "pickaxe"));

        blackstone = item(Content.block(r, "blackstone", plain(1.5F), "pickaxe"));
        set(r, "blackstone", blackstone, "blackstone_slab", 2.0F);

        Block polished = item(Content.block(r, "polished_blackstone", plain(2.0F), "pickaxe"));
        set(r, "polished_blackstone", polished, "polished_blackstone_slab", 2.0F);
        item(Content.block(r, "polished_blackstone_pressure_plate",
                new Shapes.PressurePlate(Material.ROCK, MapColor.BLACK, BlockPressurePlate.Sensitivity.MOBS, SoundType.STONE), "pickaxe"));
        item(Content.block(r, "polished_blackstone_button", new Shapes.StoneButton(), null));

        Block bricks = item(Content.block(r, "polished_blackstone_bricks", plain(1.5F), "pickaxe"));
        set(r, "polished_blackstone_brick", bricks, "polished_blackstone_brick_slab", 2.0F);
        item(Content.block(r, "cracked_polished_blackstone_bricks", plain(1.5F), "pickaxe"));
        item(Content.block(r, "chiseled_polished_blackstone", plain(1.5F), "pickaxe"));

        Block gilded = new BlockGildedBlackstone(Sounds.type("gilded_blackstone"));
        item(Content.block(r, "gilded_blackstone", gilded, "pickaxe"));

        GameRegistry.registerWorldGenerator(new BlackstoneGeneration(), 10);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (Block block : WITH_ITEMS) {
            Content.registerBlockItem(event.getRegistry(), block);
        }
    }

    private static Block plain(float hardness) {
        return new Shapes.Plain(Material.ROCK, MapColor.BLACK, hardness, 6.0F, SoundType.STONE);
    }

    /** Stairs, slab and wall of a block: {@code <prefix>_stairs}, the slab, {@code <prefix>_wall}. */
    private static void set(IForgeRegistry<Block> r, String prefix, Block base, String slab, float slabHardness) {
        item(Content.block(r, prefix + "_stairs", new Shapes.Stairs(base.getDefaultState()), "pickaxe"));
        BlockNetherSlab.Half half = new BlockNetherSlab.Half(base, base.getDefaultState(), slabHardness, 6.0F);
        BlockNetherSlab.Double full = new BlockNetherSlab.Double(base, base.getDefaultState(), slabHardness, 6.0F, half);
        item(Content.slab(r, slab, half, full, "pickaxe"));
        Block wall = Content.block(r, prefix + "_wall", new Shapes.Wall(base), "pickaxe");
        Content.multipart(wall);
        item(wall);
    }

    private static Block item(Block block) {
        WITH_ITEMS.add(block);
        return block;
    }
}

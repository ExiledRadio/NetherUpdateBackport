package com.exiledradio.netherupdatebackport.common.shape;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButtonStone;
import net.minecraft.block.BlockButtonWood;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockWall;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Vanilla's block shapes with their constructors opened, for every stream's building blocks.
 *
 * <p>Vanilla's stairs, walls, buttons, plates and pillars already do everything 1.16's do (corners,
 * connections, redstone, axis placement); 1.12.2 only hides their constructors or ties them to one
 * material. Nothing here changes how they behave.
 *
 * <h2>Strength</h2>
 * Every {@code resistance} here is 1.16's blast resistance, as the wiki and 1.16's code give it. 1.12.2's
 * {@code setResistance} stores its argument tripled and explosions read it divided by five, so
 * {@link #strength} passes {@code blast * 5 / 3} to land on the same number.
 */
public final class Shapes {

    private Shapes() {
    }

    /** Sets hardness and 1.16's blast resistance on any block. */
    public static <B extends Block> B strength(B block, float hardness, float blast) {
        block.setHardness(hardness);
        block.setResistance(blast * 5.0F / 3.0F);
        return block;
    }

    /** A plain full block with a material, map color, strength and sound. */
    public static class Plain extends Block {
        public Plain(Material material, MapColor color, float hardness, float resistance, SoundType sound) {
            super(material, color);
            strength(this, hardness, resistance);
            setSoundType(sound);
        }
    }

    /** A block with an axis, as logs, basalt and stems. */
    public static class Pillar extends BlockRotatedPillar {
        private final MapColor color;

        public Pillar(Material material, MapColor color, float hardness, float resistance, SoundType sound) {
            super(material, color);
            this.color = color;
            strength(this, hardness, resistance);
            setSoundType(sound);
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.color;
        }
    }

    /** Stairs made of a block; strength and sound come from it, as vanilla's. */
    public static final class Stairs extends BlockStairs {
        public Stairs(IBlockState base) {
            super(base);
            this.useNeighborBrightness = true;
        }
    }

    /**
     * A wall made of a block.
     *
     * <p>Extending {@link BlockWall} is what makes it connect to vanilla's walls and every modded wall
     * built the same way. Vanilla's {@code variant} property (cobblestone or mossy) comes with it and is
     * pinned to its first value and hidden from the blockstate files.
     */
    public static final class Wall extends BlockWall {
        public Wall(Block base) {
            super(base);
        }

        @Override
        @SuppressWarnings("deprecation")
        public String getLocalizedName() {
            // Vanilla's appends ".normal", the name of the cobblestone variant.
            return net.minecraft.util.text.translation.I18n.translateToLocal(getTranslationKey() + ".name");
        }

        @Override
        public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
            items.add(new ItemStack(this));
        }

        @Override
        public int damageDropped(IBlockState state) {
            return 0;
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState();
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return 0;
        }
    }

    /** A stone button (20 ticks, arrows do not press it). */
    public static final class StoneButton extends BlockButtonStone {
        public StoneButton() {
            strength(this, 0.5F, 0.5F);
            setSoundType(SoundType.STONE);
        }
    }

    /** A wooden button (30 ticks, arrows press it). */
    public static final class WoodButton extends BlockButtonWood {
        public WoodButton() {
            strength(this, 0.5F, 0.5F);
            setSoundType(SoundType.WOOD);
        }
    }

    /** A pressure plate: {@code MOBS} for stone (players and mobs), {@code EVERYTHING} for wood. */
    public static final class PressurePlate extends BlockPressurePlate {
        private final MapColor color;

        public PressurePlate(Material material, MapColor color, Sensitivity sensitivity, SoundType sound) {
            super(material, sensitivity);
            this.color = color;
            strength(this, 0.5F, 0.5F);
            setSoundType(sound);
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.color;
        }
    }
}

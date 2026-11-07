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
            // BlockRotatedPillar leaves the first axis (X) as the default; logs, stems and chains stand upright.
            setDefaultState(this.blockState.getBaseState().withProperty(AXIS, net.minecraft.util.EnumFacing.Axis.Y));
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

    /** A door; the item is a {@code net.minecraft.item.ItemDoor} the caller registers. */
    public static final class Door extends net.minecraft.block.BlockDoor {
        private net.minecraft.item.Item item;

        public Door(Material material, SoundType sound, float hardness) {
            super(material);
            strength(this, hardness, hardness);
            setSoundType(sound);
        }

        public void setItem(net.minecraft.item.Item item) {
            this.item = item;
        }

        @Override
        public net.minecraft.item.Item getItemDropped(IBlockState state, java.util.Random rand, int fortune) {
            return state.getValue(HALF) == EnumDoorHalf.UPPER ? net.minecraft.init.Items.AIR : this.item;
        }

        @Override
        public ItemStack getItem(net.minecraft.world.World world, BlockPos pos, IBlockState state) {
            return new ItemStack(this.item);
        }
    }

    /** A trapdoor. */
    public static final class Trapdoor extends net.minecraft.block.BlockTrapDoor {
        public Trapdoor(Material material, SoundType sound, float hardness) {
            super(material);
            strength(this, hardness, hardness);
            setSoundType(sound);
        }
    }

    /**
     * A wooden fence. It joins every other wooden fence (vanilla's join only fences of their own
     * material, which these share: {@code Material.WOOD}) and fence gates.
     */
    public static final class Fence extends net.minecraft.block.BlockFence {
        public Fence(Material material, MapColor color, SoundType sound, float hardness, float blast) {
            super(material, color);
            strength(this, hardness, blast);
            setSoundType(sound);
        }
    }

    /** A fence gate. Its map color is passed as vanilla's plank type's; the gate's own color is overridden. */
    public static final class FenceGate extends net.minecraft.block.BlockFenceGate {
        private final MapColor color;

        public FenceGate(MapColor color, SoundType sound, float hardness, float blast) {
            super(net.minecraft.block.BlockPlanks.EnumType.OAK);
            this.color = color;
            strength(this, hardness, blast);
            setSoundType(sound);
        }

        @Override
        @SuppressWarnings("deprecation")
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.color;
        }
    }
}

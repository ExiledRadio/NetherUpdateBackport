package com.exiledradio.netherupdatebackport.common.shape;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A 1.16 slab in 1.12.2's two-block form, the family's way (Village &amp; Pillage's building slabs).
 *
 * <h2>Two blocks per slab</h2>
 * 1.12.2 has no "type=double" slab state: a slab and the full block two of them make are separate
 * blocks, and {@link net.minecraft.item.ItemSlab} swaps one for the other when a second slab is placed
 * on the first. So each slab registers {@link Half} and {@link Double}; the double has no item of its own
 * and drops two halves, as vanilla's do.
 *
 * <h2>The one-value variant</h2>
 * {@link BlockSlab} insists on a variant property (how vanilla packs eight stone slabs into one block,
 * compared by {@code ItemSlab} before merging). These are one block each, so the property has a single
 * value, hidden from the blockstate files with {@code Content.ignore}.
 *
 * <p>{@link #isDouble()} is answered by the subclass rather than a field, because {@code BlockSlab}'s
 * constructor calls it before any field of this class is assigned.
 *
 * <h2>Flammable wood slabs</h2>
 * Strength, sound and map color come from the block the slab is made of. Fire behavior is the caller's
 * ({@code Blocks.FIRE.setFireInfo}); nether woods never burn, so they set none.
 */
public abstract class BlockNetherSlab extends BlockSlab {

    public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

    /** The only variant there is. */
    public enum Variant implements IStringSerializable {
        DEFAULT;

        @Override
        public String getName() {
            return "default";
        }
    }

    private final MapColor color;

    /** The half slab every one of these drops and is picked as; set once both halves exist. */
    Block half;

    BlockNetherSlab(Block base, IBlockState baseState, float hardness, float resistance) {
        super(baseState.getMaterial(), base.getMapColor(baseState, null, null));
        this.color = base.getMapColor(baseState, null, null);
        Shapes.strength(this, hardness, resistance);
        setSoundType(base.getSoundType());

        IBlockState state = this.blockState.getBaseState().withProperty(VARIANT, Variant.DEFAULT);
        if (!isDouble()) {
            state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
        }
        setDefaultState(state);
        this.useNeighborBrightness = !isDouble();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return isDouble() ? new BlockStateContainer(this, VARIANT) : new BlockStateContainer(this, HALF, VARIANT);
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.color;
    }

    @Override
    public String getTranslationKey(int meta) {
        return getTranslationKey();
    }

    @Override
    public IProperty<?> getVariantProperty() {
        return VARIANT;
    }

    @Override
    public Comparable<?> getTypeForItem(ItemStack stack) {
        return Variant.DEFAULT;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = getDefaultState();
        if (!isDouble()) {
            state = state.withProperty(HALF, (meta & 8) != 0 ? EnumBlockHalf.TOP : EnumBlockHalf.BOTTOM);
        }
        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return !isDouble() && state.getValue(HALF) == EnumBlockHalf.TOP ? 8 : 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this.half);
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.half);
    }

    /** @return the half slab (itself, or the half this double is made of) */
    public Block half() {
        return this.half;
    }

    /** The slab itself. */
    public static final class Half extends BlockNetherSlab {
        public Half(Block base, IBlockState baseState, float hardness, float resistance) {
            super(base, baseState, hardness, resistance);
            this.half = this;
        }

        @Override
        public boolean isDouble() {
            return false;
        }
    }

    /** Two slabs stacked into one block space. */
    public static final class Double extends BlockNetherSlab {
        public Double(Block base, IBlockState baseState, float hardness, float resistance, Half half) {
            super(base, baseState, hardness, resistance);
            this.half = half;
        }

        @Override
        public boolean isDouble() {
            return true;
        }
    }
}

package com.exiledradio.netherupdatebackport.fungus;

import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Weeping vines (growing down from a ceiling) and twisting vines (growing up from a floor), each a head
 * block (the growing tip, with an age) over body blocks, as 1.16's {@code AbstractTopPlantBlock} and
 * {@code AbstractBodyPlantBlock}.
 *
 * <h2>Age in 16 steps</h2>
 * 1.16's tip has ages 0-25 and grows while below 25; 1.12.2 has 16 metadata values. The tip's age runs 0-15
 * and a new tip takes the next age 15 times in 25, which keeps the expected length of a vine 1.16's, as
 * Update Aquatic's kelp does.
 *
 * <h2>Rules</h2>
 * Growth 10% per random tick into air; bone meal grows a run of tips (1.16's {@code 0.826} chain); a vine
 * needs its own vine or a solid face behind it and breaks when that goes; a tip with vine beyond it becomes
 * body and a body with nothing beyond becomes a tip. Climbable. Drops itself with shears or Silk Touch,
 * otherwise a third of the time (more with Fortune). <b>Now:</b> shears on the tip stop its growth.
 */
public abstract class BlockNetherVines extends Block {

    public static final int MAX_AGE = 15;
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, MAX_AGE);

    /** 1.16's {@code table_bonus} for weeping and twisting vines, Fortune 0 to III. */
    static final float[] DROP_CHANCE = {0.33F, 0.55F, 0.77F, 1.0F};

    static final SoundEvent CROP = Sounds.event("block.growing_plant.crop");

    final EnumFacing growth;

    BlockNetherVines(MapColor color, SoundType sound, EnumFacing growth) {
        super(Material.PLANTS, color);
        setSoundType(sound);
        setHardness(0.0F);
        this.growth = growth;
    }

    abstract Head head();

    abstract Body body();

    boolean isVine(Block block) {
        return block == head() || block == body();
    }

    /** @return true if the block behind it (opposite its growth) holds it up */
    public boolean canStay(World world, BlockPos pos) {
        BlockPos behind = pos.offset(this.growth.getOpposite());
        IBlockState support = world.getBlockState(behind);
        return isVine(support.getBlock()) || support.getBlockFaceShape(world, behind, this.growth) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && canStay(world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return isVine(world.getBlockState(pos.offset(this.growth)).getBlock()) ? body().getDefaultState() : head().randomTip(world.rand);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (!canStay(world, pos)) {
            world.scheduleUpdate(pos, this, 1);
            return;
        }
        boolean beyond = isVine(world.getBlockState(pos.offset(this.growth)).getBlock());
        if (this == head() && beyond) {
            world.setBlockState(pos, body().getDefaultState(), 2);
        } else if (this == body() && !beyond) {
            world.setBlockState(pos, head().randomTip(world.rand), 2);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (!canStay(world, pos)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.BlockRenderLayer getRenderLayer() {
        return net.minecraft.util.BlockRenderLayer.CUTOUT;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : RANDOM;
        if (random.nextFloat() < DROP_CHANCE[Math.max(0, Math.min(fortune, 3))]) {
            drops.add(new ItemStack(head()));
        }
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        if (tool.getItem() == Items.SHEARS) {
            if (!world.isRemote) {
                spawnAsEntity(world, pos, new ItemStack(head()));
            }
            player.addExhaustion(0.005F);
            return;
        }
        super.harvestBlock(world, player, pos, state, te, tool);
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(head());
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(head());
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(head());
    }

    /** 1.16's {@code PlantBlockHelper.getBlocksToGrowWhenBonemealed}: 1 + more while a 0.826-shrinking chance holds. */
    static int blocksToGrow(Random rand) {
        double chance = 1.0;
        int n = 0;
        while (rand.nextDouble() < chance) {
            chance *= 0.826;
            n++;
        }
        return n;
    }

    /** The growing tip. */
    public static class Head extends BlockNetherVines implements IGrowable {
        private final AxisAlignedBB box;
        Body body;

        Head(MapColor color, SoundType sound, EnumFacing growth, AxisAlignedBB box) {
            super(color, sound, growth);
            this.box = box;
            setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0));
            setTickRandomly(true);
        }

        @Override
        Head head() {
            return this;
        }

        @Override
        Body body() {
            return this.body;
        }

        IBlockState randomTip(Random rand) {
            // 1.16 places a tip at a random age 0-24 of 25.
            return getDefaultState().withProperty(AGE, rand.nextInt(25) * (MAX_AGE + 1) / 25);
        }

        /** @return the state of a new tip grown from one of this age (1.16's age + 1, folded) */
        IBlockState next(IBlockState from, Random rand) {
            int age = from.getValue(AGE);
            if (age < MAX_AGE && rand.nextInt(25) < 15) {
                age++;
            }
            return getDefaultState().withProperty(AGE, age);
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, AGE);
        }

        @Override
        @SuppressWarnings("deprecation")
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(AGE, meta & 15);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(AGE);
        }

        @Override
        @SuppressWarnings("deprecation")
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
            return this.box;
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
            if (!canStay(world, pos)) {
                world.destroyBlock(pos, true);
                return;
            }
            BlockPos into = pos.offset(this.growth);
            if (state.getValue(AGE) < MAX_AGE && world.isAirBlock(into)
                    && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(world, into, world.getBlockState(into), rand.nextDouble() < 0.1)) {
                world.setBlockState(into, next(state, rand));
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(world, into, world.getBlockState(into), world.getBlockState(into));
            }
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                        EnumFacing facing, float hitX, float hitY, float hitZ) {
            ItemStack held = player.getHeldItem(hand);
            if (held.getItem() != Items.SHEARS || state.getValue(AGE) == MAX_AGE) {
                return false;
            }
            if (!world.isRemote) {
                world.setBlockState(pos, state.withProperty(AGE, MAX_AGE), 2);
                world.playSound(null, pos, CROP, SoundCategory.BLOCKS, 1.0F, 1.0F);
                held.damageItem(1, player);
            }
            return true;
        }

        @Override
        public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
            return world.isAirBlock(pos.offset(this.growth));
        }

        @Override
        public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
            return true;
        }

        @Override
        public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
            // 1.16: the tips grown each take the next age, starting from this tip's age + 1.
            BlockPos at = pos.offset(this.growth);
            IBlockState tip = next(state, rand);
            int n = blocksToGrow(rand);
            for (int i = 0; i < n && world.isAirBlock(at); i++) {
                world.setBlockState(at, tip);
                at = at.offset(this.growth);
                tip = next(tip, rand);
            }
        }
    }

    /** The vine behind the tip. */
    public static class Body extends BlockNetherVines implements IGrowable {
        private static final AxisAlignedBB BOX = new AxisAlignedBB(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 1, 15 / 16.0);
        final Head head;

        Body(Head head, MapColor color, SoundType sound) {
            super(color, sound, head.growth);
            this.head = head;
            head.body = this;
        }

        @Override
        Head head() {
            return this.head;
        }

        @Override
        Body body() {
            return this;
        }

        @Override
        @SuppressWarnings("deprecation")
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
            return BOX;
        }

        @Nullable
        private BlockPos tip(World world, BlockPos pos) {
            BlockPos at = pos;
            Block block;
            do {
                at = at.offset(this.growth);
                block = world.getBlockState(at).getBlock();
            } while (block == this);
            return block == this.head ? at : null;
        }

        @Override
        public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
            BlockPos tip = tip(world, pos);
            return tip != null && world.isAirBlock(tip.offset(this.growth));
        }

        @Override
        public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
            return true;
        }

        @Override
        public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
            BlockPos tip = tip(world, pos);
            if (tip != null) {
                this.head.grow(world, rand, tip, world.getBlockState(tip));
            }
        }
    }
}

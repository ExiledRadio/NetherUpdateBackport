package com.exiledradio.netherupdatebackport.soul.campfire;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.16's soul campfire: Village &amp; Pillage's campfire (1.14), copied as the family's pattern, with 1.16's
 * soul differences: light 10, 2 damage, drops soul soil, no lava sparks.
 *
 * <p>The rest of these notes are the campfire's.
 *
 * <h2>State</h2>
 * Three properties, all kept in metadata: {@code facing} (the way the logs lie, and which slot is
 * which), {@code lit}, and {@code signal_fire} - true while a hay bale sits directly underneath, which
 * sends the smoke up three times as high. 1.16's fourth, {@code waterlogged}, is not a property here: the
 * water is Fluidlogged API's, through Backport Core's {@link Waterloggable} (and Backport Core's 1.13+ rules, {@code waterlog/fluidlogged/WaterRules}).
 *
 * <h2>Water (1.16.5's {@code CampfireBlock})</h2>
 * Only with Fluidlogged API installed ({@link Waterlogging#isAvailable()}). Without it the campfire is never
 * waterlogged: it is placed lit, and a water bucket used on a lit one puts it out (hiss and smoke burst) and
 * leaves an empty bucket, as before waterlogging.
 * <ul>
 * <li>Placed into a water source it is waterlogged and unlit.</li>
 * <li>Water coming in (a water bucket, a command, a source forming) puts a lit one out with the hiss
 * ({@code entity.generic.extinguish_fire}) and the smoke burst; what is cooking stays.</li>
 * <li>A waterlogged campfire cannot be lit: flint and steel and fire charges act as on any block (fire in
 * front), burning arrows and fireballs do nothing.</li>
 * </ul>
 *
 * <h2>What it does, from minecraft.wiki (current Java)</h2>
 * <ul>
 * <li><b>Light 10</b> while lit, none when out.</li>
 * <li><b>Hurts what stands in it</b>: 2 points of fire damage, which the half-second hurt cooldown turns
 * into one hit every ten ticks. Only living things, only while lit, and not those immune to fire, with
 * Fire Resistance (the damage is fire damage, so 1.12.2 already skips it) or wearing Frost Walker boots.
 * Sneaking does <i>not</i> protect anyone: 1.14's snapshots briefly had that, and 19w04a took it out
 * before release. It never sets anything alight, so a mob killed in one drops raw meat.</li>
 * <li><b>Cooks</b> up to four foods at once, no fuel - see {@link TileEntitySoulCampfire}. Food goes on lit
 * or unlit campfires alike, as in current Java (1.14.4 itself only took it while lit).</li>
 * <li><b>Lit</b> by flint and steel, a fire charge or a burning arrow. Placed lit.</li>
 * <li><b>Put out</b> by a shovel, a water bucket or a splash water bottle - never from below, and never
 * by rain. Putting it out keeps what is cooking; it carries on when relit.</li>
 * <li><b>Drops</b> one soul soil, or itself with Silk Touch, plus whatever was cooking.</li>
 * <li>Mobs path around a lit one rather than through it.</li>
 * </ul>
 *
 */
public class BlockSoulCampfire extends Block implements ITileEntityProvider, Waterloggable {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final PropertyBool SIGNAL_FIRE = PropertyBool.create("signal_fire");

    /** 1.14's shape: a slab seven sixteenths high. */
    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 7.0D / 16.0D, 1.0D);

    /** The block event 1.12.2 plays as a fire going out: a hiss, as 1.14's shovel and potion use. */
    static final int EXTINGUISH_EVENT = 1009;

    public BlockSoulCampfire() {
        super(Material.WOOD, MapColor.OBSIDIAN);
        // 1.14: hardness 2, wood sounds, quickest with an axe but breakable by hand.
        setHardness(2.0F);
        setResistance(2.0F);
        setSoundType(SoundType.WOOD);
        setHarvestLevel("axe", 0);
        setDefaultState(this.blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(LIT, Boolean.TRUE)
                .withProperty(SIGNAL_FIRE, Boolean.FALSE));
    }

    /**
     * @return true for a lit campfire - the hook for bees calming over smoke (1.15). See the class
     * notes; nothing in this mod calls it.
     */
    public static boolean isSmokeSource(IBlockState state) {
        return state.getBlock() instanceof BlockSoulCampfire && state.getValue(LIT).booleanValue();
    }

    /** @return true if this state is one of this mod's campfires, lit */
    public static boolean isLit(IBlockState state) {
        return isSmokeSource(state);
    }

    // ---------------------------------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------------------------------

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, LIT, SIGNAL_FIRE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
                .withProperty(LIT, Boolean.valueOf((meta & 4) != 0))
                .withProperty(SIGNAL_FIRE, Boolean.valueOf((meta & 8) != 0));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = state.getValue(FACING).getHorizontalIndex();
        if (state.getValue(LIT).booleanValue()) {
            meta |= 4;
        }
        if (state.getValue(SIGNAL_FIRE).booleanValue()) {
            meta |= 8;
        }
        return meta;
    }

    /**
     * Placed lit, facing the way the player looks, a signal fire if there is hay underneath; placed into a
     * water source, unlit (1.16.5: {@code LIT = !waterlogged}).
     */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, EntityLivingBase placer) {
        return getDefaultState()
                .withProperty(FACING, placer.getHorizontalFacing())
                .withProperty(LIT, Boolean.valueOf(!Waterlogging.isAvailable() || !Waterlogging.hasWaterSource(world, pos)))
                .withProperty(SIGNAL_FIRE, Boolean.valueOf(isHay(world.getBlockState(pos.down()))));
    }

    /** 1.14's {@code updateShape} for the block below: hay placed or taken away under it. */
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        if (!from.equals(pos.down())) {
            return;
        }
        boolean signal = isHay(world.getBlockState(from));
        if (signal != state.getValue(SIGNAL_FIRE).booleanValue()) {
            world.setBlockState(pos, state.withProperty(SIGNAL_FIRE, Boolean.valueOf(signal)), 3);
        }
    }

    static boolean isHay(IBlockState below) {
        return below.getBlock() == Blocks.HAY_BLOCK;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    // ---------------------------------------------------------------------------------------------
    // Using it
    // ---------------------------------------------------------------------------------------------

    /**
     * Right-click: light it, put it out, or put food on it, depending on what is in the hand.
     *
     * <p>Lighting and putting out are handled here rather than by the items themselves because in
     * 1.12.2 flint and steel would set fire to the block in front, and a bucket would pour water
     * beside it. A lit campfire hands flint and steel back to the item, which lights the space next
     * to it as 1.14's does.
     */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        boolean lit = state.getValue(LIT).booleanValue();
        Item item = held.getItem();

        // 1.16.5's canLight: never while waterlogged; the item then lights the space in front, as on any block.
        boolean canLight = !lit && !Waterlogging.hasWater(world, pos);
        if (canLight && item instanceof ItemFlintAndSteel) {
            if (!world.isRemote) {
                world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F,
                        world.rand.nextFloat() * 0.4F + 0.8F);
                setLit(world, pos, state, true);
                held.damageItem(1, player);
            }
            return true;
        }
        if (canLight && item instanceof ItemFireball) {
            if (!world.isRemote) {
                world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0F,
                        (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F);
                setLit(world, pos, state, true);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
            return true;
        }
        if (lit && side != EnumFacing.DOWN && item.getToolClasses(held).contains("shovel")) {
            if (!world.isRemote) {
                extinguish(world, pos, state, false);
                held.damageItem(1, player);
            }
            return true;
        }
        // With Fluidlogged API a water bucket is left to the bucket: it waterlogs the campfire, which puts it out
        // (onWaterChanged). Without it the campfire can't hold water, so the bucket puts it out here.
        if (lit && item == Items.WATER_BUCKET && !Waterlogging.isAvailable()) {
            if (!world.isRemote) {
                extinguish(world, pos, state, true);
                if (!player.capabilities.isCreativeMode) {
                    player.setHeldItem(hand, new ItemStack(Items.BUCKET));
                }
                player.addStat(StatList.getObjectUseStats(Items.WATER_BUCKET));
            }
            return true;
        }

        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntitySoulCampfire && SoulCampfireRecipes.cooks(held)) {
            if (!world.isRemote && ((TileEntitySoulCampfire) tile).placeFood(
                    player.capabilities.isCreativeMode ? held.copy() : held,
                    com.exiledradio.netherupdatebackport.ModConfig.soul.campfireCookTime)) {
                player.addStat(StatList.getObjectUseStats(item));
            }
            return true;
        }
        return false;
    }

    /** Lights or puts out, without the extinguishing effects. */
    static void setLit(World world, BlockPos pos, IBlockState state, boolean lit) {
        world.setBlockState(pos, state.withProperty(LIT, Boolean.valueOf(lit)), 11);
    }

    /**
     * Puts a lit campfire out: the hiss, the smoke burst, and the food kept for when it is relit.
     *
     * @param burst 1.14's waterlogging puff - twenty smoke plumes at once. The shovel has none.
     */
    public static void extinguish(World world, BlockPos pos, IBlockState state, boolean burst) {
        if (!isLit(state) || world.isRemote) {
            return;
        }
        world.playEvent(null, EXTINGUISH_EVENT, pos, 0);
        if (burst) {
            PacketSoulCampfireDowse.send(world, pos, state.getValue(SIGNAL_FIRE).booleanValue());
        }
        setLit(world, pos, state, false);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntitySoulCampfire) {
            ((TileEntitySoulCampfire) tile).dowse();
        }
    }

    /**
     * 1.16.5's {@code placeLiquid}: water coming into a lit campfire puts it out, with the hiss and the smoke burst,
     * keeping what is cooking.
     */
    @Override
    public void onWaterChanged(World world, BlockPos pos, IBlockState state, boolean hasWater) {
        if (!hasWater || world.isRemote) {
            return;
        }
        IBlockState now = world.getBlockState(pos);
        if (!isLit(now)) {
            return;
        }
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        PacketSoulCampfireDowse.send(world, pos, now.getValue(SIGNAL_FIRE).booleanValue());
        setLit(world, pos, now, false);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntitySoulCampfire) {
            ((TileEntitySoulCampfire) tile).dowse();
        }
    }

    /** Lights an unlit campfire from outside: a burning arrow. Never a waterlogged one. */
    public static void ignite(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() instanceof BlockSoulCampfire && !state.getValue(LIT).booleanValue() && !world.isRemote
                && !Waterlogging.hasWater(world, pos)) {
            setLit(world, pos, state, true);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Contact damage
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!entity.isImmuneToFire() && state.getValue(LIT).booleanValue() && entity instanceof EntityLivingBase
                && !EnchantmentHelper.hasFrostWalkerEnchantment((EntityLivingBase) entity)) {
            entity.attackEntityFrom(DamageSource.IN_FIRE, 2.0F);
        }
    }

    @Nullable
    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world, BlockPos pos,
                                          @Nullable EntityLiving entity) {
        return state.getValue(LIT).booleanValue() ? PathNodeType.DAMAGE_FIRE : null;
    }

    // ---------------------------------------------------------------------------------------------
    // Light, drops, tile
    // ---------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public int getLightValue(IBlockState state) {
        return state.getValue(LIT).booleanValue() ? 10 : 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getLightValue(state);
    }

    /** One soul soil (stream L's own, by name); Silk Touch drops the campfire itself, through {@link #canSilkHarvest}. */
    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
                         int fortune) {
        net.minecraft.item.Item soil = com.exiledradio.netherupdatebackport.common.Owners.ours("soul_soil");
        if (soil != null) {
            drops.add(new ItemStack(soil));
        }
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world,
                                  BlockPos pos, EntityPlayer player) {
        return new ItemStack(this);
    }

    /** Whatever was cooking falls out when the campfire goes. */
    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntitySoulCampfire) {
            for (ItemStack stack : ((TileEntitySoulCampfire) tile).items()) {
                if (!stack.isEmpty()) {
                    InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
            ((TileEntitySoulCampfire) tile).items().clear();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntitySoulCampfire();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    // ---------------------------------------------------------------------------------------------
    // Shape and rendering
    // ---------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SHAPE;
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
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /**
     * 1.16's {@code animateTick} for the soul campfire: a crackle one tick in ten, and no lava sparks (1.16's
     * {@code spawnParticles} is false for it). The smoke itself comes from the tile entity's tick.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (!state.getValue(LIT).booleanValue()) {
            return;
        }
        if (rand.nextInt(10) == 0 && SoulCampfireContent.crackle != null) {
            world.playSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoulCampfireContent.crackle,
                    SoundCategory.BLOCKS, 0.5F + rand.nextFloat(), rand.nextFloat() * 0.7F + 0.6F, false);
        }
    }
}

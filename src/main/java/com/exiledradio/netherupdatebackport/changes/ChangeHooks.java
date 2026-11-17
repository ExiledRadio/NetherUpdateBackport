package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.ModConfig;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * The hooks of patches P5 to P11 ({@code asm.MechanicsTransformer}, docs/PATCHES.md): 1.16's behavior where
 * vanilla 1.12.2 code calls in. Each reads its setting on every call, so a change switched off in the settings
 * screen is vanilla's code again at once.
 */
public final class ChangeHooks {

    private static final Field IDLE_TIME = ReflectionHelper.findField(EntityLivingBase.class, "idleTime", "field_70708_bq");

    private ChangeHooks() {
    }

    private static boolean on() {
        return ModConfig.changes.enabled;
    }

    // P5: knockback resistance --------------------------------------------------------------------------------

    public static boolean knockbackActive() {
        return on() && ModConfig.changes.knockbackResistance;
    }

    /**
     * 1.16.5's {@code LivingEntity.knockback}, after Forge's own event as in 1.12.2: the strength is scaled by
     * {@code 1 - knockback resistance} (1.12.2 rolls the resistance as a chance to take none).
     */
    public static void knockBack(EntityLivingBase entity, Entity attacker, float strength, double xRatio, double zRatio) {
        LivingKnockBackEvent event = ForgeHooks.onLivingKnockBack(entity, attacker, strength, xRatio, zRatio);
        if (event.isCanceled()) {
            return;
        }
        strength = event.getStrength();
        xRatio = event.getRatioX();
        zRatio = event.getRatioZ();
        strength = (float) (strength * (1.0D - entity.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue()));
        if (strength <= 0.0F) {
            return;
        }
        entity.isAirBorne = true;
        Vec3d push = new Vec3d(xRatio, 0.0D, zRatio).normalize().scale(strength);
        entity.motionX = entity.motionX / 2.0D - push.x;
        entity.motionZ = entity.motionZ / 2.0D - push.z;
        if (entity.onGround) {
            entity.motionY = Math.min(0.4D, entity.motionY / 2.0D + strength);
        }
    }

    // P6: Mending ---------------------------------------------------------------------------------------------

    /**
     * Stands in for {@code EnchantmentHelper.getEnchantedItem(Mending, player)} in the XP orb's pickup: 1.16.5's
     * {@code getRandomItemWith(MENDING, player, ItemStack::isDamaged)}, a random one of the damaged items that
     * have it, so an undamaged one never takes the orb.
     */
    public static ItemStack mendingItem(Enchantment enchantment, EntityLivingBase entity) {
        if (!on() || !ModConfig.changes.mending || enchantment != Enchantments.MENDING) {
            return EnchantmentHelper.getEnchantedItem(enchantment, entity);
        }
        List<ItemStack> damaged = new ArrayList<ItemStack>();
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = entity.getItemStackFromSlot(slot);
            if (!stack.isEmpty() && stack.isItemDamaged() && EnchantmentHelper.getEnchantmentLevel(enchantment, stack) > 0) {
                damaged.add(stack);
            }
        }
        return damaged.isEmpty() ? ItemStack.EMPTY : damaged.get(entity.getRNG().nextInt(damaged.size()));
    }

    // P7: portal search radius ----------------------------------------------------------------------------------

    /** 1.16's {@code findPortalAround}: 16 blocks when the portal leads into the nether, 128 elsewhere. */
    public static int portalRadius(World destination) {
        if (on() && ModConfig.changes.portalSearch && destination.provider.getDimensionType() == DimensionType.NETHER) {
            return 16;
        }
        return 128;
    }

    // P8: lava pushes -------------------------------------------------------------------------------------------

    /** {@code updateFluidHeightAndDoFluidPushing(LAVA, ...)}: 0.007 in an ultrawarm dimension, 0.0023 elsewhere. */
    public static double lavaPushScale(World world) {
        return world.provider.doesWaterVaporize() ? 0.007D : 0.0023333333333333335D;
    }

    /**
     * 1.16.5's {@code Entity.updateFluidHeightAndDoFluidPushing} for lava, run each tick right after 1.12.2's
     * water handling: the flow of every lava block the entity is in, averaged, normalized for all but players,
     * scaled and added to its motion.
     */
    public static void lavaPush(Entity entity) {
        if (!on() || !ModConfig.changes.lavaPushes || !entity.isPushedByWater()) {
            return;
        }
        AxisAlignedBB box = entity.getEntityBoundingBox().shrink(0.001D);
        int x0 = MathHelper.floor(box.minX);
        int x1 = MathHelper.ceil(box.maxX);
        int y0 = MathHelper.floor(box.minY);
        int y1 = MathHelper.ceil(box.maxY);
        int z0 = MathHelper.floor(box.minZ);
        int z1 = MathHelper.ceil(box.maxZ);
        World world = entity.world;
        if (!world.isAreaLoaded(new BlockPos(x0, y0, z0), new BlockPos(x1, y1, z1), true)) {
            return;
        }
        double depth = 0.0D;
        Vec3d flow = Vec3d.ZERO;
        int n = 0;
        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
        try {
            for (int x = x0; x < x1; x++) {
                for (int y = y0; y < y1; y++) {
                    for (int z = z0; z < z1; z++) {
                        pos.setPos(x, y, z);
                        IBlockState state = world.getBlockState(pos);
                        if (state.getMaterial() != Material.LAVA || !(state.getBlock() instanceof BlockLiquid)) {
                            continue;
                        }
                        double top = y + height(world, pos, state);
                        if (top < box.minY) {
                            continue;
                        }
                        depth = Math.max(top - box.minY, depth);
                        Vec3d here = state.getBlock().modifyAcceleration(world, pos.toImmutable(), entity, Vec3d.ZERO);
                        if (depth < 0.4D) {
                            here = here.scale(depth);
                        }
                        flow = flow.add(here);
                        n++;
                    }
                }
            }
        } finally {
            pos.release();
        }
        if (flow.length() <= 0.0D) {
            return;
        }
        if (n > 0) {
            flow = flow.scale(1.0D / n);
        }
        if (!(entity instanceof EntityPlayer)) {
            flow = flow.normalize();
        }
        flow = flow.scale(lavaPushScale(world));
        if (Math.abs(entity.motionX) < 0.003D && Math.abs(entity.motionZ) < 0.003D && flow.length() < 0.0045000000000000005D) {
            flow = flow.normalize().scale(0.0045000000000000005D);
        }
        entity.motionX += flow.x;
        entity.motionY += flow.y;
        entity.motionZ += flow.z;
    }

    /** 1.16's {@code FluidState.getHeight}: a full block under the same fluid, else its level of 9 (a source 8/9). */
    static float height(IBlockAccess world, BlockPos pos, IBlockState state) {
        if (world.getBlockState(pos.up()).getMaterial() == state.getMaterial()) {
            return 1.0F;
        }
        return 1.0F - BlockLiquid.getLiquidHeightPercent(state.getValue(BlockLiquid.LEVEL));
    }

    // P9: totem of undying --------------------------------------------------------------------------------------

    /** 1.16.2: the totem adds Fire Resistance for 800 ticks after Regeneration II and Absorption II. */
    public static void totemUsed(EntityLivingBase entity) {
        if (on() && ModConfig.changes.totemFireResistance) {
            entity.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 800, 0));
        }
    }

    // P10: riders stay ------------------------------------------------------------------------------------------

    /**
     * 1.16's {@code requiresCustomPersistence}: {@code isPassenger()}. True skips {@code despawnEntity}, as 1.16
     * skips {@code checkDespawn}, and keeps the idle timer at 0 there too.
     */
    public static boolean keepRider(EntityLiving mob) {
        if (!ModConfig.mobs.enabled || !ModConfig.mobs.ridersStay || !mob.isRiding()) {
            return false;
        }
        try {
            IDLE_TIME.setInt(mob, 0);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return true;
    }

    // P11: redstone dot and cross -------------------------------------------------------------------------------

    public static boolean redstoneActive() {
        return on() && ModConfig.changes.redstoneDot;
    }

    /** {@code BlockRedstoneWire.getWeakPower} as 1.16.5's {@code getSignal}. */
    public static int weakPower(BlockRedstoneWire wire, IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return RedstoneDot.weakPower(wire, state, world, pos, side);
    }

    /** {@code BlockRedstoneWire.getActualState}'s result: 1.16 draws unconnected dust as a cross. */
    public static IBlockState actualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return redstoneActive() ? RedstoneDot.crossIfLone(state) : state;
    }
}

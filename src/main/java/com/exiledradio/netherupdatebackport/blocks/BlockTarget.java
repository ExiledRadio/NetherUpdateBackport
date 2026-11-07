package com.exiledradio.netherupdatebackport.blocks;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Triggers;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * The target: a projectile that lands on it gives a redstone signal, 15 at the centre of the face it hit
 * and weaker towards the edges, for 20 ticks after an arrow (or trident) and 8 after anything else.
 *
 * <p>1.16's {@code TargetBlock}: the power is {@code ceil(15 * (0.5 - d) / 0.5)}, at least 1, where d is the
 * hit point's larger distance from the face's centre along the face's two axes; a target already counting
 * down ignores new hits. Projectile hits come from Forge's impact event (arrows, snowballs, eggs, ender
 * pearls, potions, fireballs), so vanilla's projectile code is untouched.
 *
 * <p>Hitting the centre (15) from 30 blocks or more grants <i>Bullseye</i>.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class BlockTarget extends Shapes.Plain {

    public static final PropertyInteger POWER = PropertyInteger.create("power", 0, 15);

    BlockTarget() {
        super(Material.GRASS, MapColor.QUARTZ, 0.5F, 0.5F, SoundType.PLANT);
        setDefaultState(this.blockState.getBaseState().withProperty(POWER, 0));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWER);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(POWER, meta & 15);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(POWER);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return state.getValue(POWER);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (state.getValue(POWER) != 0) {
            world.setBlockState(pos, state.withProperty(POWER, 0), 3);
        }
    }

    /** @return the power a hit at this point of this face gives (1.16's formula) */
    public static int strength(EnumFacing face, Vec3d hit) {
        // MathHelper.frac is client-only in 1.12.2.
        double dx = Math.abs(hit.x - Math.floor(hit.x) - 0.5);
        double dy = Math.abs(hit.y - Math.floor(hit.y) - 0.5);
        double dz = Math.abs(hit.z - Math.floor(hit.z) - 0.5);
        double d;
        if (face.getAxis() == EnumFacing.Axis.Y) {
            d = Math.max(dx, dz);
        } else if (face.getAxis() == EnumFacing.Axis.Z) {
            d = Math.max(dx, dy);
        } else {
            d = Math.max(dy, dz);
        }
        return Math.max(1, MathHelper.ceil(15.0 * MathHelper.clamp((0.5 - d) / 0.5, 0.0, 1.0)));
    }

    /**
     * Handles a projectile landing on a target.
     *
     * @return the power given, or 0 if the target was still counting down from an earlier hit
     */
    public static int hit(World world, BlockPos pos, EnumFacing face, Vec3d at, Entity projectile) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockTarget) || world.isUpdateScheduled(pos, state.getBlock())) {
            return 0;
        }
        int power = strength(face, at);
        world.setBlockState(pos, state.withProperty(POWER, power), 3);
        world.scheduleUpdate(pos, state.getBlock(), projectile instanceof EntityArrow ? 20 : 8);
        return power;
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent event) {
        RayTraceResult result = event.getRayTraceResult();
        Entity projectile = event.getEntity();
        if (result == null || result.typeOfHit != RayTraceResult.Type.BLOCK || projectile.world.isRemote
                || BlocksContent.target == null || projectile.world.getBlockState(result.getBlockPos()).getBlock() != BlocksContent.target) {
            return;
        }
        int power = hit(projectile.world, result.getBlockPos(), result.sideHit, result.hitVec, projectile);
        Entity shooter = shooter(projectile);
        if (power == 15 && shooter instanceof EntityPlayerMP && shooter.getDistance(projectile.posX, projectile.posY, projectile.posZ) >= 30.0) {
            Triggers.fire((EntityPlayerMP) shooter, "target_bullseye");
        }
    }

    private static Entity shooter(Entity projectile) {
        if (projectile instanceof EntityArrow) {
            return ((EntityArrow) projectile).shootingEntity;
        }
        if (projectile instanceof EntityThrowable) {
            return ((EntityThrowable) projectile).getThrower();
        }
        if (projectile instanceof EntityFireball) {
            return ((EntityFireball) projectile).shootingEntity;
        }
        return null;
    }
}

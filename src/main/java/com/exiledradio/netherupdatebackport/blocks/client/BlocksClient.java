package com.exiledradio.netherupdatebackport.blocks.client;

import com.exiledradio.netherupdatebackport.blocks.BlocksContent;
import com.exiledradio.netherupdatebackport.blocks.ItemLodestoneCompass;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleDrip;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/** Client side of stream U: crying obsidian's tears and the lodestone compass's needle. */
public final class BlocksClient {

    private BlocksClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new BlocksClient());
    }

    @SubscribeEvent
    public void models(ModelRegistryEvent event) {
        if (BlocksContent.lodestoneCompass != null) {
            BlocksContent.lodestoneCompass.addPropertyOverride(new ResourceLocation("angle"), new Needle());
        }
    }

    /** A purple tear dripping from crying obsidian (1.16's dripping obsidian tear, drawn as a drip). */
    public static void tear(World world, double x, double y, double z) {
        Minecraft.getMinecraft().effectRenderer.addEffect(new Tear(world, x, y, z));
    }

    private static final class Tear extends ParticleDrip {
        Tear(World world, double x, double y, double z) {
            super(world, x, y, z, Material.LAVA);
            color();
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            color();
        }

        private void color() {
            // 1.16's DripParticle colors for the obsidian tear.
            this.particleRed = 0.51171875F;
            this.particleGreen = 0.03125F;
            this.particleBlue = 0.890625F;
        }
    }

    /**
     * The needle: towards the bound lodestone in its dimension, spinning otherwise, with the vanilla compass's
     * swing in a player's hand.
     */
    private static final class Needle implements IItemPropertyGetter {
        private double rotation;
        private double rota;
        private long lastUpdateTick;

        @Override
        public float apply(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase holder) {
            if (holder == null && !stack.isOnItemFrame()) {
                return 0.0F;
            }
            boolean held = holder != null;
            Entity entity = held ? holder : stack.getItemFrame();
            if (world == null) {
                world = entity.world;
            }
            BlockPos target = ItemLodestoneCompass.target(stack);
            double angle;
            if (target != null && ItemLodestoneCompass.dimension(stack) == world.provider.getDimension()) {
                double yaw = held ? entity.rotationYaw : frameYaw((EntityItemFrame) entity);
                yaw = MathHelper.positiveModulo(yaw / 360.0, 1.0);
                double towards = Math.atan2(target.getZ() + 0.5 - entity.posZ, target.getX() + 0.5 - entity.posX) / (Math.PI * 2.0);
                angle = 0.5 - (yaw - 0.25 - towards);
            } else {
                angle = Math.random();
            }
            if (held) {
                angle = wobble(world, angle);
            }
            return MathHelper.positiveModulo((float) angle, 1.0F);
        }

        private double wobble(World world, double angle) {
            if (world.getTotalWorldTime() != this.lastUpdateTick) {
                this.lastUpdateTick = world.getTotalWorldTime();
                double d = angle - this.rotation;
                d = MathHelper.positiveModulo(d + 0.5, 1.0) - 0.5;
                this.rota += d * 0.1;
                this.rota *= 0.8;
                this.rotation = MathHelper.positiveModulo(this.rotation + this.rota, 1.0);
            }
            return this.rotation;
        }

        private static double frameYaw(EntityItemFrame frame) {
            return MathHelper.wrapDegrees(180 + frame.facingDirection.getHorizontalIndex() * 90);
        }
    }
}

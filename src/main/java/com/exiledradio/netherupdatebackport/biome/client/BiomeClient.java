package com.exiledradio.netherupdatebackport.biome.client;

import com.exiledradio.netherupdatebackport.biome.BiomeContent;
import com.exiledradio.netherupdatebackport.biome.NetherBiomes;
import com.exiledradio.netherupdatebackport.biome.WorldProviderNether;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * Client side of stream V: 1.16's biome fog colors, ambient particles (ash, white ash, crimson and warped spores)
 * and the biome names in the player's language.
 */
public final class BiomeClient {

    private final Random random = new Random();

    private BiomeClient() {
    }

    public static void init() {
        if (!BiomeContent.enabled()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new BiomeClient());
        WorldProviderNether.setFogSource(BiomeClient::fog);
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
                @Override
                public void onResourceManagerReload(IResourceManager manager) {
                    BiomeContent.names(key -> I18n.hasKey(key) ? I18n.format(key) : null);
                }
            });
        }
    }

    // ------------------------------------------------------------------------------------------
    // Fog: FogRenderer's CubicSampler.gaussianSampleVec3 over the biome cells around the camera.
    // ------------------------------------------------------------------------------------------

    private static final double[] KERNEL = {0.0D, 1.0D, 4.0D, 6.0D, 4.0D, 1.0D, 0.0D};

    static Vec3d fog(WorldProviderNether provider, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        World world = mc.world;
        if (view == null || world == null) {
            return null;
        }
        double cx = view.prevPosX + (view.posX - view.prevPosX) * partialTicks;
        double cy = view.prevPosY + (view.posY - view.prevPosY) * partialTicks + view.getEyeHeight();
        double cz = view.prevPosZ + (view.posZ - view.prevPosZ) * partialTicks;
        // (camera - 2) / 4: quart coordinates. 1.12.2 keeps one biome per column, so the y axis drops out.
        double qx = (cx - 2.0D) * 0.25D;
        double qz = (cz - 2.0D) * 0.25D;
        int i = (int) Math.floor(qx);
        int k = (int) Math.floor(qz);
        double fx = qx - i;
        double fz = qz - k;
        int y = (int) Math.floor(cy);
        double total = 0.0D;
        double r = 0.0D;
        double g = 0.0D;
        double b = 0.0D;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int l = 0; l < 6; ++l) {
            double wx = lerp(fx, KERNEL[l + 1], KERNEL[l]);
            int x = i - 2 + l;
            for (int n = 0; n < 6; ++n) {
                double wz = lerp(fz, KERNEL[n + 1], KERNEL[n]);
                int z = k - 2 + n;
                double w = wx * wz;
                pos.setPos((x << 2) + 2, y, (z << 2) + 2);
                int color = color(world, pos);
                total += w;
                r += w * ((color >> 16 & 255) / 255.0D);
                g += w * ((color >> 8 & 255) / 255.0D);
                b += w * ((color & 255) / 255.0D);
            }
        }
        return total <= 0.0D ? null : new Vec3d(r / total, g / total, b / total);
    }

    private static int color(World world, BlockPos pos) {
        NetherBiomes.Kind kind = world.isBlockLoaded(pos) ? NetherBiomes.at(world, pos) : null;
        return (kind == null ? NetherBiomes.Kind.NETHER_WASTES : kind).fogColor;
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    // ------------------------------------------------------------------------------------------
    // Ambient particles: ClientWorld.animateTick's biome part, 667 x (16 and 32 blocks) samples a tick.
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || mc.world == null || mc.player == null || mc.isGamePaused()
                || !(mc.world.provider instanceof WorldProviderNether)) {
            return;
        }
        WorldClient world = mc.world;
        int px = (int) Math.floor(mc.player.posX);
        int py = (int) Math.floor(mc.player.posY);
        int pz = (int) Math.floor(mc.player.posZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int j = 0; j < 667; ++j) {
            sample(mc, world, px, py, pz, 16, pos);
            sample(mc, world, px, py, pz, 32, pos);
        }
    }

    private void sample(Minecraft mc, WorldClient world, int x, int y, int z, int range, BlockPos.MutableBlockPos pos) {
        Random r = this.random;
        pos.setPos(x + r.nextInt(range) - r.nextInt(range), y + r.nextInt(range) - r.nextInt(range), z + r.nextInt(range) - r.nextInt(range));
        if (pos.getY() < 0 || pos.getY() > 255 || !world.isBlockLoaded(pos)) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.isFullCube()) {
            return;
        }
        NetherBiomes.Kind kind = NetherBiomes.at(world, pos);
        if (kind == null || kind.particle == null || r.nextFloat() > kind.particleChance) {
            return;
        }
        double px = pos.getX() + r.nextDouble();
        double py = pos.getY() + r.nextDouble();
        double pz = pos.getZ() + r.nextDouble();
        Particle p;
        switch (kind.particle) {
            case ASH:
                p = new Rising(world, px, py, pz, 0.0D, 0.0D, 0.0D, 0.5F, -0.004D);
                break;
            case WHITE_ASH: {
                Random wr = world.rand;
                double vx = wr.nextFloat() * -1.9D * wr.nextFloat() * 0.1D;
                double vy = wr.nextFloat() * -0.5D * wr.nextFloat() * 0.1D * 5.0D;
                double vz = wr.nextFloat() * -1.9D * wr.nextFloat() * 0.1D;
                p = new Rising(world, px, py, pz, vx, vy, vz, 0.0F, -5.0E-4D);
                p.setRBGColorF(0.7294118F, 0.69411767F, 0.7607843F);
                break;
            }
            case CRIMSON_SPORE: {
                Random wr = world.rand;
                p = new Spore(world, px, py, pz, wr.nextGaussian() * 1.0E-6F, wr.nextGaussian() * 1.0E-4F, wr.nextGaussian() * 1.0E-6F, false);
                p.setRBGColorF(0.9F, 0.4F, 0.5F);
                break;
            }
            case WARPED_SPORE: {
                double vy = world.rand.nextFloat() * -1.9D * world.rand.nextFloat() * 0.1D;
                p = new Spore(world, px, py, pz, 0.0D, vy, 0.0D, true);
                p.setRBGColorF(0.1F, 0.1F, 0.3F);
                break;
            }
            default:
                return;
        }
        mc.effectRenderer.addEffect(p);
    }

    /** 1.16's {@code RisingParticle} as {@code AshParticle} and {@code WhiteAshParticle} configure it; sprite generic_0. */
    private static final class Rising extends Particle {
        private final double fallSpeed;

        Rising(World world, double x, double y, double z, double vx, double vy, double vz, float colorMax, double fallSpeed) {
            super(world, x, y, z, 0.0D, 0.0D, 0.0D);
            this.fallSpeed = fallSpeed;
            this.motionX = this.motionX * 0.1F + vx;
            this.motionY = this.motionY * -0.1F + vy;
            this.motionZ = this.motionZ * 0.1F + vz;
            float f = world.rand.nextFloat() * colorMax;
            this.particleRed = f;
            this.particleGreen = f;
            this.particleBlue = f;
            this.particleScale *= 0.75F;
            this.particleMaxAge = Math.max(1, (int) (20.0D / (world.rand.nextFloat() * 0.8D + 0.2D)));
            this.canCollide = false;
            setParticleTextureIndex(0);
        }

        @Override
        public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rx, float rz, float ryz, float rxy, float rxz) {
            float scale = this.particleScale;
            this.particleScale = scale * Math.max(0.0F, Math.min(1.0F, (this.particleAge + partialTicks) / this.particleMaxAge * 32.0F));
            super.renderParticle(buffer, entity, partialTicks, rx, rz, ryz, rxy, rxz);
            this.particleScale = scale;
        }

        @Override
        public void onUpdate() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (this.particleAge++ >= this.particleMaxAge) {
                setExpired();
                return;
            }
            this.motionY += this.fallSpeed;
            move(this.motionX, this.motionY, this.motionZ);
            if (this.posY == this.prevPosY) {
                this.motionX *= 1.1D;
                this.motionZ *= 1.1D;
            }
            this.motionX *= 0.96F;
            this.motionY *= 0.96F;
            this.motionZ *= 0.96F;
            if (this.onGround) {
                this.motionX *= 0.7F;
                this.motionZ *= 0.7F;
            }
        }
    }

    /** 1.16's {@code UnderwaterParticle} as the spore factories make it: drifts, no gravity, no collision. */
    private static final class Spore extends Particle {
        Spore(World world, double x, double y, double z, double vx, double vy, double vz, boolean tiny) {
            super(world, x, y - 0.125D, z, vx, vy, vz);
            setSize(0.01F, 0.01F);
            this.particleScale *= this.rand.nextFloat() * 0.6F + 0.6F;
            this.particleMaxAge = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
            this.canCollide = false;
            if (tiny) {
                setSize(0.001F, 0.001F);
            }
            setParticleTextureIndex(0);
        }

        @Override
        public void onUpdate() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            if (this.particleMaxAge-- <= 0) {
                setExpired();
            } else {
                move(this.motionX, this.motionY, this.motionZ);
            }
        }
    }
}

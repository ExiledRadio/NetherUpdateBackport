package com.exiledradio.netherupdatebackport.biome;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldProviderHell;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Dimension -1 with 1.16's biomes: vanilla's {@link WorldProviderHell} in everything (terrain generator, light,
 * dimension type {@code NETHER}, save folder) but the biome source, which is {@link NetherBiomeSource} seeded from
 * the world. Registered in place of vanilla's provider through {@code DimensionManager} at post-init, unless
 * another mod got there first ({@link BiomeContent#postInit}).
 */
public class WorldProviderNether extends WorldProviderHell {

    /** Base fog colors for the client: set by the client class, read here, so this class names no client type. */
    public interface FogSource {
        /** @return the fog color for this frame, or null for vanilla's */
        Vec3d fog(WorldProviderNether provider, float partialTicks);
    }

    static volatile FogSource fogSource;

    public static void setFogSource(FogSource source) {
        fogSource = source;
    }

    @Override
    public void init() {
        super.init();
        this.biomeProvider = new NetherBiomeSource(this.world.getSeed(), BiomeContent.weights());
    }

    /** 1.16: the fog takes the biome's fog color, blended over the biomes around the camera. */
    @Override
    @SideOnly(Side.CLIENT)
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        FogSource source = fogSource;
        Vec3d fog = source == null ? null : source.fog(this, partialTicks);
        return fog != null ? fog : new Vec3d(0.20000000298023224D, 0.029999999329447746D, 0.029999999329447746D);
    }
}

package com.exiledradio.netherupdatebackport.biome;

import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeHell;

import java.util.Random;

/**
 * One of 1.16's four new nether biomes. A {@link BiomeHell}, so code that asks "is this a nether biome" by class
 * still says yes; tagged NETHER, HOT and DRY in the Biome Dictionary.
 *
 * <p>Vanilla mobs' spawn lists are 1.16.5's {@code BiomeMaker} entries; the 1.16 mobs of this mod (hoglins,
 * piglins, striders) are added by name at post-init ({@link BiomeContent#postInit}), since entities register after
 * biomes. Decoration is {@link NetherDecorator}'s.
 */
public class NetherBiome extends BiomeHell {

    final NetherBiomes.Kind kind;

    NetherBiome(NetherBiomes.Kind kind, String name) {
        super(new BiomeProperties(name).setTemperature(2.0F).setRainfall(0.0F).setRainDisabled());
        this.kind = kind;
        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
        switch (kind) {
            case SOUL_SAND_VALLEY:
                this.spawnableMonsterList.add(new SpawnListEntry(EntitySkeleton.class, 20, 5, 5));
                this.spawnableMonsterList.add(new SpawnListEntry(EntityGhast.class, 50, 4, 4));
                this.spawnableMonsterList.add(new SpawnListEntry(EntityEnderman.class, 1, 4, 4));
                break;
            case BASALT_DELTAS:
                this.spawnableMonsterList.add(new SpawnListEntry(EntityGhast.class, 40, 1, 1));
                this.spawnableMonsterList.add(new SpawnListEntry(EntityMagmaCube.class, 100, 2, 5));
                break;
            case CRIMSON_FOREST:
                this.spawnableMonsterList.add(new SpawnListEntry(EntityPigZombie.class, 1, 2, 4));
                break;
            case WARPED_FOREST:
                this.spawnableMonsterList.add(new SpawnListEntry(EntityEnderman.class, 1, 4, 4));
                break;
            default:
                break;
        }
    }

    public NetherBiomes.Kind kind() {
        return this.kind;
    }

    /** Called by the nether's chunk generator for the chunk being populated (its biome at the chunk's +16 corner). */
    @Override
    public void decorate(World world, Random rand, BlockPos pos) {
        NetherDecorator.decorate(this.kind, world, rand, pos.getX() >> 4, pos.getZ() >> 4);
    }
}

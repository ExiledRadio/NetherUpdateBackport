package com.exiledradio.netherupdatebackport.biome;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;

/**
 * Stream V: the crimson forest, warped forest, soul sand valley and basalt deltas (1.16's Proposal A).
 *
 * <ul>
 *   <li>Registers the four biomes (nether wastes is vanilla's {@code minecraft:hell}, renamed).</li>
 *   <li>At post-init, replaces dimension -1's provider with {@link WorldProviderNether} (1.16's biome source, vanilla's
 *       terrain), unless another mod has already replaced it ({@code biomes.overrideOtherMods} makes ours win).</li>
 *   <li>Surfaces through {@link NetherSurfaces}, features through {@link NetherDecorator}, and here the pieces of
 *       1.12.2's nether decoration that 1.16 leaves out of a biome: nether mushrooms (not in the forests or soul
 *       sand valleys) and open lava springs (not in basalt deltas, which get delta springs instead).</li>
 *   <li>1.16.5's spawn lists, with this mod's own mobs (hoglins, piglins, striders) looked up by name.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class BiomeContent {

    private static boolean providerReplaced;

    private BiomeContent() {
    }

    public static boolean enabled() {
        return ModConfig.biomes.enabled;
    }

    /** @return true once dimension -1 uses this mod's provider (false when another mod's nether was kept) */
    public static boolean providerReplaced() {
        return providerReplaced;
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        if (!enabled()) {
            return;
        }
        for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
            if (kind == NetherBiomes.Kind.NETHER_WASTES) {
                continue;
            }
            // The English name until post-init puts the language file's in (1.12.2 biome names are plain text).
            NetherBiome biome = new NetherBiome(kind, kind.id);
            biome.setRegistryName(Content.id(kind.id));
            event.getRegistry().register(biome);
            BiomeDictionary.addTypes(biome, BiomeDictionary.Type.NETHER, BiomeDictionary.Type.HOT, BiomeDictionary.Type.DRY);
            NetherBiomes.put(kind, biome);
        }
    }

    /** Called from post-init, when every entity is registered and other mods have set up their dimensions. */
    public static void postInit() {
        if (!enabled()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(NetherSurfaces.class);
        MinecraftForge.TERRAIN_GEN_BUS.register(new TerrainEvents());
        MinecraftForge.EVENT_BUS.register(new WastesEvents());
        names(new java.util.function.Function<String, String>() {
            @Override
            @SuppressWarnings("deprecation")
            public String apply(String key) {
                return net.minecraft.util.text.translation.I18n.canTranslate(key)
                        ? net.minecraft.util.text.translation.I18n.translateToLocal(key) : null;
            }
        });
        spawns();
        replaceProvider();
    }

    // ------------------------------------------------------------------------------------------
    // Names
    // ------------------------------------------------------------------------------------------

    private static Field nameField;

    /** The lang key of a nether biome's name: {@code biome.netherupdatebackport.<id>}, nether wastes included. */
    public static String nameKey(NetherBiomes.Kind kind) {
        return "biome." + NetherUpdateBackport.MOD_ID + "." + kind.id;
    }

    /** Sets the five biomes' names (vanilla's hell's too) from a translator; the client calls it again on reload. */
    public static void names(java.util.function.Function<String, String> translate) {
        if (!enabled()) {
            return;
        }
        try {
            if (nameField == null) {
                nameField = ReflectionHelper.findField(Biome.class, "biomeName", "field_76791_y");
            }
            for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
                Biome biome = NetherBiomes.biome(kind);
                String name = translate.apply(nameKey(kind));
                if (biome != null && name != null) {
                    nameField.set(biome, name);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            NetherUpdateBackport.LOGGER.warn("Could not name the nether biomes", e);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Spawns: 1.16.5's BiomeMaker
    // ------------------------------------------------------------------------------------------

    private static void spawns() {
        Biome crimson = NetherBiomes.biome(NetherBiomes.Kind.CRIMSON_FOREST);
        add(crimson, "hoglin", EnumCreatureType.MONSTER, 9, 3, 4);
        add(crimson, "piglin", EnumCreatureType.MONSTER, 5, 3, 4);
        add(Biomes.HELL, "piglin", EnumCreatureType.MONSTER, 15, 4, 4);
        // Striders (stream R) already spawn in nether wastes; 1.16 has them in all five.
        if (ModConfig.striders.spawn) {
            for (NetherBiomes.Kind kind : NetherBiomes.Kind.values()) {
                if (kind != NetherBiomes.Kind.NETHER_WASTES) {
                    add(NetherBiomes.biome(kind), "strider", EnumCreatureType.CREATURE, 60, 1, 2);
                }
            }
        }
    }

    /** Adds a spawn of this mod's entity {@code name}, if it is registered (its stream may be off). */
    @SuppressWarnings("unchecked")
    private static void add(@Nullable Biome biome, String name, EnumCreatureType type, int weight, int min, int max) {
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(NetherUpdateBackport.MOD_ID, name));
        if (biome == null || entry == null || !EntityLiving.class.isAssignableFrom(entry.getEntityClass())) {
            return;
        }
        Class<? extends EntityLiving> cls = (Class<? extends EntityLiving>) entry.getEntityClass();
        for (Biome.SpawnListEntry e : biome.getSpawnableList(type)) {
            if (e.entityClass == cls) {
                return;
            }
        }
        biome.getSpawnableList(type).add(new Biome.SpawnListEntry(cls, weight, min, max));
    }

    /** @return the class of this mod's entity, or null */
    @Nullable
    static Class<? extends Entity> entity(String name) {
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(NetherUpdateBackport.MOD_ID, name));
        return entry == null ? null : entry.getEntityClass();
    }

    // ------------------------------------------------------------------------------------------
    // The provider
    // ------------------------------------------------------------------------------------------

    /**
     * @return true if dimension -1 is still vanilla's: its type is {@code NETHER}, or makes exactly a
     * {@link WorldProviderHell} (and not this mod's provider already)
     */
    public static boolean netherIsVanilla(DimensionType type) {
        if (type == DimensionType.NETHER) {
            return true;
        }
        try {
            WorldProvider p = type.createDimension();
            return p.getClass() == WorldProviderHell.class;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void replaceProvider() {
        if (!DimensionManager.isDimensionRegistered(-1)) {
            NetherUpdateBackport.LOGGER.info("No nether dimension is registered; the 1.16 nether biomes are not used.");
            return;
        }
        DimensionType current = DimensionManager.getProviderType(-1);
        if (!netherIsVanilla(current) && !ModConfig.biomes.overrideOtherMods) {
            NetherUpdateBackport.LOGGER.info("Another mod has replaced the nether ({}); the 1.16 nether biomes step aside "
                    + "(biomes.overrideOtherMods makes them win).", current);
            return;
        }
        DimensionType ours = DimensionType.register("the_nether", "_nether", -1, WorldProviderNether.class, false);
        DimensionManager.unregisterDimension(-1);
        DimensionManager.registerDimension(-1, ours);
        providerReplaced = true;
        NetherUpdateBackport.LOGGER.info("The nether uses 1.16's biomes.");
    }

    /** {@code biomes.weights} as kind -> weight; missing or unreadable entries are 1. */
    public static Map<NetherBiomes.Kind, Float> weights() {
        Map<NetherBiomes.Kind, Float> out = new EnumMap<NetherBiomes.Kind, Float>(NetherBiomes.Kind.class);
        for (String line : ModConfig.biomes.weights) {
            int eq = line.indexOf('=');
            if (eq < 0) {
                continue;
            }
            NetherBiomes.Kind kind = NetherBiomes.Kind.byId(line.substring(0, eq).trim());
            try {
                float w = Float.parseFloat(line.substring(eq + 1).trim());
                if (kind != null && w >= 0.0F && !Float.isNaN(w) && !Float.isInfinite(w)) {
                    out.put(kind, w);
                }
            } catch (NumberFormatException ignored) {
                NetherUpdateBackport.LOGGER.warn("biomes.weights: '{}' is not 'biome=number'", line);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------
    // 1.12.2's nether decoration, per biome
    // ------------------------------------------------------------------------------------------

    /** @return the biome a nether chunk is decorated as: the one at its +16 corner, as {@code ChunkGeneratorHell}. */
    static NetherBiomes.Kind decorationKind(net.minecraft.world.World world, int chunkX, int chunkZ) {
        NetherBiomes.Kind kind = NetherBiomes.of(world.getBiome(new BlockPos((chunkX << 4) + 16, 0, (chunkZ << 4) + 16)));
        return kind == null ? NetherBiomes.Kind.NETHER_WASTES : kind;
    }

    /** Turns off what 1.16 leaves out. Registered on the terrain-generation bus, where these events are posted. */
    public static final class TerrainEvents {
        @SubscribeEvent
        public void populate(PopulateChunkEvent.Populate event) {
            if (event.getType() == PopulateChunkEvent.Populate.EventType.NETHER_LAVA && NetherBiomes.active(event.getWorld())
                    && decorationKind(event.getWorld(), event.getChunkX(), event.getChunkZ()) == NetherBiomes.Kind.BASALT_DELTAS) {
                event.setResult(Event.Result.DENY);
            }
        }

        @SubscribeEvent
        public void decorate(DecorateBiomeEvent.Decorate event) {
            if (event.getType() != DecorateBiomeEvent.Decorate.EventType.SHROOM || !NetherBiomes.active(event.getWorld())) {
                return;
            }
            NetherBiomes.Kind kind = decorationKind(event.getWorld(), event.getChunkPos().x, event.getChunkPos().z);
            if (kind == NetherBiomes.Kind.CRIMSON_FOREST || kind == NetherBiomes.Kind.WARPED_FOREST || kind == NetherBiomes.Kind.SOUL_SAND_VALLEY) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    /** Nether wastes is vanilla's hell, whose own decorator does nothing: its 1.16 additions come in here. */
    public static final class WastesEvents {
        @SubscribeEvent
        public void decorated(DecorateBiomeEvent.Post event) {
            if (!NetherBiomes.active(event.getWorld())) {
                return;
            }
            int cx = event.getChunkPos().x;
            int cz = event.getChunkPos().z;
            if (decorationKind(event.getWorld(), cx, cz) == NetherBiomes.Kind.NETHER_WASTES
                    && !(event.getWorld().getBiome(new BlockPos((cx << 4) + 16, 0, (cz << 4) + 16)) instanceof NetherBiome)) {
                NetherDecorator.decorate(NetherBiomes.Kind.NETHER_WASTES, event.getWorld(), event.getRand(), cx, cz);
            }
        }
    }
}

package com.exiledradio.netherupdatebackport.soul;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Sounds;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stream L: soul soil, soul fire, the soul torch, the soul lantern, the soul campfire ({@code campfire}
 * subpackage) and Soul Speed.
 *
 * <p>Numbers from 1.16.5: soul soil 0.5 (shovel, no slowdown, unlike soul sand), soul lantern 3.5, light 10
 * for every soul light source.
 *
 * <h2>Fire turning to soul fire</h2>
 * In 1.16 any fire placed on soul sand or soul soil is soul fire. 1.12.2's flint and steel, fire charges,
 * lava and spreading fire all place vanilla's fire; it is changed into soul fire at the end of the tick in
 * which it appeared over a soul block (found through Forge's neighbor-notify event, as stream S's basalt
 * generator does), so no vanilla fire code is touched.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class SoulContent {

    public static final String[] NAMES = {"soul_soil", "soul_fire", "soul_torch", "soul_lantern", "soul_campfire"};

    public static Block soulSoil;
    public static BlockSoulFire soulFire;
    public static Block soulTorch;
    public static Block soulLantern;
    public static Enchantment soulSpeed;

    private static final Map<World, Set<BlockPos>> PENDING = new HashMap<World, Set<BlockPos>>();

    private SoulContent() {
    }

    public static boolean enabled() {
        return ModConfig.soul.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BlockOwnership.declare(NetherUpdateBackport.MOD_ID, NAMES);
        if (!enabled()) {
            return;
        }
        soulSoil = Content.block(event.getRegistry(), "soul_soil",
                new Shapes.Plain(Material.GROUND, MapColor.BROWN, 0.5F, 0.5F, Sounds.type("soul_soil")), "shovel");
        soulFire = Content.block(event.getRegistry(), "soul_fire", new BlockSoulFire(), null);
        soulFire.setCreativeTab(null);
        Content.multipart(soulFire);
        soulTorch = Content.block(event.getRegistry(), "soul_torch", new BlockSoulTorch(), null);
        soulLantern = Content.block(event.getRegistry(), "soul_lantern", new BlockSoulLantern(Sounds.type("lantern")), "pickaxe");
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        Content.registerBlockItem(event.getRegistry(), soulSoil);
        Content.registerBlockItem(event.getRegistry(), soulTorch);
        Content.registerBlockItem(event.getRegistry(), soulLantern);
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        if (!enabled()) {
            return;
        }
        soulSpeed = new SoulSpeed();
        soulSpeed.setRegistryName(Content.id("soul_speed"));
        soulSpeed.setName(NetherUpdateBackport.MOD_ID + ".soul_speed");
        event.getRegistry().register(soulSpeed);
    }

    // ---------------------------------------------------------------------------------------------
    // Fire on soul blocks
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void neighborNotify(BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        if (world.isRemote || soulFire == null || !ModConfig.soul.soulFire || event.getState().getBlock() != Blocks.FIRE) {
            return;
        }
        Set<BlockPos> set = PENDING.get(world);
        if (set == null) {
            set = new LinkedHashSet<BlockPos>();
            PENDING.put(world, set);
        }
        set.add(event.getPos().toImmutable());
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        Set<BlockPos> set = PENDING.remove(event.world);
        if (set == null) {
            return;
        }
        for (BlockPos pos : set) {
            convert(event.world, pos);
        }
    }

    /** Turns vanilla fire standing on a soul block into soul fire. @return true if it did */
    public static boolean convert(World world, BlockPos pos) {
        if (world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() == Blocks.FIRE
                && BlockSoulFire.isBase(world.getBlockState(pos.down()).getBlock())) {
            world.setBlockState(pos, soulFire.getDefaultState(), 3);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        PENDING.remove(event.getWorld());
    }

    /** Punching soul fire puts it out, as vanilla's {@code extinguishFire} does only for its own fire. */
    @SubscribeEvent
    public static void punch(PlayerInteractEvent.LeftClickBlock event) {
        if (soulFire == null || event.getFace() == null) {
            return;
        }
        BlockPos fire = event.getPos().offset(event.getFace());
        if (event.getWorld().getBlockState(fire).getBlock() == soulFire) {
            if (!event.getWorld().isRemote) {
                BlockSoulFire.extinguish(event.getWorld(), fire);
            }
            event.setCanceled(true);
        }
    }
}

package com.exiledradio.netherupdatebackport.fungus;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Compat;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Sounds;
import com.exiledradio.netherupdatebackport.common.shape.BlockNetherSlab;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stream F: crimson and warped nylium, fungi, roots, sprouts, weeping and twisting vines, the warped wart
 * block, shroomlight, and both nether wood sets.
 *
 * <p>Numbers from 1.16.5's {@code Blocks}: stems and hyphae 2, planks, slabs, stairs, fences and gates 2/3,
 * doors and trapdoors 3, signs 1, plates and buttons 0.5, nylium 0.4, wart blocks and shroomlight 1. The woods
 * use {@code Material.WOOD} so axes, fences and every wood check treat them as wood, but they are never given
 * fire spread, never burn, and are not furnace fuel (1.16's {@code NETHER_WOOD}).
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class FungusContent {

    /** Every block of this stream, by name, in registration order. */
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<String, Block>();
    private static final List<Block> WITH_ITEMS = new ArrayList<Block>();
    private static final Map<Block, Block> STRIPPED = new LinkedHashMap<Block, Block>();
    private static final Map<NetherWood, NetherSigns.Standing> STANDING = new EnumMap<NetherWood, NetherSigns.Standing>(NetherWood.class);
    private static final Map<NetherWood, NetherSigns.Wall> WALL = new EnumMap<NetherWood, NetherSigns.Wall>(NetherWood.class);
    private static final Map<NetherWood, NetherSigns.SignItem> SIGN_ITEMS = new EnumMap<NetherWood, NetherSigns.SignItem>(NetherWood.class);
    private static final Map<NetherWood, Shapes.Door> DOORS = new EnumMap<NetherWood, Shapes.Door>(NetherWood.class);
    /** Items that are not furnace fuel although 1.12.2 would burn their wood material. */
    private static final Set<Item> NOT_FUEL = new HashSet<Item>();

    public static BlockNetherVines.Head weepingVines;
    public static BlockNetherVines.Head twistingVines;
    public static Block warpedWartBlock;
    public static Block shroomlight;

    static final SoundEvent STRIP = Sounds.event("item.axe.strip");

    private FungusContent() {
    }

    public static boolean enabled() {
        return ModConfig.fungus.enabled;
    }

    /** @return this stream's block by name, or null */
    public static Block block(String name) {
        return BLOCKS.get(name);
    }

    public static Block nylium(NetherWood wood) {
        return BLOCKS.get(wood.name + "_nylium");
    }

    public static Block stem(NetherWood wood) {
        return BLOCKS.get(wood.name + "_stem");
    }

    static NetherSigns.Standing standingSign(NetherWood wood) {
        return STANDING.get(wood);
    }

    static NetherSigns.Wall wallSign(NetherWood wood) {
        return WALL.get(wood);
    }

    static Item signItem(NetherWood wood) {
        Item item = SIGN_ITEMS.get(wood);
        return item != null ? item : net.minecraft.init.Items.AIR;
    }

    /** @return every block name this stream registers */
    public static List<String> names() {
        List<String> names = new ArrayList<String>();
        for (NetherWood w : NetherWood.values()) {
            String n = w.name;
            for (String s : new String[] {"_nylium", "_fungus", "_roots", "_stem", "_hyphae", "_planks", "_slab", "_double_slab",
                    "_stairs", "_fence", "_fence_gate", "_door", "_trapdoor", "_button", "_pressure_plate", "_sign", "_wall_sign"}) {
                names.add(n + s);
            }
            names.add("stripped_" + n + "_stem");
            names.add("stripped_" + n + "_hyphae");
        }
        for (String s : new String[] {"nether_sprouts", "weeping_vines", "weeping_vines_plant", "twisting_vines",
                "twisting_vines_plant", "warped_wart_block", "shroomlight"}) {
            names.add(s);
        }
        return names;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BlockOwnership.declare(NetherUpdateBackport.MOD_ID, names().toArray(new String[0]));
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        SoundType nylium = Sounds.type("nylium");
        SoundType fungus = Sounds.type("fungus");
        SoundType roots = Sounds.type("roots");
        SoundType stem = Sounds.type("stem");
        SoundType weeping = Sounds.type("weeping_vines");

        for (NetherWood w : NetherWood.values()) {
            String n = w.name;
            add(r, n + "_nylium", new BlockNylium(w, nylium), "pickaxe", true);
            add(r, n + "_fungus", new BlockNetherPlant.Fungus(w, w.plant, fungus), null, true);
            add(r, n + "_roots", new BlockNetherPlant.Roots(w.plant, roots), null, true);

            Block stemBlock = add(r, n + "_stem", new Shapes.Pillar(Material.WOOD, w.stem, 2.0F, 2.0F, stem), "axe", true);
            Block strippedStem = add(r, "stripped_" + n + "_stem", new Shapes.Pillar(Material.WOOD, w.stem, 2.0F, 2.0F, stem), "axe", true);
            Block hyphae = add(r, n + "_hyphae", new Shapes.Pillar(Material.WOOD, w.hyphae, 2.0F, 2.0F, stem), "axe", true);
            Block strippedHyphae = add(r, "stripped_" + n + "_hyphae", new Shapes.Pillar(Material.WOOD, w.hyphae, 2.0F, 2.0F, stem), "axe", true);
            STRIPPED.put(stemBlock, strippedStem);
            STRIPPED.put(hyphae, strippedHyphae);

            Block planks = add(r, n + "_planks", new Shapes.Plain(Material.WOOD, w.stem, 2.0F, 3.0F, SoundType.WOOD), "axe", true);
            BlockNetherSlab.Half half = new BlockNetherSlab.Half(planks, planks.getDefaultState(), 2.0F, 3.0F);
            BlockNetherSlab.Double full = new BlockNetherSlab.Double(planks, planks.getDefaultState(), 2.0F, 3.0F, half);
            Content.slab(r, n + "_slab", half, full, "axe");
            BLOCKS.put(n + "_slab", half);
            BLOCKS.put(n + "_double_slab", full);
            WITH_ITEMS.add(half);
            add(r, n + "_stairs", new Shapes.Stairs(planks.getDefaultState()), "axe", true);
            Block fence = add(r, n + "_fence", new Shapes.Fence(Material.WOOD, w.stem, SoundType.WOOD, 2.0F, 3.0F), "axe", true);
            Content.multipart(fence);
            Block gate = add(r, n + "_fence_gate", new Shapes.FenceGate(w.stem, SoundType.WOOD, 2.0F, 3.0F), "axe", true);
            Content.ignore(gate, "powered");
            Shapes.Door door = add(r, n + "_door", new Shapes.Door(Material.WOOD, SoundType.WOOD, 3.0F), "axe", false);
            Content.ignore(door, "powered");
            DOORS.put(w, door);
            add(r, n + "_trapdoor", new Shapes.Trapdoor(Material.WOOD, SoundType.WOOD, 3.0F), "axe", true);
            add(r, n + "_button", new Shapes.WoodButton(), null, true);
            add(r, n + "_pressure_plate", new Shapes.PressurePlate(Material.WOOD, w.stem, BlockPressurePlate.Sensitivity.EVERYTHING,
                    SoundType.WOOD), "axe", true);

            NetherSigns.Standing standing = add(r, n + "_sign", new NetherSigns.Standing(w), null, false);
            NetherSigns.Wall wall = add(r, n + "_wall_sign", new NetherSigns.Wall(w), null, false);
            standing.setCreativeTab(null);
            wall.setCreativeTab(null);
            Content.ignore(standing, "rotation");
            Content.ignore(wall, "facing");
            STANDING.put(w, standing);
            WALL.put(w, wall);
        }

        add(r, "nether_sprouts", new BlockNetherPlant.Sprouts(MapColor.CYAN, Sounds.type("nether_sprouts")), null, true);
        weepingVines = add(r, "weeping_vines", new BlockNetherVines.Head(MapColor.NETHERRACK, weeping, EnumFacing.DOWN,
                new AxisAlignedBB(4 / 16.0, 9 / 16.0, 4 / 16.0, 12 / 16.0, 1, 12 / 16.0)), null, true);
        add(r, "weeping_vines_plant", new BlockNetherVines.Body(weepingVines, MapColor.NETHERRACK, weeping), null, false);
        twistingVines = add(r, "twisting_vines", new BlockNetherVines.Head(MapColor.CYAN, weeping, EnumFacing.UP,
                new AxisAlignedBB(4 / 16.0, 0, 4 / 16.0, 12 / 16.0, 15 / 16.0, 12 / 16.0)), null, true);
        add(r, "twisting_vines_plant", new BlockNetherVines.Body(twistingVines, MapColor.CYAN, weeping), null, false);
        Content.ignore(weepingVines, "age");
        Content.ignore(twistingVines, "age");
        warpedWartBlock = add(r, "warped_wart_block", new Shapes.Plain(Material.GRASS, MapColor.CYAN, 1.0F, 1.0F,
                Sounds.type("wart_block")), "hoe", true);
        shroomlight = add(r, "shroomlight", new Shapes.Plain(Material.GRASS, MapColor.RED, 1.0F, 1.0F,
                Sounds.type("shroomlight")), "hoe", true);
        shroomlight.setLightLevel(1.0F);

        GameRegistry.registerTileEntity(NetherSigns.Tile.class, Content.id("nether_sign"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        for (Block block : WITH_ITEMS) {
            NOT_FUEL.add(Content.registerBlockItem(r, block));
        }
        for (NetherWood w : NetherWood.values()) {
            ItemDoor door = Content.item(r, w.name + "_door", new ItemDoor(DOORS.get(w)));
            DOORS.get(w).setItem(door);
            NetherSigns.SignItem sign = Content.item(r, w.name + "_sign", new NetherSigns.SignItem(w));
            SIGN_ITEMS.put(w, sign);
            NOT_FUEL.add(door);
            NOT_FUEL.add(sign);
        }
        registerOres();
        compost();
    }

    private static void registerOres() {
        for (NetherWood w : NetherWood.values()) {
            OreDictionary.registerOre("plankWood", BLOCKS.get(w.name + "_planks"));
            OreDictionary.registerOre("slabWood", BLOCKS.get(w.name + "_slab"));
            OreDictionary.registerOre("stairWood", BLOCKS.get(w.name + "_stairs"));
            OreDictionary.registerOre("fenceWood", BLOCKS.get(w.name + "_fence"));
            OreDictionary.registerOre("fenceGateWood", BLOCKS.get(w.name + "_fence_gate"));
        }
    }

    /** 1.16's composter chances, for the composter's owner (Rule 2). */
    private static void compost() {
        for (NetherWood w : NetherWood.values()) {
            Compat.compost(0.65F, Item.getItemFromBlock(BLOCKS.get(w.name + "_fungus")));
            Compat.compost(0.65F, Item.getItemFromBlock(BLOCKS.get(w.name + "_roots")));
        }
        Compat.compost(0.5F, Item.getItemFromBlock(BLOCKS.get("nether_sprouts")));
        Compat.compost(0.5F, Item.getItemFromBlock(weepingVines));
        Compat.compost(0.5F, Item.getItemFromBlock(twistingVines));
        Compat.compost(0.85F, Item.getItemFromBlock(warpedWartBlock));
        Compat.compost(0.85F, Item.getItemFromBlock(Blocks.NETHER_WART_BLOCK));
        Compat.compost(0.65F, Item.getItemFromBlock(shroomlight));
    }

    private static <B extends Block> B add(IForgeRegistry<Block> r, String name, B block, String tool, boolean item) {
        Content.block(r, name, block, tool);
        BLOCKS.put(name, block);
        if (item) {
            WITH_ITEMS.add(block);
        }
        return block;
    }

    // ---------------------------------------------------------------------------------------------
    // Behavior
    // ---------------------------------------------------------------------------------------------

    /** An axe strips a stem or hyphae, keeping its axis (1.16, as 1.13's logs). */
    @SubscribeEvent
    public static void strip(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        Block stripped = STRIPPED.get(state.getBlock());
        ItemStack held = event.getItemStack();
        if (stripped == null || !held.getItem().getToolClasses(held).contains("axe")) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        world.playSound(player, event.getPos(), STRIP, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!world.isRemote) {
            world.setBlockState(event.getPos(), stripped.getDefaultState()
                    .withProperty(BlockRotatedPillar.AXIS, state.getValue(BlockRotatedPillar.AXIS)), 11);
            held.damageItem(1, player);
        }
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS);
    }

    /**
     * Bone meal on netherrack beside nylium spreads the nylium (1.16's {@code NetherrackBlock}): any nylium in
     * the 3x3x3 around it and air above; with both kinds about, either at random.
     */
    @SubscribeEvent
    public static void bonemealNetherrack(BonemealEvent event) {
        if (event.getBlock().getBlock() != Blocks.NETHERRACK || !event.getWorld().isAirBlock(event.getPos().up())) {
            return;
        }
        boolean crimson = false;
        boolean warped = false;
        for (BlockPos p : BlockPos.getAllInBox(event.getPos().add(-1, -1, -1), event.getPos().add(1, 1, 1))) {
            Block b = event.getWorld().getBlockState(p).getBlock();
            crimson |= b == nylium(NetherWood.CRIMSON);
            warped |= b == nylium(NetherWood.WARPED);
        }
        if (!crimson && !warped) {
            return;
        }
        event.setResult(Event.Result.ALLOW);
        World world = event.getWorld();
        if (!world.isRemote) {
            boolean useWarped = crimson && warped ? world.rand.nextBoolean() : warped;
            world.setBlockState(event.getPos(), nylium(useWarped ? NetherWood.WARPED : NetherWood.CRIMSON).getDefaultState(), 3);
        } else {
            for (int i = 0; i < 15; i++) {
                world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, event.getPos().getX() + world.rand.nextDouble(),
                        event.getPos().getY() + 1.0 + world.rand.nextDouble() * 0.2, event.getPos().getZ() + world.rand.nextDouble(), 0, 0, 0);
            }
        }
    }

    /** Nether wood is not furnace fuel, although 1.12.2 burns anything of wood material. */
    @SubscribeEvent
    public static void fuel(FurnaceFuelBurnTimeEvent event) {
        if (NOT_FUEL.contains(event.getItemStack().getItem())) {
            event.setBurnTime(0);
        }
    }

    /** @return the stripped form of a stem or hyphae block, or null */
    public static Block stripped(Block block) {
        return STRIPPED.get(block);
    }
}

package com.exiledradio.netherupdatebackport.blocks;

import com.exiledradio.backportcore.ownership.BlockOwnership;
import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Sounds;
import com.exiledradio.netherupdatebackport.common.shape.Shapes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream U: chains, crying obsidian, targets, respawn anchors, lodestones and the lodestone compass, nether
 * gold ore, quartz bricks, cracked and chiseled nether bricks.
 *
 * <p>Numbers from 1.16.5's {@code Blocks}: chain 5/6, crying obsidian and respawn anchor 50/1200 (diamond
 * pickaxe), lodestone 3.5/3.5, target 0.5, nether gold ore 3/3, cracked and chiseled nether bricks 2/6,
 * quartz bricks as the quartz block (0.8).
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class BlocksContent {

    public static final String[] NAMES = {
            "chain", "crying_obsidian", "target", "respawn_anchor", "lodestone", "nether_gold_ore", "quartz_bricks",
            "cracked_nether_bricks", "chiseled_nether_bricks",
    };

    public static Block chain;
    public static Block cryingObsidian;
    public static BlockTarget target;
    public static BlockRespawnAnchor respawnAnchor;
    public static Block lodestone;
    public static Block netherGoldOre;
    public static ItemLodestoneCompass lodestoneCompass;

    // 1.16's sounds the blocks play themselves.
    static final SoundEvent ANCHOR_CHARGE = Sounds.event("block.respawn_anchor.charge");
    static final SoundEvent ANCHOR_SET_SPAWN = Sounds.event("block.respawn_anchor.set_spawn");
    static final SoundEvent ANCHOR_DEPLETE = Sounds.event("block.respawn_anchor.deplete");
    static final SoundEvent ANCHOR_AMBIENT = Sounds.event("block.respawn_anchor.ambient");
    static final SoundEvent COMPASS_LOCK = Sounds.event("item.lodestone_compass.lock");

    private static final List<Block> WITH_ITEMS = new ArrayList<Block>();

    private BlocksContent() {
    }

    public static boolean enabled() {
        return ModConfig.blocks.enabled;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BlockOwnership.declare(NetherUpdateBackport.MOD_ID, NAMES);
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        SoundType netherBricks = Sounds.type("nether_bricks");

        chain = item(Content.block(r, "chain", new BlockChain(Sounds.type("chain")), "pickaxe"));
        cryingObsidian = item(Content.block(r, "crying_obsidian", new BlockCryingObsidian(), "pickaxe"));
        cryingObsidian.setHarvestLevel("pickaxe", 3);
        target = item(Content.block(r, "target", new BlockTarget(), "hoe"));
        respawnAnchor = item(Content.block(r, "respawn_anchor", new BlockRespawnAnchor(), "pickaxe"));
        respawnAnchor.setHarvestLevel("pickaxe", 3);
        lodestone = item(Content.block(r, "lodestone",
                new Shapes.Plain(Material.ANVIL, MapColor.IRON, 3.5F, 3.5F, Sounds.type("lodestone")), "pickaxe"));
        netherGoldOre = item(Content.block(r, "nether_gold_ore", new BlockNetherGoldOre(Sounds.type("nether_gold_ore")), "pickaxe"));
        item(Content.block(r, "quartz_bricks", new Shapes.Plain(Material.ROCK, MapColor.QUARTZ, 0.8F, 0.8F, SoundType.STONE), "pickaxe"));
        item(Content.block(r, "cracked_nether_bricks", new Shapes.Plain(Material.ROCK, MapColor.NETHERRACK, 2.0F, 6.0F, netherBricks), "pickaxe"));
        item(Content.block(r, "chiseled_nether_bricks", new Shapes.Plain(Material.ROCK, MapColor.NETHERRACK, 2.0F, 6.0F, netherBricks), "pickaxe"));

        GameRegistry.registerWorldGenerator(new NetherGoldGeneration(), 10);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        for (Block block : WITH_ITEMS) {
            Content.registerBlockItem(event.getRegistry(), block);
        }
        lodestoneCompass = Content.item(event.getRegistry(), "lodestone_compass", new ItemLodestoneCompass());
    }

    /** Called from init: a dispenser holding glowstone charges a respawn anchor in front of it (1.16). */
    public static void init() {
        if (!enabled()) {
            return;
        }
        Item glowstone = Item.getItemFromBlock(Blocks.GLOWSTONE);
        final IBehaviorDispenseItem previous = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(glowstone);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(glowstone, new BehaviorDefaultDispenseItem() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
                BlockPos pos = source.getBlockPos().offset(facing);
                IBlockState state = source.getWorld().getBlockState(pos);
                if (state.getBlock() == respawnAnchor && state.getValue(BlockRespawnAnchor.CHARGE) < BlockRespawnAnchor.MAX) {
                    BlockRespawnAnchor.charge(source.getWorld(), pos, state);
                    stack.shrink(1);
                    return stack;
                }
                return previous != null ? previous.dispense(source, stack) : super.dispenseStack(source, stack);
            }
        });
    }

    private static <B extends Block> B item(B block) {
        WITH_ITEMS.add(block);
        return block;
    }
}

package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 1.16's hoe: a real tool ({@code HoeItem extends ToolItem}) whose blocks are leaves, hay bales, nether and
 * warped wart blocks, dried kelp blocks, targets, shroomlights and sponges (1.16.5's {@code DIGGABLES},
 * today's {@code #mineable/hoe} for the blocks 1.12.2 has).
 *
 * <ul>
 *   <li><b>Harvest tool</b>: those blocks get {@code setHarvestLevel("hoe", 0)} (none of them needs a tool to
 *       drop, so drops are unchanged), and every hoe gets the {@code hoe} tool class at its material's
 *       level, so other mods' "right tool" checks see it. Our own target, warped wart block and shroomlight
 *       are registered with it already; Update Aquatic's dried kelp block is found by name. Modded leaves
 *       (any {@link BlockLeaves}) get it too, as today's {@code #leaves} tag carries them, but a block that
 *       already names a tool keeps it.</li>
 *   <li><b>Speed</b>: on those blocks a hoe digs at its material's efficiency (wood 2, stone 4, iron 6,
 *       diamond 8, gold 12), plus Efficiency's {@code level² + 1}, as any 1.16 tool. 1.12.2's
 *       {@link ItemHoe} digs at 1 everywhere, so the dig speed event multiplies the hand speed vanilla
 *       already worked out (haste, fatigue, water and air included) by that.</li>
 *   <li><b>Durability</b>: {@code ToolItem.mineBlock} takes 1 for any block that does not break instantly,
 *       and {@code hurtEnemy} takes 2 per hit; 1.12.2's hoe takes nothing for blocks and 1 for a hit. The
 *       missing amounts are taken here. A hoe class of another mod that writes its own mining or hitting
 *       code is left alone.</li>
 * </ul>
 * No vanilla class is replaced or patched.
 */
public final class HoeTool {

    public static final String HOE = "hoe";

    /**
     * Blocks of other mods by name: Update Aquatic owns 1.13's dried kelp block. Future MC's copies of the
     * nether blocks are listed so a pack that has them gets the same tool.
     */
    private static final String[] OTHER_MODS = {
            Owners.UPDATE_AQUATIC + ":dried_kelp_block",
            "futuremc:warped_wart_block",
            "futuremc:shroomlight",
    };

    /** Per hoe item: its material, and whether it brings its own mining or hitting code. */
    private static final Map<Item, Hoe> HOES = new IdentityHashMap<Item, Hoe>();

    /** Registered on the event bus by {@link ChangesContent} when hoes are switched on. */
    HoeTool() {
    }

    /** From init, after every mod's blocks and items are registered. */
    static void init() {
        Block[] vanilla = {Blocks.LEAVES, Blocks.LEAVES2, Blocks.HAY_BLOCK, Blocks.NETHER_WART_BLOCK, Blocks.SPONGE};
        for (Block block : vanilla) {
            block.setHarvestLevel(HOE, 0);
        }
        int others = 0;
        for (String id : OTHER_MODS) {
            Block block = Owners.first(id);
            if (block != null && untooled(block)) {
                block.setHarvestLevel(HOE, 0);
                others++;
            }
        }
        for (Block block : ForgeRegistries.BLOCKS) {
            if (block instanceof BlockLeaves && untooled(block)) {
                block.setHarvestLevel(HOE, 0);
                others++;
            }
        }
        int hoes = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof ItemHoe) {
                Hoe hoe = hoe(item);
                if (hoe.material != null && item.getToolClasses(new ItemStack(item)).isEmpty()) {
                    item.setHarvestLevel(HOE, hoe.material.getHarvestLevel());
                }
                hoes++;
            }
        }
        NetherUpdateBackport.LOGGER.info("Hoes are the tool for {} vanilla and {} other blocks; {} hoe items.", vanilla.length, others, hoes);
    }

    private static boolean untooled(Block block) {
        return block.getHarvestTool(block.getDefaultState()) == null;
    }

    /** @return true if a hoe is the right tool for this state */
    public static boolean effective(IBlockState state) {
        return state.getBlock().isToolEffective(HOE, state);
    }

    /** @return this hoe's dig speed on its own blocks: its material's efficiency, 1 if unknown */
    public static float efficiency(Item item) {
        Hoe hoe = item instanceof ItemHoe ? hoe(item) : null;
        return hoe == null || hoe.material == null ? 1.0F : hoe.material.getEfficiency();
    }

    @SubscribeEvent
    public void digSpeed(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = player.getHeldItemMainhand();
        IBlockState state = event.getState();
        // A hoe of another mod that already digs faster here keeps its own speed.
        if (!(stack.getItem() instanceof ItemHoe) || !effective(state) || stack.getDestroySpeed(state) > 1.0F) {
            return;
        }
        float speed = efficiency(stack.getItem());
        if (speed <= 1.0F) {
            return;
        }
        int level = EnchantmentHelper.getEfficiencyModifier(player);
        if (level > 0) {
            speed += level * level + 1;
        }
        event.setNewSpeed(event.getNewSpeed() * speed);
    }

    /** {@code ToolItem.mineBlock}: 1 durability for every block that does not break instantly. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void broke(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        ItemStack stack = player.getHeldItemMainhand();
        if (world.isRemote || !(stack.getItem() instanceof ItemHoe) || hoe(stack.getItem()).ownMining
                || event.getState().getBlockHardness(world, pos) == 0.0F) {
            return;
        }
        // Creative players take no damage: damageItem checks that.
        stack.damageItem(1, player);
    }

    /** {@code ToolItem.hurtEnemy} takes 2; 1.12.2's hoe takes 1 itself, after the hit lands. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void hit(LivingHurtEvent event) {
        if (!"player".equals(event.getSource().getDamageType())) {
            return;
        }
        Entity source = event.getSource().getTrueSource();
        if (!(source instanceof EntityPlayer) || event.getSource().getImmediateSource() != source
                || ((EntityPlayer) source).world.isRemote) {
            return;
        }
        EntityLivingBase player = (EntityLivingBase) source;
        ItemStack stack = player.getHeldItemMainhand();
        if (stack.getItem() instanceof ItemHoe && !hoe(stack.getItem()).ownHitting) {
            stack.damageItem(1, player);
        }
    }

    private static Hoe hoe(Item item) {
        Hoe hoe = HOES.get(item);
        if (hoe == null) {
            hoe = new Hoe(item);
            HOES.put(item, hoe);
        }
        return hoe;
    }

    private static final class Hoe {
        final Item.ToolMaterial material;
        final boolean ownMining;
        final boolean ownHitting;

        Hoe(Item item) {
            Item.ToolMaterial found = null;
            try {
                found = ReflectionHelper.getPrivateValue(ItemHoe.class, (ItemHoe) item, "toolMaterial", "field_77843_a");
            } catch (RuntimeException e) {
                NetherUpdateBackport.LOGGER.warn("Could not read the material of hoe {}; it digs at hand speed.", item.getRegistryName(), e);
            }
            this.material = found;
            this.ownMining = overrides(item.getClass(), "onBlockDestroyed", "func_179218_a",
                    ItemStack.class, World.class, IBlockState.class, BlockPos.class, EntityLivingBase.class);
            this.ownHitting = overrides(item.getClass(), "hitEntity", "func_77644_a",
                    ItemStack.class, EntityLivingBase.class, EntityLivingBase.class);
        }

        /** @return true if a class between this one and {@link ItemHoe} (excluded) declares the method */
        private static boolean overrides(Class<?> cls, String mcp, String srg, Class<?>... params) {
            for (Class<?> c = cls; c != null && c != ItemHoe.class; c = c.getSuperclass()) {
                for (String name : new String[]{mcp, srg}) {
                    try {
                        c.getDeclaredMethod(name, params);
                        return true;
                    } catch (NoSuchMethodException ignored) {
                        // not here
                    } catch (LinkageError e) {
                        return false;
                    }
                }
            }
            return false;
        }
    }
}

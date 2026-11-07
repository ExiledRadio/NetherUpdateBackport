package com.exiledradio.netherupdatebackport.piglin;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerShulkerBox;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;

import javax.annotation.Nullable;

/**
 * Stream P: piglins, piglin brutes, bartering and the Snout banner pattern. Entity ids 15 (piglin) and 16 (piglin
 * brute); spawn eggs in 1.16's colors.
 *
 * <h2>Spawning</h2>
 * Piglins spawn in the nether wastes, which is 1.12.2's {@code Hell} biome, as in 1.16 (weight 15, groups of 4),
 * behind {@code piglins.spawn}; their crimson forest spawns come with the biome (stream V). Brutes spawn only in
 * bastions (stream X), through {@link BastionMobs}.
 *
 * <h2>Gold</h2>
 * Idle piglins that can see a player opening a chest, trapped chest, ender chest, barrel, shulker box or chest or
 * hopper minecart turn on them; breaking a guarded block (gold blocks, gold ores, gilded blackstone, containers)
 * angers every idle piglin within 16 blocks, seen or not ({@code PiglinTasks.angerNearbyPiglins}).
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class PiglinContent {

    static final SoundEvent PIGLIN_ADMIRING_ITEM = Sounds.event("entity.piglin.admiring_item");
    static final SoundEvent PIGLIN_AMBIENT = Sounds.event("entity.piglin.ambient");
    static final SoundEvent PIGLIN_ANGRY = Sounds.event("entity.piglin.angry");
    static final SoundEvent PIGLIN_CELEBRATE = Sounds.event("entity.piglin.celebrate");
    static final SoundEvent PIGLIN_CONVERTED = Sounds.event("entity.piglin.converted_to_zombified");
    static final SoundEvent PIGLIN_DEATH = Sounds.event("entity.piglin.death");
    static final SoundEvent PIGLIN_HURT = Sounds.event("entity.piglin.hurt");
    static final SoundEvent PIGLIN_JEALOUS = Sounds.event("entity.piglin.jealous");
    static final SoundEvent PIGLIN_RETREAT = Sounds.event("entity.piglin.retreat");
    static final SoundEvent PIGLIN_STEP = Sounds.event("entity.piglin.step");
    static final SoundEvent BRUTE_AMBIENT = Sounds.event("entity.piglin_brute.ambient");
    static final SoundEvent BRUTE_ANGRY = Sounds.event("entity.piglin_brute.angry");
    static final SoundEvent BRUTE_CONVERTED = Sounds.event("entity.piglin_brute.converted_to_zombified");
    static final SoundEvent BRUTE_DEATH = Sounds.event("entity.piglin_brute.death");
    static final SoundEvent BRUTE_HURT = Sounds.event("entity.piglin_brute.hurt");
    static final SoundEvent BRUTE_STEP = Sounds.event("entity.piglin_brute.step");
    // The crossbow's own events (stream Ow's copy has the same names, so these are the same objects).
    static final SoundEvent CROSSBOW_SHOOT = Sounds.event("item.crossbow.shoot");
    static final SoundEvent CROSSBOW_LOADING_START = Sounds.event("item.crossbow.loading_start");
    static final SoundEvent CROSSBOW_LOADING_MIDDLE = Sounds.event("item.crossbow.loading_middle");
    static final SoundEvent CROSSBOW_LOADING_END = Sounds.event("item.crossbow.loading_end");

    @Nullable
    public static Item snoutPattern;

    private PiglinContent() {
    }

    public static boolean enabled() {
        return ModConfig.piglins.enabled;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        snoutPattern = Content.item(event.getRegistry(), "piglin_banner_pattern", new ItemSnoutPattern());
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityPiglin.class).id(Content.id("piglin"), 15)
                .name(NetherUpdateBackport.MOD_ID + ".piglin").tracker(80, 3, true).egg(0x995F40, 0xF9F3A4).build());
        event.getRegistry().register(EntityEntryBuilder.create().entity(EntityPiglinBrute.class).id(Content.id("piglin_brute"), 16)
                .name(NetherUpdateBackport.MOD_ID + ".piglin_brute").tracker(80, 3, true).egg(0x592A10, 0xF9F3A4).build());
        Bartering.register();
    }

    /** Called from init: piglins in the nether wastes. */
    public static void init() {
        if (enabled() && ModConfig.piglins.spawn) {
            EntityRegistry.addSpawn(EntityPiglin.class, 15, 4, 4, EnumCreatureType.MONSTER, net.minecraft.init.Biomes.HELL);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Gold anger
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (enabled() && !event.isCanceled() && player != null && !event.getWorld().isRemote && PiglinTasks.isGuardedBlock(event.getState())) {
            PiglinTasks.angerNearbyPiglins(player, false);
        }
    }

    @SubscribeEvent
    public static void onOpen(PlayerContainerEvent.Open event) {
        EntityPlayer player = event.getEntityPlayer();
        if (enabled() && !player.world.isRemote && isGuardedContainer(event.getContainer())) {
            PiglinTasks.angerNearbyPiglins(player, true);
        }
    }

    /** @return true for the containers 1.16 guards: chests, ender chests, barrels, shulker boxes, container minecarts */
    public static boolean isGuardedContainer(Container container) {
        if (container instanceof ContainerShulkerBox) {
            return true;
        }
        IInventory inv = null;
        if (container instanceof ContainerChest) {
            inv = ((ContainerChest) container).getLowerChestInventory();
            if (inv instanceof TileEntityChest || inv instanceof InventoryLargeChest || inv instanceof InventoryEnderChest) {
                return true;
            }
        } else if (!container.inventorySlots.isEmpty()) {
            Slot first = container.inventorySlots.get(0);
            inv = first.inventory;
        }
        if (inv instanceof EntityMinecartContainer) {
            return true;
        }
        if (inv instanceof TileEntity) {
            TileEntity te = (TileEntity) inv;
            return te.getWorld() != null && PiglinTasks.isGuardedBlock(te.getWorld().getBlockState(te.getPos()));
        }
        return false;
    }

    /** The Snout banner pattern: a pattern item (one to a stack, uncommon), named "Banner Pattern" over "Snout". */
    static final class ItemSnoutPattern extends Item {
        ItemSnoutPattern() {
            setMaxStackSize(1);
        }

        @Override
        public EnumRarity getRarity(ItemStack stack) {
            return EnumRarity.UNCOMMON;
        }

        @Override
        @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
        public void addInformation(ItemStack stack, @Nullable net.minecraft.world.World world, java.util.List<String> tooltip,
                                   net.minecraft.client.util.ITooltipFlag flag) {
            tooltip.add(net.minecraft.util.text.TextFormatting.GRAY + net.minecraft.client.resources.I18n.format(getTranslationKey() + ".desc"));
        }
    }
}

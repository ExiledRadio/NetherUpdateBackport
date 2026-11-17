package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.passive.AbstractChestHorse;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.HorseArmorType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ContainerHorseChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 1.16's smaller changes that need no patch: dispensers, the enderman's blocks, piston hardness, the vine's
 * sound, burning projectiles and TNT, and the End platform. Patches P12 to P25 and their hooks
 * ({@link SmallHooks}) do the rest.
 */
public final class SmallChanges {

    private static final Field HORSE_CHEST = ReflectionHelper.findField(AbstractHorse.class, "horseChest", "field_110296_bG");
    private static final Field SOUND = ReflectionHelper.findField(Block.class, "blockSoundType", "field_149762_H");

    /** 1.16's {@code SoundType.VINE}: grass, but its own step (the climbing sound). */
    static SoundType vine;
    /** Set by {@link #init}, for the self-test. */
    public static boolean pistonsHarder;

    SmallChanges() {
    }

    private static boolean on() {
        return ModConfig.changes.enabled;
    }

    /** From pre-init: the vine's sound event exists before the sound registry event. */
    static void preInit() {
        vine = new SoundType(1.0F, 1.0F, SoundEvents.BLOCK_GRASS_BREAK, Sounds.event("block.vine.step"), SoundEvents.BLOCK_GRASS_PLACE,
                SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);
    }

    /** From init: every mod's blocks and items are registered. */
    static void init() {
        if (ModConfig.changes.dispensers) {
            dispensers();
        }
        if (ModConfig.changes.endermanBlocks) {
            endermanBlocks();
        }
        if (ModConfig.changes.pistonHardness) {
            // 20w06a: hardness and blast resistance 0.5 to 1.5 (setHardness raises the resistance to match), mined
            // with a pickaxe. Material.PISTON needs no tool, so a hand still gets the drop.
            for (Block b : new Block[] {Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.PISTON_HEAD}) {
                b.setHardness(1.5F);
                b.setHarvestLevel("pickaxe", 0);
            }
            pistonsHarder = true;
        }
        if (ModConfig.changes.vineClimbing) {
            try {
                SOUND.set(Blocks.VINE, vine);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    // Enderman ----------------------------------------------------------------------------------------------------

    /** 20w07a: 1.16.5's {@code ENDERMAN_HOLDABLE} adds nylium, fungi and roots and drops netherrack. */
    static void endermanBlocks() {
        EntityEnderman.setCarriable(Blocks.NETHERRACK, false);
        for (String name : new String[] {"crimson_nylium", "warped_nylium", "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots"}) {
            Block b = Owners.ourBlock(name);
            if (b != null) {
                EntityEnderman.setCarriable(b, true);
            }
        }
    }

    // Dispensers -----------------------------------------------------------------------------------------------------

    /** 20w15a: 1.16.5's dispenser behaviors for saddles, horse armor, carpets, chests and (mooshrooms, snow golems) shears. */
    static void dispensers() {
        wrap(Items.SADDLE, new Use() {
            @Override
            public boolean use(World world, BlockPos front, ItemStack stack) {
                return saddle(world, front, stack);
            }
        });
        Use armor = new Use() {
            @Override
            public boolean use(World world, BlockPos front, ItemStack stack) {
                return armor(world, front, stack);
            }
        };
        int armors = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            if (item.getHorseArmorType(new ItemStack(item)) != HorseArmorType.NONE) {
                wrap(item, armor);
                armors++;
            }
        }
        wrap(Item.getItemFromBlock(Blocks.CARPET), armor);
        wrap(Item.getItemFromBlock(Blocks.CHEST), new Use() {
            @Override
            public boolean use(World world, BlockPos front, ItemStack stack) {
                return chest(world, front, stack);
            }
        });
        wrap(Items.SHEARS, new Use() {
            @Override
            public boolean use(World world, BlockPos front, ItemStack stack) {
                return shear(world, front, stack);
            }
        });
        NetherUpdateBackport.LOGGER.debug("Dispensers: saddles, {} horse armors, carpets, chests and shears.", armors);
    }

    /** An equipping use; true when it worked (the stack is changed in place). */
    interface Use {
        boolean use(World world, BlockPos front, ItemStack stack);
    }

    /**
     * 1.16's {@code OptionalDispenseBehavior}: the click and the smoke when it worked; otherwise whatever this item did
     * before (vanilla's drop, or another mod's own behavior).
     */
    private static void wrap(Item item, final Use use) {
        if (item == null || item == Items.AIR) {
            return;
        }
        final IBehaviorDispenseItem previous = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(item);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(item, new IBehaviorDispenseItem() {
            @Override
            public ItemStack dispense(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
                BlockPos front = source.getBlockPos().offset(facing);
                if (!world.isRemote && on() && ModConfig.changes.dispensers && use.use(world, front, stack)) {
                    world.playEvent(1000, source.getBlockPos(), 0);
                    world.playEvent(2000, source.getBlockPos(), facing.getXOffset() + 1 + (facing.getZOffset() + 1) * 3);
                    return stack;
                }
                return previous.dispense(source, stack);
            }
        });
    }

    /** {@code IEquipable}: a pig, a strider or a tamed horse, donkey or mule, grown and unsaddled. */
    static boolean saddle(World world, BlockPos front, ItemStack stack) {
        for (EntityLivingBase e : world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(front))) {
            if (!e.isEntityAlive() || e.isChild()) {
                continue;
            }
            if (e instanceof EntityPig && !((EntityPig) e).getSaddled()) {
                ((EntityPig) e).setSaddled(true);
                world.playSound(null, e.posX, e.posY, e.posZ, SoundEvents.ENTITY_PIG_SADDLE, SoundCategory.BLOCKS, 0.5F, 1.0F);
            } else if (e instanceof AbstractHorse && ((AbstractHorse) e).canBeSaddled() && ((AbstractHorse) e).isTame()
                    && !((AbstractHorse) e).isHorseSaddled()) {
                // The horse's own inventory listener plays the saddle sound.
                ((AbstractHorse) e).replaceItemInInventory(400, new ItemStack(Items.SADDLE));
            } else if (!strider(e)) {
                continue;
            }
            stack.shrink(1);
            return true;
        }
        return false;
    }

    private static Method striderSaddled;
    private static Method striderEquip;

    /** Stream R's strider, by its own public methods (streams never link against each other). */
    private static boolean strider(EntityLivingBase e) {
        if (!com.exiledradio.netherupdatebackport.common.Content.id("strider").equals(net.minecraft.entity.EntityList.getKey(e))) {
            return false;
        }
        try {
            if (striderEquip == null) {
                striderSaddled = e.getClass().getMethod("isSaddled");
                striderEquip = e.getClass().getMethod("equipSaddle", boolean.class);
            }
            if ((Boolean) striderSaddled.invoke(e)) {
                return false;
            }
            striderEquip.invoke(e, Boolean.TRUE);
            return true;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("The strider has no isSaddled/equipSaddle", ex);
        }
    }

    /** Horse armor on a tamed horse, a carpet on a tamed llama, where it has none yet. */
    static boolean armor(World world, BlockPos front, ItemStack stack) {
        for (AbstractHorse horse : world.getEntitiesWithinAABB(AbstractHorse.class, new AxisAlignedBB(front))) {
            if (!horse.isEntityAlive() || !horse.wearsArmor()) {
                continue;
            }
            if (horse.isArmor(stack) && !wearingArmor(horse) && horse.isTame()) {
                horse.replaceItemInInventory(401, stack.splitStack(1));
                return true;
            }
        }
        return false;
    }

    private static boolean wearingArmor(AbstractHorse horse) {
        try {
            ContainerHorseChest chest = (ContainerHorseChest) HORSE_CHEST.get(horse);
            return chest.getSizeInventory() > 1 && !chest.getStackInSlot(1).isEmpty();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A chest on a tamed llama, donkey or mule that has none. */
    static boolean chest(World world, BlockPos front, ItemStack stack) {
        for (AbstractChestHorse horse : world.getEntitiesWithinAABB(AbstractChestHorse.class, new AxisAlignedBB(front))) {
            if (horse.isEntityAlive() && !horse.hasChest() && horse.isTame() && horse.replaceItemInInventory(499, stack)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * 1.16.5's {@code BeehiveDispenseBehavior.tryShearLivingEntity}, for the two 1.16 added: mooshrooms and snow golems
     * (a sheep is 1.14's, Village &amp; Pillage's; a beehive is Buzzy Bees Backport's own wrapper).
     */
    static boolean shear(World world, BlockPos front, ItemStack stack) {
        for (EntityLiving e : world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(front))) {
            if (!e.isEntityAlive() || !(e instanceof EntityMooshroom) && !(e instanceof EntitySnowman)) {
                continue;
            }
            IShearable target = (IShearable) e;
            BlockPos pos = new BlockPos(e.posX, e.posY, e.posZ);
            if (!target.isShearable(stack, world, pos)) {
                continue;
            }
            List<ItemStack> drops = target.onSheared(stack, world, pos, 0);
            for (ItemStack drop : drops) {
                EntityItem item = e.entityDropItem(drop, 1.0F);
                if (item != null) {
                    item.motionY += world.rand.nextFloat() * 0.05F;
                    item.motionX += (world.rand.nextFloat() - world.rand.nextFloat()) * 0.1F;
                    item.motionZ += (world.rand.nextFloat() - world.rand.nextFloat()) * 0.1F;
                }
            }
            if (stack.attemptDamageItem(1, world.rand, null)) {
                stack.setCount(0);
            }
            return true;
        }
        return false;
    }

    // Burning projectiles and TNT ----------------------------------------------------------------------------------------

    /**
     * 1.16.5's {@code TNTBlock.onProjectileHit}: any projectile on fire lights the TNT it hits. Burning arrows already
     * do in 1.12.2 (when they stick in it), so they are left to vanilla.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void impact(ProjectileImpactEvent event) {
        Entity projectile = event.getEntity();
        RayTraceResult ray = event.getRayTraceResult();
        World world = projectile.world;
        if (world.isRemote || event.isCanceled() || ray == null || ray.typeOfHit != RayTraceResult.Type.BLOCK || projectile instanceof EntityArrow
                || !projectile.isBurning() || !on() || !ModConfig.changes.burningProjectilesTnt) {
            return;
        }
        BlockPos pos = ray.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockTNT)) {
            return;
        }
        Entity owner = projectile instanceof EntityThrowable ? ((EntityThrowable) projectile).getThrower()
                : projectile instanceof EntityFireball ? ((EntityFireball) projectile).shootingEntity : null;
        ((BlockTNT) state.getBlock()).explode(world, pos, state.withProperty(BlockTNT.EXPLODE, Boolean.TRUE),
                owner instanceof EntityLivingBase ? (EntityLivingBase) owner : null);
        world.setBlockToAir(pos);
    }

    // The cave mood (client: changes.client.CaveMood) --------------------------------------------------------------------

    /**
     * 1.16.5's {@code BiomeSoundHandler} mood step: sky light lowers it a little; without sky light, block light above 0
     * lowers it and darkness raises it by {@code 1 / tickDelay} (6000 for the cave).
     */
    public static float caveMood(float moodiness, int skyLight, int blockLight, int tickDelay) {
        if (skyLight > 0) {
            return moodiness - (float) skyLight / 15.0F * 0.001F;
        }
        return moodiness - (float) (blockLight - 1) / (float) tickDelay;
    }

    // The End platform ---------------------------------------------------------------------------------------------------

    /** 1.16.5's {@code ServerWorld.END_SPAWN_POINT}. */
    static final BlockPos END_SPAWN = new BlockPos(100, 50, 0);

    /**
     * 20w21a: any entity going through an End portal rebuilds the platform ({@code makeObsidianPlatform}), not only a
     * player (1.12.2's {@code Teleporter.placeInPortal} does that one).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void travel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (event.isCanceled() || event.getDimension() != 1 || entity.dimension == 1 || entity instanceof EntityPlayer
                || entity.world.isRemote || !on() || !ModConfig.changes.endPlatform) {
            return;
        }
        WorldServer end = entity.getServer() == null ? null : entity.getServer().getWorld(1);
        if (end != null && end.provider.getDimensionType().getId() == 1) {
            makePlatform(end);
        }
    }

    /** 1.16.5's {@code ServerWorld.makeObsidianPlatform}: 5x5 obsidian at y 48, three layers of air above. */
    public static void makePlatform(World end) {
        int x = END_SPAWN.getX();
        int y = END_SPAWN.getY() - 2;
        int z = END_SPAWN.getZ();
        for (BlockPos p : BlockPos.getAllInBoxMutable(x - 2, y + 1, z - 2, x + 2, y + 3, z + 2)) {
            end.setBlockState(p, Blocks.AIR.getDefaultState());
        }
        for (BlockPos p : BlockPos.getAllInBoxMutable(x - 2, y, z - 2, x + 2, y, z + 2)) {
            end.setBlockState(p, Blocks.OBSIDIAN.getDefaultState());
        }
    }

    /**
     * 1.16.2 (20w28a): a player arriving in the End stands in the middle of the platform's center block, not on the
     * corner of it (1.12.2's {@code placeInPortal} puts it at the block's whole coordinates).
     */
    @SubscribeEvent
    public void arrived(PlayerEvent.PlayerChangedDimensionEvent event) {
        EntityPlayer player = event.player;
        if (event.toDim != 1 || !(player instanceof EntityPlayerMP) || !on() || !ModConfig.changes.endPlatform) {
            return;
        }
        if (player.posX == END_SPAWN.getX() && player.posZ == END_SPAWN.getZ() && player.posY == END_SPAWN.getY() - 1) {
            ((EntityPlayerMP) player).connection.setPlayerLocation(player.posX + 0.5D, player.posY, player.posZ + 0.5D,
                    player.rotationYaw, player.rotationPitch);
        }
    }
}

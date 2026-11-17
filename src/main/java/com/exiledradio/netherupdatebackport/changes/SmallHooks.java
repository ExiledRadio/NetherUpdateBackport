package com.exiledradio.netherupdatebackport.changes;

import com.exiledradio.netherupdatebackport.ModConfig;
import com.exiledradio.netherupdatebackport.common.AngerGameRules;
import com.exiledradio.netherupdatebackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The hooks of patches P12 to P25 ({@code asm.SmallChangesTransformer}, docs/PATCHES.md): 1.16's smaller
 * behavior changes where vanilla 1.12.2 code calls in. Each reads its setting on every call, so a change switched
 * off in the settings screen is vanilla's code again at once.
 */
public final class SmallHooks {

    private SmallHooks() {
    }

    private static boolean on() {
        return ModConfig.changes.enabled;
    }

    // P12: withers on soul soil -------------------------------------------------------------------------------

    @javax.annotation.Nullable
    private static Block soulSoil;
    private static boolean soulSoilLooked;

    @javax.annotation.Nullable
    static Block soulSoil() {
        if (!soulSoilLooked) {
            soulSoil = Owners.ourBlock("soul_soil");
            soulSoilLooked = soulSoil != null || net.minecraftforge.fml.common.Loader.instance().hasReachedState(
                    net.minecraftforge.fml.common.LoaderState.AVAILABLE);
        }
        return soulSoil;
    }

    /**
     * Stands in for {@code BlockStateMatcher.forBlock(Blocks.SOUL_SAND)} in the wither's patterns: 1.16.5's
     * {@code BlockTags.WITHER_SUMMON_BASE_BLOCKS}, soul sand and soul soil. Read on every match, as the patterns
     * themselves are built once.
     */
    public static com.google.common.base.Predicate<IBlockState> witherBase(final Block block) {
        return new com.google.common.base.Predicate<IBlockState>() {
            @Override
            public boolean apply(@javax.annotation.Nullable IBlockState state) {
                if (state == null) {
                    return false;
                }
                Block b = state.getBlock();
                return b == block || on() && ModConfig.changes.witherSoulSoil && b != null && b == soulSoil();
            }
        };
    }

    // P13: mobs avoid magma --------------------------------------------------------------------------------------

    /**
     * {@code checkNeighborBlocks}'s result: 1.16.5's {@code checkNeighbourBlocks} also counts magma blocks
     * ({@code isBurningBlock}) anywhere in the 3x3x3 around a walkable node, but its own column, as a fire danger.
     * Lava there is 1.15's change and Buzzy Bees Backport's.
     */
    public static PathNodeType neighbors(PathNodeType result, IBlockAccess world, int x, int y, int z) {
        if (result != PathNodeType.WALKABLE || !on() || !ModConfig.changes.magmaPathing) {
            return result;
        }
        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if ((dx != 0 || dz != 0) && world.getBlockState(pos.setPos(x + dx, y + dy, z + dz)).getBlock() == Blocks.MAGMA) {
                            return PathNodeType.DANGER_FIRE;
                        }
                    }
                }
            }
        } finally {
            pos.release();
        }
        return result;
    }

    // P14: hoe enchantments ---------------------------------------------------------------------------------------

    /** {@code EnumEnchantmentType.DIGGER}: 1.16's hoe is a {@code ToolItem}, so a digger too. */
    public static boolean hoeDigger(Item item) {
        return item instanceof ItemHoe && on() && ModConfig.changes.hoeEnchanting;
    }

    private static final Field HOE_MATERIAL = ReflectionHelper.findField(ItemHoe.class, "toolMaterial", "field_77843_a");

    /** {@code ItemHoe.getItemEnchantability}: 1.16's {@code TieredItem.getEnchantmentValue}, the material's. */
    public static int hoeEnchantability(ItemHoe hoe) {
        if (!on() || !ModConfig.changes.hoeEnchanting) {
            return 0;
        }
        try {
            Item.ToolMaterial material = (Item.ToolMaterial) HOE_MATERIAL.get(hoe);
            return material == null ? 0 : material.getEnchantability();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // P15: compasses and Curse of Vanishing ----------------------------------------------------------------------

    /** 1.16's {@code CompassItem implements IVanishable}: the curse applies to it (the lodestone compass is one). */
    public static boolean vanishingCompass(Enchantment enchantment, ItemStack stack) {
        if (enchantment != Enchantments.VANISHING_CURSE || !on() || !ModConfig.changes.compassVanishing) {
            return false;
        }
        Item item = stack.getItem();
        return item == Items.COMPASS || item == Owners.ours("lodestone_compass");
    }

    // P16: parrot imitation ----------------------------------------------------------------------------------------

    private static final Field MIMIC_SOUNDS = ReflectionHelper.findField(EntityParrot.class, "MIMIC_SOUNDS");
    private static Method vanillaMimic;

    public static boolean parrotActive() {
        return on() && ModConfig.changes.parrotImitation;
    }

    /**
     * Stands in for {@code playMimicSound(world, this)} in {@code EntityParrot.onLivingUpdate}: 1.16.5's
     * {@code aiStep} tries {@code imitateNearbyMobs} 1 tick in 400 (1.12.2: every tick, at 1 in 50).
     */
    public static boolean parrotTick(World world, Entity parrot) {
        if (!parrotActive()) {
            try {
                if (vanillaMimic == null) {
                    vanillaMimic = ReflectionHelper.findMethod(EntityParrot.class, "playMimicSound", "func_192006_b", World.class, Entity.class);
                }
                return (Boolean) vanillaMimic.invoke(null, world, parrot);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return world.rand.nextInt(400) == 0 && imitateNearbyMobs(world, parrot);
    }

    /**
     * {@code EntityParrot.playAmbientSound}, for a parrot on a player's shoulder: 1.16.5's
     * {@code playShoulderEntityAmbientSound}, 1 tick in 200 an imitation or (failing that) its own call.
     */
    public static void shoulderParrot(World world, Entity player) {
        if (player.isSilent() || world.rand.nextInt(200) != 0) {
            return;
        }
        if (!imitateNearbyMobs(world, player)) {
            world.playSound(null, player.posX, player.posY, player.posZ, ambient(world, world.rand), player.getSoundCategory(), 1.0F,
                    pitch(world.rand));
        }
    }

    /** 1.16.5's {@code ParrotEntity.imitateNearbyMobs}: half the time, one of the mobs within 20 it can imitate. */
    public static boolean imitateNearbyMobs(World world, Entity entity) {
        if (!entity.isEntityAlive() || entity.isSilent() || world.rand.nextInt(2) != 0) {
            return false;
        }
        final Map<Class<? extends Entity>, SoundEvent> sounds = mimicSounds();
        List<EntityLiving> list = world.getEntitiesWithinAABB(EntityLiving.class, entity.getEntityBoundingBox().grow(20.0D),
                new com.google.common.base.Predicate<EntityLiving>() {
                    @Override
                    public boolean apply(@javax.annotation.Nullable EntityLiving e) {
                        return e != null && sounds.containsKey(e.getClass());
                    }
                });
        if (list.isEmpty()) {
            return false;
        }
        EntityLiving mob = list.get(world.rand.nextInt(list.size()));
        if (mob.isSilent()) {
            return false;
        }
        world.playSound(null, entity.posX, entity.posY, entity.posZ, sounds.get(mob.getClass()), entity.getSoundCategory(), 0.7F,
                pitch(world.rand));
        return true;
    }

    /** 1.16.5's {@code getAmbient}: no imitation on Peaceful. */
    private static SoundEvent ambient(World world, Random rand) {
        if (world.getDifficulty() != EnumDifficulty.PEACEFUL && rand.nextInt(1000) == 0) {
            List<SoundEvent> list = new ArrayList<SoundEvent>(mimicSounds().values());
            SoundEvent sound = list.isEmpty() ? null : list.get(rand.nextInt(list.size()));
            return sound == null ? SoundEvents.ENTITY_PARROT_AMBIENT : sound;
        }
        return SoundEvents.ENTITY_PARROT_AMBIENT;
    }

    private static float pitch(Random rand) {
        return (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F;
    }

    @SuppressWarnings("unchecked")
    static Map<Class<? extends Entity>, SoundEvent> mimicSounds() {
        try {
            return (Map<Class<? extends Entity>, SoundEvent>) MIMIC_SOUNDS.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    // P17: snow golems and water bottles --------------------------------------------------------------------------

    /** 1.16's {@code isSensitiveToWater}, which the snow golem answers yes to (1.16-pre1). */
    public static boolean waterSensitive(EntityLivingBase entity) {
        return entity instanceof EntitySnowman && on() && ModConfig.changes.snowGolemWater;
    }

    // P18: undead horses on a lead ---------------------------------------------------------------------------------

    public static boolean leashActive() {
        return on() && ModConfig.changes.undeadHorseLeash;
    }

    /** 1.16's {@code AbstractHorseEntity} has no {@code canBeLeashed} of its own: {@code MobEntity}'s, for any horse. */
    public static boolean canLeash(AbstractHorse horse) {
        return !horse.getLeashed();
    }

    // P19: walls and panes -----------------------------------------------------------------------------------------

    /** 1.16.5's {@code WallBlock.connectsTo}: {@code block instanceof PaneBlock} (glass panes and iron bars). */
    public static boolean wallToPane(IBlockAccess world, BlockPos pos) {
        return on() && ModConfig.changes.wallsPanes && world.getBlockState(pos).getBlock() instanceof BlockPane;
    }

    // P20: furnace XP -------------------------------------------------------------------------------------------------

    /**
     * Before a broken furnace drops its items: 1.16.5's {@code getRecipesToAwardAndPopExperience}, for what 1.12.2
     * still owes, the output slot (1.12.2 pays a smelt's XP when its result is taken out). As 1.16's
     * {@code createExperience}: count times XP, the fraction a chance of one more, orbs at the block's center.
     */
    public static void furnaceBroken(World world, BlockPos pos) {
        if (world.isRemote || !on() || !ModConfig.changes.furnaceXp) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityFurnace)) {
            return;
        }
        ItemStack out = ((TileEntityFurnace) tile).getStackInSlot(2);
        if (out.isEmpty()) {
            return;
        }
        float each = FurnaceRecipes.instance().getSmeltingExperience(out);
        float total = out.getCount() * each;
        int xp = MathHelper.floor(total);
        float frac = total - xp;
        if (frac != 0.0F && Math.random() < frac) {
            xp++;
        }
        while (xp > 0) {
            int orb = EntityXPOrb.getXPSplit(xp);
            xp -= orb;
            world.spawnEntity(new EntityXPOrb(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, orb));
        }
    }

    // P21: suffocation --------------------------------------------------------------------------------------------------

    /** Farmland's and grass paths' {@code causesSuffocation}: 1.16's {@code isSuffocating(Blocks::always)}. */
    public static boolean suffocates() {
        return on() && ModConfig.changes.suffocation;
    }

    // P22: ghast sound range -------------------------------------------------------------------------------------------

    public static boolean ghastActive() {
        return on() && ModConfig.changes.ghastSoundRange;
    }

    // P23: armor stands in grass and snow -------------------------------------------------------------------------------

    /** Stands in for the two {@code world.setBlockToAir} of {@code ItemArmorStand.onItemUse}: 1.16 (20w07a) keeps them. */
    public static boolean armorStandClear(World world, BlockPos pos) {
        if (on() && ModConfig.changes.armorStandPlacement) {
            return false;
        }
        return world.setBlockToAir(pos);
    }

    // P24: universal anger ------------------------------------------------------------------------------------------------

    /** 1.16.5's {@code HurtByTargetGoal.canUse}: with {@code universalAnger}, a player's hit is not fought back here. */
    public static boolean ignoreHurtByPlayer(EntityCreature owner) {
        return owner.getRevengeTarget() instanceof EntityPlayer && AngerGameRules.universalAnger(owner.world);
    }

    // P25: climbing steps -----------------------------------------------------------------------------------------------

    private static volatile Set<Block> climbable;

    /**
     * {@code Entity.move}'s {@code block != Blocks.LADDER}, which keeps the climb out of the step distance: 1.16.5
     * tests {@code BlockTags.CLIMBABLE} (ladders, vines, scaffolding, weeping and twisting vines).
     */
    public static boolean climbable(Block block, Block ladder) {
        if (block == ladder) {
            return true;
        }
        if (block == null || !on() || !ModConfig.changes.vineClimbing) {
            return false;
        }
        return block instanceof BlockVine || climbableBlocks().contains(block);
    }

    private static Set<Block> climbableBlocks() {
        Set<Block> set = climbable;
        if (set == null) {
            set = Collections.newSetFromMap(new java.util.IdentityHashMap<Block, Boolean>());
            for (String name : new String[] {"weeping_vines", "weeping_vines_plant", "twisting_vines", "twisting_vines_plant"}) {
                Block b = Owners.ourBlock(name);
                if (b != null) {
                    set.add(b);
                }
            }
            Block scaffolding = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "scaffolding"));
            if (scaffolding != null && scaffolding != Blocks.AIR) {
                set.add(scaffolding);
            }
            if (net.minecraftforge.fml.common.Loader.instance().hasReachedState(net.minecraftforge.fml.common.LoaderState.AVAILABLE)) {
                climbable = set;
            }
        }
        return set;
    }
}

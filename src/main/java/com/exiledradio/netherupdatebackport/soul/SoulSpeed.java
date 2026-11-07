package com.exiledradio.netherupdatebackport.soul;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Sounds;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Soul Speed I-III: faster on soul sand and soul soil, and soul sand no longer slows the wearer.
 *
 * <p>1.16.5's numbers ({@code LivingEntity.tryAddSoulSpeed} and {@code SoulSpeedEnchantment}): boots only,
 * very rare, treasure (never from the enchanting table; bastion chests and bartering give it), costs
 * 10/level to +15. Each time the wearer moves onto a new block while on a soul block, speed gets
 * {@code 0.03 * (1 + 0.35 * level)} added (transient, removed off soul blocks) and the boots lose a point
 * of durability 4% of the time. Every 5 ticks while moving, a soul rises and {@code particle.soul_escape}
 * may play.
 *
 * <h2>Soul sand's slowdown</h2>
 * 1.12.2's soul sand multiplies a mob's horizontal motion by 0.4 from inside its own collision code, after
 * the move. For a wearer the next tick starts by dividing the same 0.4 back out, when the mob still stands
 * in soul sand, so its speed is what it would be on any block (1.16's {@code getBlockSpeedFactor} of 1). Both
 * sides do this, since players move on their own client.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class SoulSpeed extends Enchantment {

    static final UUID MODIFIER = UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038");
    static final SoundEvent SOUL_ESCAPE = Sounds.event("particle.soul_escape");

    /** Each mob's block last tick, to act on block changes as 1.16's {@code onChangedBlock}. */
    private static final Map<EntityLivingBase, BlockPos> LAST = new WeakHashMap<EntityLivingBase, BlockPos>();

    SoulSpeed() {
        super(Rarity.VERY_RARE, EnumEnchantmentType.ARMOR_FEET, new EntityEquipmentSlot[] {EntityEquipmentSlot.FEET});
    }

    @Override
    public int getMinEnchantability(int level) {
        return level * 10;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean isTreasureEnchantment() {
        return true;
    }

    @Override
    public boolean isAllowedOnBooks() {
        return false;
    }

    /** @return the block 1.16 reads for movement: half a block under the feet */
    static BlockPos below(EntityLivingBase mob) {
        return new BlockPos(MathHelper.floor(mob.posX), MathHelper.floor(mob.posY - 0.5000001), MathHelper.floor(mob.posZ));
    }

    /** @return true on soul sand or soul soil (1.16's {@code soul_speed_blocks}) */
    static boolean onSoulBlock(EntityLivingBase mob) {
        Block block = mob.world.getBlockState(below(mob)).getBlock();
        return block == Blocks.SOUL_SAND || block == SoulContent.soulSoil && block != null;
    }

    public static int level(EntityLivingBase mob) {
        return SoulContent.soulSpeed == null ? 0 : EnchantmentHelper.getMaxEnchantmentLevel(SoulContent.soulSpeed, mob);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase mob = event.getEntityLiving();
        if (SoulContent.soulSpeed == null) {
            return;
        }
        int level = level(mob);
        if (level > 0 && inSoulSand(mob)) {
            mob.motionX /= 0.4;
            mob.motionZ /= 0.4;
        }
        BlockPos now = new BlockPos(mob);
        if (!mob.world.isRemote && !now.equals(LAST.put(mob, now))) {
            changedBlock(mob, level);
        }
        if (mob.world.isRemote && level > 0 && mob.ticksExisted % 5 == 0 && mob.motionX != 0 && mob.motionZ != 0
                && !(mob instanceof EntityPlayer && ((EntityPlayer) mob).isSpectator()) && onSoulBlock(mob)) {
            com.exiledradio.netherupdatebackport.soul.client.SoulClient.soul(mob.world,
                    mob.posX + (mob.getRNG().nextDouble() - 0.5) * mob.width, mob.posY + 0.1,
                    mob.posZ + (mob.getRNG().nextDouble() - 0.5) * mob.width, mob.motionX * -0.2, 0.1, mob.motionZ * -0.2);
            float volume = mob.getRNG().nextFloat() * 0.4F + mob.getRNG().nextFloat() > 0.9F ? 0.6F : 0.0F;
            mob.world.playSound(mob.posX, mob.posY, mob.posZ, SOUL_ESCAPE, SoundCategory.PLAYERS, volume,
                    0.6F + mob.getRNG().nextFloat() * 0.4F, false);
        }
    }

    /** 1.16's {@code onChangedBlock} part for Soul Speed: drop the boost, then add it again if still on soul. */
    static void changedBlock(EntityLivingBase mob, int level) {
        IAttributeInstance speed = mob.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (speed.getModifier(MODIFIER) != null) {
            speed.removeModifier(MODIFIER);
        }
        if (level > 0 && !mob.world.isAirBlock(below(mob)) && onSoulBlock(mob)) {
            speed.applyModifier(new AttributeModifier(MODIFIER, "Soul speed boost", 0.03F * (1.0F + level * 0.35F), 0).setSaved(false));
            if (mob.getRNG().nextFloat() < 0.04F) {
                ItemStack boots = mob.getItemStackFromSlot(EntityEquipmentSlot.FEET);
                boots.damageItem(1, mob);
            }
        }
    }

    /**
     * @return true if soul sand slowed the mob last move: vanilla's {@code doBlockCollisions} calls every block
     * whose position the slightly shrunk box covers
     */
    static boolean inSoulSand(EntityLivingBase mob) {
        AxisAlignedBB box = mob.getEntityBoundingBox().shrink(0.001);
        for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            if (mob.world.getBlockState(p).getBlock() == Blocks.SOUL_SAND) {
                return true;
            }
        }
        return false;
    }
}

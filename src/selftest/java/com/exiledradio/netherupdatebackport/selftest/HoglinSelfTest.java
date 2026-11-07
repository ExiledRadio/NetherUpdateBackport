package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.hoglin.EntityHoglin;
import com.exiledradio.netherupdatebackport.hoglin.EntityZoglin;
import com.exiledradio.netherupdatebackport.hoglin.Flinging;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;

import java.util.List;
import java.util.Random;

/** Stream H: hoglins and zoglins. */
public final class HoglinSelfTest implements SelfTest {

    @Override
    public String name() {
        return "hoglins";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(EntityList.isRegistered(Content.id("hoglin")), "hoglins are registered");
        c.ok(EntityList.isRegistered(Content.id("zoglin")), "zoglins are registered");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("hoglin")) && EntityList.ENTITY_EGGS.containsKey(Content.id("zoglin")),
                "both have spawn eggs");

        BlockPos area = c.area();
        EntityHoglin hoglin = spawnHoglin(world, area);
        c.eq(40.0, hoglin.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "hoglin health 40");
        c.eq(0.3, Math.round(hoglin.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() * 1000) / 1000.0, "speed 0.3");
        c.eq(0.6, hoglin.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getBaseValue(), "knockback resistance 0.6");
        c.eq(6.0, hoglin.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "attack 6");
        if (Owners.ours("crimson_fungus") != null) {
            c.ok(hoglin.isBreedingItem(new ItemStack(Owners.ours("crimson_fungus"))), "bred with crimson fungus");
        }
        c.ok(!hoglin.isBreedingItem(new ItemStack(Items.WHEAT)), "not wheat");
        EntityHoglin baby = spawnHoglin(world, area.add(3, 0, 0));
        baby.setGrowingAge(-24000);
        c.ok(baby.isChild(), "a baby hoglin");
        c.eq(0.5, baby.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "bites for 0.5");

        // The fling ---------------------------------------------------------------------------------
        EntityPig pig = new EntityPig(world);
        pig.setPosition(area.getX() + 2.5, area.getY(), area.getZ() + 0.5);
        world.spawnEntity(pig);
        float before = pig.getHealth();
        pig.motionX = pig.motionY = pig.motionZ = 0;
        c.ok(hoglin.attackEntityAsMob(pig), "an adult hoglin's hit lands");
        float lost = before - pig.getHealth();
        c.ok(lost >= 3.0F && lost <= 8.0F, "for 3 to 8 (6 / 2 + up to 5): " + lost);
        c.ok(pig.motionY > 0 || Math.abs(pig.motionX) + Math.abs(pig.motionZ) > 0, "and throws the pig");
        int min = 99;
        int max = 0;
        for (int i = 0; i < 300; i++) {
            EntityPig p = new EntityPig(world);
            p.setPosition(area.getX() + 2.5, area.getY(), area.getZ() + 2.5);
            p.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(100);
            p.setHealth(100);
            Flinging.hurtAndThrow(hoglin, p, true);
            int d = Math.round(100 - p.getHealth());
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        c.ok(min == 3 && max == 8, "damage over 300 hits ranges 3-8: " + min + "-" + max);
        pig.setDead();

        // Zombification ---------------------------------------------------------------------------------
        c.ok(hoglin.isConverting(), "in the overworld a hoglin is turning into a zoglin");
        EntityHoglin immune = spawnHoglin(world, area.add(6, 0, 0));
        immune.setImmuneToZombification(true);
        c.ok(!immune.isConverting(), "unless immune");
        for (int i = 0; i < 305 && !hoglin.isDead; i++) {
            hoglin.onUpdate();
        }
        c.ok(hoglin.isDead, "after 15 seconds the hoglin is gone");
        List<EntityZoglin> zoglins = world.getEntitiesWithinAABB(EntityZoglin.class, new AxisAlignedBB(area).grow(4));
        c.eq(1, zoglins.size(), "and a zoglin stands in its place");
        if (!zoglins.isEmpty()) {
            EntityZoglin zoglin = zoglins.get(0);
            c.ok(zoglin.isPotionActive(MobEffects.NAUSEA), "with Nausea");
            c.eq(EnumCreatureAttribute.UNDEAD, zoglin.getCreatureAttribute(), "zoglins are undead");
            c.ok(zoglin.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue() == 40.0, "40 health");
            zoglin.setDead();
        }
        WorldServer nether = world.getMinecraftServer().getWorld(-1);
        BlockPos nPos = new BlockPos(3000, 100, 3000);
        nether.getChunkProvider().provideChunk(nPos.getX() >> 4, nPos.getZ() >> 4);
        EntityHoglin native_ = new EntityHoglin(nether);
        native_.setPosition(nPos.getX(), nPos.getY(), nPos.getZ());
        c.ok(!native_.isConverting(), "in the nether it does not convert");
        NBTTagCompound tag = new NBTTagCompound();
        immune.writeEntityToNBT(tag);
        EntityHoglin copy = new EntityHoglin(world);
        copy.readEntityFromNBT(tag);
        c.ok(copy.isImmuneToZombification(), "immunity is saved");

        // Repellents -----------------------------------------------------------------------------------
        BlockPos rep = c.area();
        // An earlier version of this test left a portal just outside the cleared area; the world is kept between runs.
        world.setBlockState(rep.add(-6, 2, 0), Blocks.AIR.getDefaultState(), 2 | 16);
        EntityHoglin calm = spawnHoglin(world, rep);
        c.ok(!calm.isPacified(), "no repellent, not pacified");
        if (Owners.ourBlock("warped_fungus") != null) {
            world.setBlockState(rep.add(5, 0, 0), Owners.ourBlock("warped_fungus").getDefaultState(), 2);
            EntityHoglin near = spawnHoglin(world, rep.add(0, 0, 2));
            c.eq(rep.add(5, 0, 0), near.nearestRepellent(), "a warped fungus 5 blocks off repels it");
            c.ok(near.isPacified(), "and pacifies it");
        }
        world.setBlockToAir(rep.add(5, 0, 0));
        world.setBlockState(rep.add(12, 2, 0), Blocks.PORTAL.getDefaultState(), 2 | 16);
        c.eq(rep.add(12, 2, 0), EntityHoglin.findRepellent(world, rep.add(6, 0, 4)), "a nether portal repels too");
        c.ok(EntityHoglin.findRepellent(world, rep.add(12, 0, 25)) == null, "nothing repels from 25 blocks away");
        world.setBlockState(rep.add(12, 2, 0), Blocks.AIR.getDefaultState(), 2 | 16);

        // Zoglin targets ---------------------------------------------------------------------------------
        EntityZoglin z = new EntityZoglin(world);
        c.ok(!zIs(z, new EntityCreeper(world)), "zoglins leave creepers alone");
        c.ok(!zIs(z, new EntityZoglin(world)), "and other zoglins");
        c.ok(zIs(z, new EntityPig(world)), "but attack pigs");
        c.ok(zIs(z, new EntityHoglin(world)), "and hoglins");

        // Loot -------------------------------------------------------------------------------------------
        EntityHoglin loot = spawnHoglin(world, c.area());
        int pork = 0;
        int cooked = 0;
        for (int i = 0; i < 50; i++) {
            for (ItemStack s : world.getLootTableManager().getLootTableFromLocation(EntityHoglin.LOOT)
                    .generateLootForPools(new Random(i), new LootContext.Builder(world).withLootedEntity(loot).build())) {
                pork += s.getItem() == Items.PORKCHOP ? s.getCount() : 0;
            }
        }
        c.ok(pork >= 100 && pork <= 200, "50 hoglins drop 2-4 porkchops each: " + pork);
        loot.setFire(10);
        for (ItemStack s : world.getLootTableManager().getLootTableFromLocation(EntityHoglin.LOOT)
                .generateLootForPools(new Random(1), new LootContext.Builder(world).withLootedEntity(loot).build())) {
            cooked += s.getItem() == Items.COOKED_PORKCHOP ? s.getCount() : 0;
        }
        c.ok(cooked >= 2, "a burning one drops cooked porkchops: " + cooked);
        loot.setDead();
        int flesh = 0;
        for (ItemStack s : world.getLootTableManager().getLootTableFromLocation(EntityZoglin.LOOT)
                .generateLootForPools(new Random(3), new LootContext.Builder(world).withLootedEntity(z).build())) {
            flesh += s.getItem() == Items.ROTTEN_FLESH ? s.getCount() : 0;
        }
        c.ok(flesh >= 1 && flesh <= 3, "zoglins drop rotten flesh: " + flesh);
        immune.setDead();
        baby.setDead();
        calm.setDead();
    }

    private static boolean zIs(EntityZoglin z, net.minecraft.entity.EntityLivingBase e) {
        try {
            java.lang.reflect.Method m = EntityZoglin.class.getDeclaredMethod("isTargetable", net.minecraft.entity.EntityLivingBase.class);
            m.setAccessible(true);
            return (Boolean) m.invoke(null, e);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static EntityHoglin spawnHoglin(WorldServer world, BlockPos at) {
        EntityHoglin h = new EntityHoglin(world);
        h.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        world.spawnEntity(h);
        return h;
    }
}

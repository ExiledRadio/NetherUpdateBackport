package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.strider.EntityStrider;
import com.exiledradio.netherupdatebackport.strider.StriderContent;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;

import java.util.Random;

/** Stream R: striders. */
public final class StriderSelfTest implements SelfTest {

    @Override
    public String name() {
        return "striders";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(EntityList.isRegistered(Content.id("strider")), "striders are registered");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("strider")), "with a spawn egg");
        c.ok(StriderContent.stick != null && StriderContent.stick.getMaxDamage() == 100, "the warped fungus on a stick lasts 100 uses");

        // A lava pool: 9x9, 4 deep, stone under it --------------------------------------------------------
        BlockPos area = c.area();
        BlockPos top = area.add(0, 3, 0);
        c.fill(area.add(-4, 0, -4), area.add(4, 3, 4), Blocks.LAVA.getDefaultState());
        EntityStrider strider = spawn(world, top.getX() + 0.5, top.getY() + 2.0, top.getZ() + 0.5);
        c.eq(20.0, strider.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "20 health");
        c.eq(0.175, Math.round(strider.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() * 1000) / 1000.0, "speed 0.175");
        c.ok(strider.isImmuneToFire(), "lava and fire never hurt it");
        for (int i = 0; i < 60; i++) {
            strider.onUpdate();
        }
        c.ok(!strider.isDead && strider.getHealth() == 20.0F, "after 3 seconds on lava it is unharmed");
        c.ok(Math.abs(strider.posY - (top.getY() + 0.5)) < 0.1, "it stands on the lava's surface: y " + strider.posY + " (surface " + (top.getY() + 0.5) + ")");
        c.ok(strider.onGround, "as on the ground");
        c.ok(!strider.isCold(), "and is warm");
        c.ok(!strider.isInLava(), "standing on lava is not swimming in it");

        EntityStrider deep = spawn(world, area.getX() + 2.5, area.getY() + 0.1, area.getZ() + 2.5);
        double start = deep.posY;
        for (int i = 0; i < 100; i++) {
            deep.onUpdate();
        }
        c.ok(deep.posY > start + 2.0, "one under the lava floats up: from y " + start + " to " + deep.posY);
        deep.setDead();

        // Cold, water --------------------------------------------------------------------------------
        BlockPos dry = c.area();
        EntityStrider cold = spawn(world, dry.getX() + 0.5, dry.getY(), dry.getZ() + 0.5);
        double base = cold.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        cold.onUpdate();
        c.ok(cold.isCold(), "on stone a strider is cold");
        double slow = cold.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
        c.ok(Math.abs(slow - base * 0.66) < 1e-6, "and moves at 0.66 of its speed: " + slow + " of " + base);
        world.setBlockState(dry.add(0, 0, 0), Blocks.WATER.getDefaultState(), 2 | 16);
        cold.setPosition(dry.getX() + 0.5, dry.getY(), dry.getZ() + 0.5);
        float before = cold.getHealth();
        cold.onUpdate();
        cold.hurtResistantTime = 0;
        cold.onUpdate();
        c.ok(cold.getHealth() < before, "water hurts it: " + before + " -> " + cold.getHealth());
        world.setBlockToAir(dry);
        cold.setDead();

        // Saddle, riding, boosting -----------------------------------------------------------------------
        EntityPlayerMP player = Players.real(world, "strider");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        player.setPosition(strider.posX, strider.posY, strider.posZ);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        strider.processInteract(player, EnumHand.MAIN_HAND);
        c.ok(strider.isSaddled(), "a saddle goes on");
        c.ok(player.getHeldItem(EnumHand.MAIN_HAND).isEmpty(), "using it up");
        NBTTagCompound tag = new NBTTagCompound();
        strider.writeEntityToNBT(tag);
        c.ok(tag.getBoolean("Saddle"), "the saddle is saved");
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        strider.processInteract(player, EnumHand.MAIN_HAND);
        c.eq(strider, player.getRidingEntity(), "an empty hand mounts a saddled strider");
        c.ok(!strider.canBeSteered(), "without the stick it does not steer");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(StriderContent.stick));
        c.ok(strider.canBeSteered(), "with the stick it does");
        ItemStack stick = player.getHeldItem(EnumHand.MAIN_HAND);
        StriderContent.stick.onItemRightClick(world, player, EnumHand.MAIN_HAND);
        c.eq(1, stick.getItemDamage(), "a boost costs one use");
        c.ok(!strider.boost(), "and a second boost waits for the first to end");
        player.dismountRidingEntity();
        if (Owners.ours("warped_fungus") != null) {
            c.ok(strider.isBreedingItem(new ItemStack(Owners.ours("warped_fungus"))), "bred with warped fungus");
        }

        // Spawning ------------------------------------------------------------------------------------
        EntitySpawnPlacementRegistry.getPlacementForEntity(EntityStrider.class);
        c.ok(EntitySpawnPlacementRegistry.getPlacementForEntity(EntityStrider.class).canSpawnAt(world, top),
                "spawn placement accepts a lava block");
        c.ok(!EntitySpawnPlacementRegistry.getPlacementForEntity(EntityStrider.class).canSpawnAt(world, top.up(3)),
                "and refuses air");
        EntityStrider probe = new EntityStrider(world);
        probe.setPosition(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
        c.ok(probe.getCanSpawnHere(), "lava with air above is a spawn spot");
        world.setBlockState(top.up(), Blocks.STONE.getDefaultState(), 2 | 16);
        c.ok(!probe.getCanSpawnHere(), "lava under stone is not");
        world.setBlockToAir(top.up());
        int jockeys = 0;
        int babies = 0;
        BlockPos field = c.area();
        for (int i = 0; i < 300; i++) {
            EntityStrider s = new EntityStrider(world);
            s.setPosition(field.getX() + 0.5 + (i % 15) * 2, field.getY(), field.getZ() + 0.5 + (i / 15));
            world.spawnEntity(s);
            s.onInitialSpawn(world.getDifficultyForLocation(field), null);
            if (!s.getPassengers().isEmpty() && s.getPassengers().get(0) instanceof EntityPigZombie) {
                jockeys++;
                EntityPigZombie rider = (EntityPigZombie) s.getPassengers().get(0);
                c.ok(rider.getHeldItemMainhand().getItem() == StriderContent.stick, "a zombified piglin rider holds the stick");
                c.ok(s.isSaddled(), "on a saddled strider");
            } else if (!s.getPassengers().isEmpty() && s.getPassengers().get(0) instanceof EntityStrider) {
                babies++;
            }
        }
        c.ok(jockeys >= 2 && jockeys <= 25, "about 1 in 30 spawns with a zombified piglin: " + jockeys + "/300");
        c.ok(babies >= 15 && babies <= 50, "about 1 in 10 of the rest carries a baby: " + babies + "/300");
        for (net.minecraft.entity.Entity e : world.getEntitiesWithinAABB(net.minecraft.entity.Entity.class, new AxisAlignedBB(field).grow(40, 10, 40))) {
            if (!(e instanceof EntityPlayerMP)) {
                e.setDead();
            }
        }

        // Loot, recipe, advancements -------------------------------------------------------------------
        int string = 0;
        for (ItemStack s : world.getLootTableManager().getLootTableFromLocation(EntityStrider.LOOT)
                .generateLootForPools(new Random(5), new LootContext.Builder(world).withLootedEntity(strider).build())) {
            string += s.getItem() == Items.STRING ? s.getCount() : 0;
        }
        c.ok(string >= 2 && string <= 5, "a strider drops 2-5 string: " + string);
        Recipes.checkAll(c);
        for (String a : new String[] {"nether/ride_strider", "nether/ride_strider_in_overworld_lava"}) {
            c.ok(world.getAdvancementManager().getAdvancement(new ResourceLocation("netherupdatebackport", a)) != null, "advancement " + a + " loaded");
        }
        strider.setDead();
    }

    private static EntityStrider spawn(WorldServer world, double x, double y, double z) {
        EntityStrider s = new EntityStrider(world);
        s.setPosition(x, y, z);
        world.spawnEntity(s);
        return s;
    }
}

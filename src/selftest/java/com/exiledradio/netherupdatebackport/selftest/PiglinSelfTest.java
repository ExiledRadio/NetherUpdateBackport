package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.common.Content;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.common.Triggers;
import com.exiledradio.netherupdatebackport.hoglin.EntityHoglin;
import com.exiledradio.netherupdatebackport.owned.ItemCrossbow;
import com.exiledradio.netherupdatebackport.owned.OwnedContent;
import com.exiledradio.netherupdatebackport.piglin.AbstractPiglin;
import com.exiledradio.netherupdatebackport.piglin.Bartering;
import com.exiledradio.netherupdatebackport.piglin.BastionMobs;
import com.exiledradio.netherupdatebackport.piglin.Crossbows;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglin;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglinBrute;
import com.exiledradio.netherupdatebackport.piglin.Memories.Mem;
import com.exiledradio.netherupdatebackport.piglin.PiglinContent;
import com.exiledradio.netherupdatebackport.piglin.PiglinTasks;

import net.minecraft.advancements.Advancement;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Stream P: piglins, piglin brutes, bartering, the Snout pattern, and the crossbow copy they carry. */
public final class PiglinSelfTest implements SelfTest {

    @Override
    public String name() {
        return "piglins";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        registration(c);
        items(c);
        spawning(c, world);
        bartering(c, world);
        admiring(c, world);
        anger(c, world);
        sensesAndFights(c, world);
        hoglins(c, world);
        brutes(c, world);
        zombification(c, world);
        saving(c, world);
        advancements(c, world);
    }

    // ---------------------------------------------------------------------------------------------

    private static void registration(Check c) {
        c.ok(net.minecraft.entity.EntityList.isRegistered(Content.id("piglin")), "piglins are registered");
        c.ok(net.minecraft.entity.EntityList.isRegistered(Content.id("piglin_brute")), "piglin brutes are registered");
        c.ok(net.minecraft.entity.EntityList.ENTITY_EGGS.containsKey(Content.id("piglin"))
                && net.minecraft.entity.EntityList.ENTITY_EGGS.containsKey(Content.id("piglin_brute")), "both have spawn eggs");
        c.ok(PiglinContent.snoutPattern != null, "the Snout banner pattern item exists");
        if (PiglinContent.snoutPattern != null) {
            ItemStack s = new ItemStack(PiglinContent.snoutPattern);
            c.eq(1, s.getMaxStackSize(), "it stacks to one");
            c.eq(EnumRarity.UNCOMMON, s.getRarity(), "uncommon");
        }
        // The crossbow copy (no Village & Pillage in the test game).
        c.ok(OwnedContent.crossbowCopy(), "this game has no other crossbow, so the copy is registered");
        c.ok(OwnedContent.crossbow != null && Owners.ours("crossbow") == OwnedContent.crossbow, "the crossbow copy exists");
        c.eq(OwnedContent.crossbow, Crossbows.item(), "and is the crossbow piglins carry");
        if (OwnedContent.crossbow != null) {
            c.eq(465, new ItemStack(OwnedContent.crossbow).getMaxDamage(), "465 durability, as the owner's");
        }
        for (String e : new String[]{"quick_charge", "multishot", "piercing"}) {
            c.ok(ForgeRegistries.ENCHANTMENTS.getValue(Content.id(e)) != null, "enchantment " + e + " is registered");
        }
        c.ok(net.minecraft.entity.EntityList.isRegistered(Content.id("crossbow_arrow"))
                && net.minecraft.entity.EntityList.isRegistered(Content.id("crossbow_spectral_arrow"))
                && net.minecraft.entity.EntityList.isRegistered(Content.id("crossbow_firework")), "its three projectiles are registered");
        c.ok(net.minecraft.item.crafting.CraftingManager.REGISTRY.containsKey(Content.id("crossbow")), "and its recipe");

        boolean hell = false;
        for (Biome.SpawnListEntry e : Biomes.HELL.getSpawnableList(EnumCreatureType.MONSTER)) {
            if (e.entityClass == EntityPiglin.class) {
                hell = e.itemWeight == 15 && e.minGroupCount == 4 && e.maxGroupCount == 4;
            }
        }
        c.ok(hell, "piglins spawn in the nether wastes (Hell): weight 15, groups of 4");
    }

    private static void items(Check c) {
        Item[] loved = {Items.GOLD_INGOT, Items.GOLDEN_APPLE, Items.CLOCK, Items.GOLDEN_HELMET, Items.GOLDEN_SWORD,
                Item.getItemFromBlock(Blocks.GOLD_BLOCK), Item.getItemFromBlock(Blocks.GOLD_ORE), Items.GOLDEN_HORSE_ARMOR};
        for (Item i : loved) {
            c.ok(PiglinTasks.isLovedItem(new ItemStack(i)), "piglins love " + i.getRegistryName());
        }
        c.ok(PiglinTasks.isLovedItem(new ItemStack(Items.GOLDEN_APPLE, 1, 1)), "and enchanted golden apples");
        if (Owners.ours("nether_gold_ore") != null) {
            c.ok(PiglinTasks.isLovedItem(new ItemStack(Owners.ours("nether_gold_ore"))), "and nether gold ore");
        }
        if (Owners.ours("gilded_blackstone") != null) {
            c.ok(PiglinTasks.isLovedItem(new ItemStack(Owners.ours("gilded_blackstone"))), "and gilded blackstone");
        }
        c.ok(!PiglinTasks.isLovedItem(new ItemStack(Items.IRON_INGOT)), "not iron");
        c.ok(!PiglinTasks.isLovedItem(new ItemStack(Items.GOLD_NUGGET)), "nor gold nuggets (kept, not admired)");
        c.ok(PiglinTasks.isBarterCurrency(new ItemStack(Items.GOLD_INGOT)), "gold ingots are the currency");
        c.ok(!PiglinTasks.isBarterCurrency(new ItemStack(Items.GOLDEN_APPLE)), "a golden apple is not");
        if (Owners.ours("soul_torch") != null) {
            c.ok(PiglinTasks.isRepellentItem(new ItemStack(Owners.ours("soul_torch"))), "a soul torch is repellent");
        }
        c.ok(PiglinTasks.isGuardedBlock(Blocks.GOLD_BLOCK.getDefaultState()), "gold blocks are guarded");
        c.ok(PiglinTasks.isGuardedBlock(Blocks.CHEST.getDefaultState()) && PiglinTasks.isGuardedBlock(Blocks.ENDER_CHEST.getDefaultState())
                && PiglinTasks.isGuardedBlock(Blocks.PURPLE_SHULKER_BOX.getDefaultState()), "and chests, ender chests, shulker boxes");
        c.ok(PiglinTasks.isGuardedBlock(Blocks.GOLD_ORE.getDefaultState()), "and gold ore");
        if (Owners.ourBlock("nether_gold_ore") != null) {
            c.ok(PiglinTasks.isGuardedBlock(Owners.ourBlock("nether_gold_ore").getDefaultState()), "and nether gold ore");
        }
        c.ok(!PiglinTasks.isGuardedBlock(Blocks.STONE.getDefaultState()), "not stone");

        ItemStack bow = new ItemStack(Crossbows.item());
        c.ok(Crossbows.isCrossbow(bow), "the crossbow is recognized by name");
        Crossbows.setCharged(bow, true);
        c.ok(ItemCrossbow.isCharged(bow), "a piglin's loaded mark is the copy's own Charged tag");
    }

    private static void spawning(Check c, WorldServer world) {
        BlockPos area = c.area();
        int babies = 0;
        int crossbows = 0;
        int swords = 0;
        int helmets = 0;
        int adults = 0;
        boolean babyArmed = false;
        for (int i = 0; i < 400; i++) {
            EntityPiglin p = new EntityPiglin(world);
            p.setPosition(area.getX() + 0.5, area.getY(), area.getZ() + 0.5);
            p.onInitialSpawn(world.getDifficultyForLocation(area), null);
            if (p.isChild()) {
                babies++;
                babyArmed |= !p.getHeldItemMainhand().isEmpty();
            } else {
                adults++;
                Item main = p.getHeldItemMainhand().getItem();
                crossbows += main == Crossbows.item() ? 1 : 0;
                swords += main == Items.GOLDEN_SWORD ? 1 : 0;
                helmets += p.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() == Items.GOLDEN_HELMET ? 1 : 0;
            }
        }
        c.ok(babies >= 50 && babies <= 115, "one in five spawns a baby: " + babies + "/400");
        c.ok(!babyArmed, "babies spawn unarmed");
        c.eq(adults, crossbows + swords, "every adult holds a crossbow or a golden sword");
        c.ok(crossbows > adults / 3 && swords > adults / 3, "about half each: " + crossbows + " crossbows, " + swords + " swords");
        c.ok(helmets >= adults / 25 && helmets <= adults / 5, "a golden helmet one time in ten: " + helmets + "/" + adults);

        EntityPiglin p = new EntityPiglin(world);
        p.onInitialSpawn(world.getDifficultyForLocation(area), null);
        c.ok(p.memories.has(Mem.HUNTED_RECENTLY), "a new piglin waits 30-120 s before its first hunt");
        c.eq(16.0, p.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "piglin health 16");
        c.eq(0.35, Math.round(p.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() * 1000) / 1000.0, "speed 0.35");
        c.eq(5.0, p.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "attack 5");
        EntityPiglin baby = new EntityPiglin(world);
        baby.setChild(true);
        c.ok(Math.abs(baby.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() - 0.42) < 0.001,
                "a baby is 20% faster: " + baby.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
        c.ok(Math.abs(baby.height - 0.975F) < 0.001 && Math.abs(baby.getEyeHeight() - 0.93F) < 0.001, "and half the size");

        EntityPiglin probe = new EntityPiglin(world);
        probe.setPosition(area.getX() + 0.5, area.getY(), area.getZ() + 0.5);
        c.ok(probe.getCanSpawnHere(), "piglins may spawn on stone in any light");
        world.setBlockState(area.down(), Blocks.NETHER_WART_BLOCK.getDefaultState(), 2 | 16);
        c.ok(!probe.getCanSpawnHere(), "but not on nether wart blocks");
        world.setBlockState(area.down(), Blocks.STONE.getDefaultState(), 2 | 16);

        // Bastion mobs for stream X.
        EntityLiving cross = BastionMobs.spawn(world, area.add(2, 0, 2), 0.0F, "crossbow_piglin");
        EntityLiving sword = BastionMobs.spawn(world, area.add(4, 0, 2), 0.0F, "sword_piglin");
        EntityLiving brute = BastionMobs.spawn(world, area.add(6, 0, 2), 0.0F, "melee_piglin");
        EntityLiving brute2 = BastionMobs.spawn(world, area.add(8, 0, 2), 0.0F, "melee_piglin_always");
        c.ok(cross instanceof EntityPiglin && Crossbows.isCrossbow(cross.getHeldItemMainhand()), "a bastion crossbow piglin");
        c.ok(sword instanceof EntityPiglin && sword.getHeldItemMainhand().getItem() == Items.GOLDEN_SWORD, "a bastion sword piglin");
        c.ok(brute instanceof EntityPiglinBrute && brute2 instanceof EntityPiglinBrute, "melee_piglin and melee_piglin_always are brutes");
        c.ok(brute != null && brute.getHeldItemMainhand().getItem() == Items.GOLDEN_AXE, "with a golden axe");
        c.eq(area.add(6, 0, 2), brute instanceof EntityPiglinBrute ? ((EntityPiglinBrute) brute).home() : null, "and its home where it stood");
        c.ok(cross != null && cross.isNoDespawnRequired() && brute != null && brute.isNoDespawnRequired(), "bastion mobs never despawn");
        NBTTagCompound tag = new NBTTagCompound();
        if (cross != null) {
            cross.writeEntityToNBT(tag);
        }
        c.ok(tag.getBoolean("CannotHunt"), "bastion piglins do not hunt");
        int bastionBabies = 0;
        for (int i = 0; i < 40; i++) {
            EntityLiving m = BastionMobs.spawn(world, area.add(10, 0, 10), 0.0F, "sword_piglin");
            bastionBabies += m != null && m.isChild() ? 1 : 0;
            if (m != null) {
                m.setDead();
            }
        }
        c.eq(0, bastionBabies, "a structure spawn is never a baby");
        c.ok(BastionMobs.spawn(world, area, 0.0F, "nonsense") == null, "an unknown template spawns nothing");
        for (EntityLiving m : new EntityLiving[]{cross, sword, brute, brute2}) {
            if (m != null) {
                m.setDead();
            }
        }
    }

    private static void bartering(Check c, WorldServer world) {
        EntityPiglin p = new EntityPiglin(world);
        BlockPos area = c.area();
        p.setPosition(area.getX() + 0.5, area.getY(), area.getZ() + 0.5);
        int rolls = 4590;
        Map<String, Integer> kinds = new HashMap<String, Integer>();
        boolean counts = true;
        boolean soulSpeed = true;
        boolean potions = true;
        Enchantment soul = ForgeRegistries.ENCHANTMENTS.getValue(Content.id("soul_speed"));
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> out = Bartering.roll(p);
            if (out.size() != 1) {
                counts = false;
                continue;
            }
            ItemStack s = out.get(0);
            String key = s.getItem().getRegistryName().toString();
            if (s.getItem() == Items.POTIONITEM) {
                key += PotionUtils.getPotionFromItem(s) == PotionTypes.WATER ? "/water" : "/fire";
                potions &= PotionUtils.getPotionFromItem(s) == PotionTypes.WATER || PotionUtils.getPotionFromItem(s) == PotionTypes.FIRE_RESISTANCE;
            }
            if (s.getItem() == Items.SPLASH_POTION) {
                potions &= PotionUtils.getPotionFromItem(s) == PotionTypes.FIRE_RESISTANCE;
            }
            kinds.merge(key, s.getCount(), Integer::sum);
            kinds.merge(key + "#", 1, Integer::sum);
            int n = s.getCount();
            if (s.getItem() == Items.ENDER_PEARL) {
                counts &= n >= 2 && n <= 4;
            } else if (s.getItem() == Items.IRON_NUGGET) {
                counts &= n >= 10 && n <= 36;
            } else if (s.getItem() == Items.SPECTRAL_ARROW) {
                counts &= n >= 6 && n <= 12;
            } else if (s.getItem() == Owners.ours("blackstone")) {
                counts &= n >= 8 && n <= 16;
            } else if (s.getItem() == Owners.ours("crying_obsidian")) {
                counts &= n >= 1 && n <= 3;
            }
            if (soul != null && (s.getItem() == Items.ENCHANTED_BOOK || s.getItem() == Items.IRON_BOOTS)) {
                int level = s.getItem() == Items.IRON_BOOTS ? EnchantmentHelper.getEnchantmentLevel(soul, s)
                        : EnchantmentHelper.getEnchantments(s).getOrDefault(soul, 0);
                soulSpeed &= level >= 1 && level <= 3;
            }
        }
        com.exiledradio.netherupdatebackport.NetherUpdateBackport.LOGGER.info("SELFTEST barter tally: {}", kinds);
        c.ok(counts, "every barter gives one stack, ender pearls 2-4, iron nuggets 10-36, spectral arrows 6-12, blackstone 8-16");
        c.ok(soulSpeed, "books and iron boots carry Soul Speed I-III");
        c.ok(potions, "potions are Fire Resistance, or water bottles");
        String[] all = {"minecraft:enchanted_book", "minecraft:iron_boots", "minecraft:potion/fire", "minecraft:splash_potion",
                "minecraft:potion/water", "minecraft:iron_nugget", "minecraft:ender_pearl", "minecraft:string", "minecraft:quartz",
                "minecraft:obsidian", "netherupdatebackport:crying_obsidian", "minecraft:fire_charge", "minecraft:leather",
                "minecraft:soul_sand", "minecraft:netherbrick", "minecraft:spectral_arrow", "minecraft:gravel",
                "netherupdatebackport:blackstone"};
        int[] weights = {5, 8, 8, 8, 10, 10, 10, 20, 20, 40, 40, 40, 40, 40, 40, 40, 40, 40};
        for (int i = 0; i < all.length; i++) {
            int got = kinds.getOrDefault(all[i] + "#", 0);
            double expected = rolls * weights[i] / 459.0;
            c.ok(Math.abs(got - expected) < Math.max(4.5 * Math.sqrt(expected), 6), "barter " + all[i] + ": " + got + " of ~" + Math.round(expected));
        }
    }

    private static void admiring(Check c, WorldServer world) {
        BlockPos area = c.area();
        EntityPiglin p = spawnPiglin(world, area.add(5, 0, 5), false);
        p.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        c.ok(p.wantsToPickUp(new ItemStack(Items.GOLD_INGOT)), "an adult wants gold ingots");
        c.ok(p.wantsToPickUp(new ItemStack(Items.PORKCHOP)), "and porkchops");
        if (Owners.ours("soul_torch") != null) {
            c.ok(!p.wantsToPickUp(new ItemStack(Owners.ours("soul_torch"))), "never soul torches");
        }
        c.ok(!p.wantsToPickUp(new ItemStack(Items.WOODEN_SWORD)), "nor a wooden sword over its golden one");

        // Picking up thrown gold, admiring it for 6 s, bartering it.
        EntityItem gold = new EntityItem(world, p.posX, p.posY, p.posZ, new ItemStack(Items.GOLD_INGOT, 1));
        gold.setNoPickupDelay();
        world.spawnEntity(gold);
        tick(p, 2);
        c.eq(Items.GOLD_INGOT, p.getHeldItemOffhand().getItem(), "it picks up a gold ingot into its off hand");
        c.eq(1, p.getHeldItemOffhand().getCount(), "one at a time");
        c.ok(p.memories.has(Mem.ADMIRING_ITEM), "and admires it");
        c.eq(PiglinTasks.Activity.ADMIRE_ITEM, p.activity(), "the admire activity");
        int ticks = 0;
        while (!p.getHeldItemOffhand().isEmpty() && ticks < 200) {
            tick(p, 1);
            ticks++;
        }
        c.ok(p.getHeldItemOffhand().isEmpty() || p.getHeldItemOffhand().getItem() == Items.GOLD_INGOT,
                "the off hand is let go after admiring");
        c.ok(ticks >= 110 && ticks <= 130, "about 6 seconds of admiring: " + ticks + " ticks");
        List<EntityItem> thrown = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(area).grow(16, 8, 16),
                e -> e.getItem().getItem() != Items.GOLD_INGOT);
        c.ok(!thrown.isEmpty(), "and it throws back a barter: " + (thrown.isEmpty() ? "-" : thrown.get(0).getItem()));
        gold.setDead();
        for (EntityItem e : thrown) {
            e.setDead();
        }

        // Handed gold by a player.
        EntityPiglin q = spawnPiglin(world, area.add(10, 0, 10), false);
        FakePlayer player = c.player("piglin", area.add(11, 0, 10));
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT, 2));
        c.ok(q.processInitialInteract(player, EnumHand.MAIN_HAND), "an adult takes a gold ingot handed to it");
        c.eq(1, player.getHeldItemMainhand().getCount(), "one");
        c.eq(Items.GOLD_INGOT, q.getHeldItemOffhand().getItem(), "and admires it");
        EntityPiglin baby = spawnPiglin(world, area.add(14, 0, 10), false);
        baby.setChild(true);
        c.ok(!baby.processInitialInteract(player, EnumHand.MAIN_HAND), "a baby does not take gold for bartering");

        // Food and nuggets.
        EntityPiglin r = spawnPiglin(world, area.add(20, 0, 20), false);
        r.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        EntityItem nuggets = new EntityItem(world, r.posX, r.posY, r.posZ, new ItemStack(Items.GOLD_NUGGET, 5));
        nuggets.setNoPickupDelay();
        world.spawnEntity(nuggets);
        tick(r, 2);
        c.ok(nuggets.isDead, "gold nuggets are picked up whole");
        c.ok(r.inventory().getStackInSlot(0).getItem() == Items.GOLD_NUGGET && r.inventory().getStackInSlot(0).getCount() == 5,
                "into its inventory");
        EntityItem pork = new EntityItem(world, r.posX, r.posY, r.posZ, new ItemStack(Items.PORKCHOP));
        pork.setNoPickupDelay();
        world.spawnEntity(pork);
        tick(r, 2);
        c.ok(pork.isDead && r.memories.has(Mem.ATE_RECENTLY), "a porkchop is eaten");
        c.ok(!r.wantsToPickUp(new ItemStack(Items.PORKCHOP)), "and no more for 10 s");

        // A loved item in sight starts admiring and walks to it.
        EntityPiglin s = spawnPiglin(world, area.add(3, 0, 25), false);
        EntityItem far = new EntityItem(world, s.posX + 4, s.posY, s.posZ, new ItemStack(Items.GOLDEN_APPLE));
        far.setNoPickupDelay();
        world.spawnEntity(far);
        boolean seen = false;
        boolean admiring = false;
        for (int i = 0; i < 25; i++) {
            tick(s, 1);
            seen |= s.memories.get(Mem.NEAREST_VISIBLE_WANTED_ITEM) == far;
            admiring |= s.memories.has(Mem.ADMIRING_ITEM);
        }
        c.ok(seen, "it sees the golden apple 4 blocks off");
        c.ok(admiring, "and goes to admire it");
        tick(s, 80);
        c.ok(far.isDead || s.getHeldItemOffhand().getItem() == Items.GOLDEN_APPLE || s.getHeldItemMainhand().getItem() == Items.GOLDEN_APPLE
                || PiglinTasks.isLovedItem(s.inventory().getStackInSlot(0)), "walking over and picking it up");
        for (Entity e : new Entity[]{p, q, baby, r, s, far, pork, nuggets}) {
            e.setDead();
        }
    }

    private static void anger(Check c, WorldServer world) {
        BlockPos area = c.area();
        EntityPiglin near = spawnPiglin(world, area.add(4, 0, 4), false);
        EntityPiglin far = spawnPiglin(world, area.add(4, 0, 30), false);
        far.setPosition(far.posX, far.posY, area.getZ() + 31.5);
        FakePlayer player = c.player("piglin-anger", area.add(8, 0, 4));
        MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, area.add(9, 0, 4), Blocks.STONE.getDefaultState(), player));
        c.ok(!near.memories.has(Mem.ANGRY_AT), "breaking stone angers nobody");
        MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, area.add(9, 0, 4), Blocks.GOLD_BLOCK.getDefaultState(), player));
        c.eq(player.getUniqueID(), near.memories.get(Mem.ANGRY_AT), "breaking a gold block angers a piglin nearby");
        c.ok(near.memories.timeLeft(Mem.ANGRY_AT) == 600, "for 30 s");
        c.ok(!far.memories.has(Mem.ANGRY_AT), "but not one 27 blocks off");
        near.memories.erase(Mem.ANGRY_AT);

        world.setBlockState(area.add(8, 0, 6), Blocks.CHEST.getDefaultState());
        TileEntityChest chest = (TileEntityChest) world.getTileEntity(area.add(8, 0, 6));
        ContainerChest open = new ContainerChest(player.inventory, chest, player);
        c.ok(PiglinContent.isGuardedContainer(open), "a chest's screen is guarded");
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, open));
        c.eq(player.getUniqueID(), near.memories.get(Mem.ANGRY_AT), "opening a chest in sight of a piglin angers it");
        chest.closeInventory(player);

        // Hurt.
        EntityPiglin adult = spawnPiglin(world, area.add(14, 0, 14), false);
        EntityPiglin friend = spawnPiglin(world, area.add(16, 0, 14), false);
        EntityPiglin child = spawnPiglin(world, area.add(14, 0, 18), false);
        child.setChild(true);
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(area.getX() + 14.5, area.getY(), area.getZ() + 16.5);
        world.spawnEntity(zombie);
        tick(adult, 21);
        tick(friend, 21);
        tick(child, 21);
        adult.attackEntityFrom(DamageSource.causeMobDamage(zombie), 1.0F);
        c.eq(zombie.getUniqueID(), adult.memories.get(Mem.ANGRY_AT), "hurt, an adult is angry at its attacker");
        c.eq(zombie.getUniqueID(), friend.memories.get(Mem.ANGRY_AT), "and so are the adult piglins about");
        child.attackEntityFrom(DamageSource.causeMobDamage(zombie), 1.0F);
        c.eq(zombie, child.memories.get(Mem.AVOID_TARGET), "a hurt baby runs from its attacker");
        adult.hurtResistantTime = 0;
        adult.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F);
        c.ok(adult.memories.has(Mem.ADMIRING_DISABLED), "hit by a player, it will not admire gold for 20 s");

        c.ok(!PiglinTasks.isWearingGold(player), "a player without gold armor");
        EntityPiglin judge = spawnPiglin(world, area.add(20, 0, 20), false);
        judge.memories.set(Mem.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, player);
        c.eq(player, PiglinTasks.findNearestValidAttackTarget(judge), "is a piglin's target");
        player.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
        c.ok(PiglinTasks.isWearingGold(player), "golden boots are enough gold armor");
        player.setItemStackToSlot(EntityEquipmentSlot.FEET, ItemStack.EMPTY);
        EntityWitherSkeleton nemesis = new EntityWitherSkeleton(world);
        judge.memories.set(Mem.NEAREST_VISIBLE_NEMESIS, nemesis);
        c.eq(nemesis, PiglinTasks.findNearestValidAttackTarget(judge), "a wither skeleton comes before the player");
        EntityPigZombie zp = new EntityPigZombie(world);
        zp.setPosition(judge.posX + 2, judge.posY, judge.posZ);
        judge.memories.set(Mem.NEAREST_VISIBLE_ZOMBIFIED, zp);
        c.ok(PiglinTasks.findNearestValidAttackTarget(judge) == null, "near a zombified piglin it picks no fight");
        for (Entity e : new Entity[]{near, far, adult, friend, child, zombie, judge}) {
            e.setDead();
        }
    }

    private static void sensesAndFights(Check c, WorldServer world) {
        BlockPos area = c.area();
        // A wither skeleton in view is attacked.
        EntityPiglin p = spawnPiglin(world, area.add(4, 0, 4), false);
        EntityWitherSkeleton ws = new EntityWitherSkeleton(world);
        ws.setPosition(area.getX() + 9.5, area.getY(), area.getZ() + 4.5);
        ws.setNoAI(true);
        world.spawnEntity(ws);
        tick(p, 22);
        c.eq(ws, p.memories.get(Mem.NEAREST_VISIBLE_NEMESIS), "it sees the wither skeleton as its nemesis");
        c.eq(ws, p.getAttackTarget(), "and fights it");
        c.eq(PiglinTasks.Activity.FIGHT, p.activity(), "the fight activity");
        c.ok(p.isAggressive(), "aggressive");
        ws.setDead();
        p.setDead();

        // A zombified piglin close by is avoided.
        EntityPiglin q = spawnPiglin(world, area.add(4, 0, 12), false);
        EntityPigZombie zp = new EntityPigZombie(world);
        zp.setPosition(area.getX() + 7.5, area.getY(), area.getZ() + 12.5);
        zp.setNoAI(true);
        world.spawnEntity(zp);
        tick(q, 23);
        c.eq(zp, q.memories.get(Mem.NEAREST_VISIBLE_ZOMBIFIED), "it sees the zombified piglin");
        c.eq(zp, q.memories.get(Mem.AVOID_TARGET), "and avoids it");
        c.eq(PiglinTasks.Activity.AVOID, q.activity(), "the avoid activity");
        // RunAwayTask picks its walk target on a tick of its own; give it a few.
        boolean walking = q.hasWalkTarget() || !q.getNavigator().noPath();
        for (int i = 0; i < 10 && !walking; i++) {
            tick(q, 1);
            walking = q.hasWalkTarget() || !q.getNavigator().noPath();
        }
        c.ok(walking, "walking away");
        zp.setDead();
        q.setDead();

        // Repellents.
        if (Owners.ourBlock("soul_lantern") != null) {
            EntityPiglin r = spawnPiglin(world, area.add(20, 0, 20), false);
        r.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
            world.setBlockState(area.add(25, 0, 20), Owners.ourBlock("soul_lantern").getDefaultState(), 2 | 16);
            // Read the memory when the sensor first sets it: once the piglin has run more than 8 blocks off, the
            // next scan rightly forgets it.
            for (int i = 0; i < 21 && r.memories.get(Mem.NEAREST_REPELLENT) == null; i++) {
                tick(r, 1);
            }
            c.eq(area.add(25, 0, 20), r.memories.get(Mem.NEAREST_REPELLENT), "a soul lantern 5 blocks off repels it");
            c.ok(PiglinTasks.findNearestRepellent(world, area.add(25, 0, 0)) == null, "nothing repels from 20 blocks");
            r.setDead();
        }

        // A golden sword piglin angry at a pig hits it.
        BlockPos field = c.area();
        EntityPiglin melee = spawnPiglin(world, field.add(4, 0, 4), false);
        melee.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        EntityPig pig = pig(world, field.add(5, 0, 4));
        PiglinTasks.setAngerTarget(melee, pig);
        tick(melee, 45);
        c.ok(pig.getHealth() < pig.getMaxHealth(), "a piglin angry at a pig hits it with its sword: " + pig.getHealth());
        melee.setDead();
        pig.setDead();

        // A crossbow piglin shoots.
        EntityPiglin archer = spawnPiglin(world, field.add(4, 0, 20), false);
        archer.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Crossbows.item()));
        EntityPig target = pig(world, field.add(10, 0, 20));
        target.setNoAI(true);
        PiglinTasks.setAngerTarget(archer, target);
        boolean charging = false;
        boolean loaded = false;
        int arrows = 0;
        for (int i = 0; i < 160 && arrows == 0; i++) {
            tick(archer, 1);
            charging |= archer.isChargingCrossbow();
            loaded |= Crossbows.isCharged(archer.getHeldItemMainhand());
            arrows = world.getEntitiesWithinAABB(EntityArrow.class, new AxisAlignedBB(field).grow(40, 16, 40)).size();
        }
        c.ok(charging, "a crossbow piglin charges its crossbow");
        c.ok(loaded, "loads it");
        c.ok(arrows > 0, "and shoots (" + archer.activity() + ", target " + archer.getAttackTarget() + ", dist "
                + archer.getDistance(target) + ", sees " + archer.visibleLiving().contains(target) + ", pos " + archer.getPositionVector() + ")");
        c.ok(!Crossbows.isCharged(archer.getHeldItemMainhand()), "unloading it");
        for (EntityArrow a : world.getEntitiesWithinAABB(EntityArrow.class, new AxisAlignedBB(field).grow(40, 16, 40))) {
            a.setDead();
        }
        archer.setDead();
        target.setDead();
    }

    private static void hoglins(Check c, WorldServer world) {
        if (!net.minecraft.entity.EntityList.isRegistered(Content.id("hoglin"))) {
            return;
        }
        BlockPos area = c.area();
        EntityPiglin hunter = spawnPiglin(world, area.add(4, 0, 4), false);
        EntityHoglin hoglin = hoglin(world, area.add(10, 0, 4));
        tick(hunter, 22);
        c.eq(hoglin, hunter.memories.get(Mem.NEAREST_VISIBLE_HUNTABLE_HOGLIN), "an adult hoglin is huntable");
        c.eq(hoglin.getUniqueID(), hunter.memories.get(Mem.ANGRY_AT), "a piglin that has not hunted lately goes after it");
        c.ok(hunter.memories.has(Mem.HUNTED_RECENTLY), "and will not start another hunt for a while");
        tick(hunter, 2);
        c.eq(hoglin, hunter.getAttackTarget(), "it fights the hoglin");
        hoglin.setHealth(0.0F);
        tick(hunter, 1);
        c.ok(hunter.memories.has(Mem.CELEBRATE_LOCATION), "the hoglin dead, it celebrates where it fell");
        c.ok(hunter.getAttackTarget() == null, "its fight over");
        tick(hunter, 1);
        c.eq(PiglinTasks.Activity.CELEBRATE, hunter.activity(), "the celebrate activity");
        hoglin.setDead();
        hunter.setDead();

        BlockPos stable = c.area();
        EntityPiglin calm = spawnPiglin(world, stable.add(4, 0, 4), false);
        EntityHoglin kept = hoglin(world, stable.add(10, 0, 4));
        kept.setCannotBeHunted(true);
        EntityHoglin baby = hoglin(world, stable.add(4, 0, 10));
        baby.setGrowingAge(-24000);
        tick(calm, 22);
        c.ok(calm.memories.get(Mem.NEAREST_VISIBLE_HUNTABLE_HOGLIN) == null, "a stable's hoglin is never hunted");
        c.eq(baby, calm.memories.get(Mem.NEAREST_VISIBLE_BABY_HOGLIN), "a baby hoglin is noticed (for babies to ride)");
        EntityPiglin rider = spawnPiglin(world, stable.add(5, 0, 10), false);
        rider.setChild(true);
        EntityPiglin rider2 = spawnPiglin(world, stable.add(5, 0, 11), false);
        rider2.setChild(true);
        rider.startRiding(baby);
        rider2.startRiding(baby);
        c.eq(rider, rider2.getRidingEntity(), "a second baby piglin climbs onto the first");
        rider.dismountRidingEntity();
        rider2.dismountRidingEntity();

        // Outnumbered: three hoglins to one piglin, hurt by one, it retreats.
        EntityHoglin h2 = hoglin(world, stable.add(10, 0, 6));
        EntityHoglin h3 = hoglin(world, stable.add(10, 0, 8));
        kept.setCannotBeHunted(false);
        calm.memories.setFor(Mem.HUNTED_RECENTLY, true, 1000);
        tick(calm, 21);
        c.ok(PiglinTasks.hoglinsOutnumberPiglins(calm), "three adult hoglins outnumber one piglin: "
                + calm.memories.get(Mem.VISIBLE_ADULT_HOGLIN_COUNT));
        calm.attackEntityFrom(DamageSource.causeMobDamage(h2), 1.0F);
        c.eq(h2, calm.memories.get(Mem.AVOID_TARGET), "hurt by one, it retreats");
        for (Entity e : new Entity[]{calm, kept, baby, rider, rider2, h2, h3}) {
            e.setDead();
        }
    }

    private static void brutes(Check c, WorldServer world) {
        BlockPos area = c.area();
        EntityLiving m = BastionMobs.spawn(world, area.add(4, 0, 4), 0.0F, "melee_piglin");
        if (!(m instanceof EntityPiglinBrute)) {
            c.fail("no brute");
            return;
        }
        EntityPiglinBrute brute = (EntityPiglinBrute) m;
        brute.setImmuneToZombification(true);
        c.eq(50.0, brute.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "brute health 50");
        c.eq(7.0, brute.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "attack 7");
        FakePlayer player = c.player("brute", area.add(8, 0, 4));
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        brute.memories.set(Mem.NEAREST_VISIBLE_TARGETABLE_PLAYER, player);
        c.eq(player, brute.findNearestValidAttackTarget(), "a brute attacks a player wearing gold");
        player.setPosition(area.getX() + 20.5, area.getY(), area.getZ() + 4.5);
        c.ok(brute.findNearestValidAttackTarget() == null, "but only within 12 blocks");
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
        EntityPig pig = pig(world, area.add(5, 0, 4));
        PiglinTasks.setAngerTarget(brute, pig);
        tick(brute, 45);
        c.ok(pig.getHealth() < pig.getMaxHealth(), "a brute angry at a pig hits it with its axe: " + pig.getHealth());
        EntityItem ingot = new EntityItem(world, brute.posX, brute.posY, brute.posZ, new ItemStack(Items.GOLD_INGOT));
        ingot.setNoPickupDelay();
        world.spawnEntity(ingot);
        tick(brute, 2);
        c.ok(!ingot.isDead && brute.getHeldItemOffhand().isEmpty(), "brutes ignore gold");
        ingot.setDead();
        pig.setDead();
        brute.setDead();
    }

    private static void zombification(Check c, WorldServer world) {
        BlockPos area = c.area();
        EntityPiglin p = spawnPiglin(world, area.add(4, 0, 4), true);
        p.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
        EntityPiglin immune = spawnPiglin(world, area.add(10, 0, 4), false);
        c.ok(p.isConverting(), "in the overworld a piglin is turning into a zombified piglin");
        c.ok(!immune.isConverting(), "unless immune");
        for (int i = 0; i < 305 && !p.isDead; i++) {
            p.onUpdate();
        }
        c.ok(p.isDead, "after 15 seconds the piglin is gone");
        List<EntityPigZombie> zs = world.getEntitiesWithinAABB(EntityPigZombie.class, new AxisAlignedBB(p.posX, p.posY, p.posZ, p.posX, p.posY, p.posZ).grow(1), e -> !e.isDead);
        c.eq(1, zs.size(), "and a zombified piglin stands in its place");
        if (!zs.isEmpty()) {
            c.ok(zs.get(0).isPotionActive(MobEffects.NAUSEA), "with Nausea");
            c.eq(Items.GOLDEN_SWORD, zs.get(0).getHeldItemMainhand().getItem(), "keeping its sword");
            zs.get(0).setDead();
        }
        EntityLiving bruteMob = BastionMobs.spawn(world, area.add(16, 0, 4), 0.0F, "melee_piglin");
        for (int i = 0; i < 305 && bruteMob != null && !bruteMob.isDead; i++) {
            bruteMob.onUpdate();
        }
        c.ok(bruteMob != null && bruteMob.isDead
                && world.getEntitiesWithinAABB(EntityPigZombie.class, new AxisAlignedBB(bruteMob.posX, bruteMob.posY, bruteMob.posZ,
                bruteMob.posX, bruteMob.posY, bruteMob.posZ).grow(1), e -> !e.isDead).size() == 1,
                "a brute converts too");
        for (EntityPigZombie z : world.getEntitiesWithinAABB(EntityPigZombie.class, new AxisAlignedBB(area).grow(32))) {
            z.setDead();
        }
        WorldServer nether = world.getMinecraftServer().getWorld(-1);
        EntityPiglin native_ = new EntityPiglin(nether);
        native_.setPosition(3000, 100, 3000);
        c.ok(!native_.isConverting(), "in the nether it does not convert");
        immune.setDead();
    }

    private static void saving(Check c, WorldServer world) {
        EntityPiglin p = new EntityPiglin(world);
        p.setChild(true);
        p.setCannotHunt(true);
        p.setImmuneToZombification(true);
        p.inventory().setInventorySlotContents(0, new ItemStack(Items.GOLD_NUGGET, 7));
        p.memories.setFor(Mem.ANGRY_AT, java.util.UUID.randomUUID(), 300);
        p.memories.setFor(Mem.HUNTED_RECENTLY, true, 900);
        NBTTagCompound tag = new NBTTagCompound();
        p.writeEntityToNBT(tag);
        EntityPiglin copy = new EntityPiglin(world);
        copy.readEntityFromNBT(tag);
        c.ok(copy.isChild(), "a baby stays a baby");
        c.ok(copy.isImmuneToZombification(), "immunity is saved");
        c.ok(tag.getBoolean("CannotHunt"), "so is CannotHunt");
        c.eq(7, copy.inventory().getStackInSlot(0).getCount(), "and its inventory");
        c.eq(p.memories.get(Mem.ANGRY_AT), copy.memories.get(Mem.ANGRY_AT), "and who it is angry at");
        c.eq(300L, copy.memories.timeLeft(Mem.ANGRY_AT), "for how long");
        c.ok(copy.memories.has(Mem.HUNTED_RECENTLY), "and its last hunt");
    }

    private static void advancements(Check c, WorldServer world) {
        for (String a : new String[]{"nether/distract_piglin", "nether/find_bastion", "nether/loot_bastion", "nether/explore_nether"}) {
            c.ok(world.getAdvancementManager().getAdvancement(new ResourceLocation("netherupdatebackport", a)) != null, "advancement " + a + " loaded");
        }
        EntityPlayerMP player = Players.real(world, "piglin");
        Advancement shiny = world.getAdvancementManager().getAdvancement(new ResourceLocation("netherupdatebackport", "nether/distract_piglin"));
        if (shiny != null) {
            c.ok(!player.getAdvancements().getProgress(shiny).isDone(), "Oh Shiny is not done yet");
            Triggers.fire(player, "distract_piglin_directly");
            c.ok(player.getAdvancements().getProgress(shiny).isDone(), "handing gold to a piglin in gold armor grants Oh Shiny");
        }
        Advancement tourist = world.getAdvancementManager().getAdvancement(new ResourceLocation("netherupdatebackport", "nether/explore_nether"));
        if (tourist != null) {
            for (String b : new String[]{"soul_sand_valley", "warped_forest", "crimson_forest", "basalt_deltas"}) {
                Triggers.fire(player, "visit_" + b);
            }
            c.ok(!player.getAdvancements().getProgress(tourist).isDone(), "Hot Tourist Destinations still needs the nether wastes");
            c.ok(player.getAdvancements().getProgress(tourist).getCriterionProgress("minecraft:soul_sand_valley").isObtained(),
                    "a biome event counts");
        }
    }

    // ---------------------------------------------------------------------------------------------

    private static EntityPiglin spawnPiglin(WorldServer world, BlockPos at, boolean converting) {
        EntityPiglin p = new EntityPiglin(world);
        p.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setImmuneToZombification(!converting);
        world.spawnEntity(p);
        return p;
    }

    private static EntityHoglin hoglin(WorldServer world, BlockPos at) {
        EntityHoglin h = new EntityHoglin(world);
        h.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        h.setImmuneToZombification(true);
        h.setNoAI(true);
        world.spawnEntity(h);
        return h;
    }

    private static EntityPig pig(WorldServer world, BlockPos at) {
        EntityPig pig = new EntityPig(world);
        pig.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        world.spawnEntity(pig);
        return pig;
    }

    private static void tick(AbstractPiglin p, int n) {
        for (int i = 0; i < n && !p.isDead; i++) {
            p.onUpdate();
        }
    }
}

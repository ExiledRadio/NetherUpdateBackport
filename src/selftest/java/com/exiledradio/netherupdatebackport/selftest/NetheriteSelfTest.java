package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.netherupdatebackport.common.Owners;
import com.exiledradio.netherupdatebackport.netherite.EntityFireproofItem;
import com.exiledradio.netherupdatebackport.netherite.NetheriteContent;
import com.google.common.collect.Multimap;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.Collections;
import java.util.List;

/** Stream N: netherite. */
public final class NetheriteSelfTest implements SelfTest {

    @Override
    public String name() {
        return "netherite";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.eq(30.0F, NetheriteContent.ancientDebris.getDefaultState().getBlockHardness(world, BlockPos.ORIGIN), "ancient debris hardness 30");
        c.eq(50.0F, NetheriteContent.netheriteBlock.getDefaultState().getBlockHardness(world, BlockPos.ORIGIN), "netherite block hardness 50");
        @SuppressWarnings("deprecation")
        float blast = NetheriteContent.ancientDebris.getExplosionResistance(null);
        c.ok(Math.abs(blast - 1200F) < 0.5F, "ancient debris blast resistance 1200: " + blast);
        c.eq(3, NetheriteContent.ancientDebris.getHarvestLevel(NetheriteContent.ancientDebris.getDefaultState()), "a diamond pickaxe mines it");
        c.ok(NetheriteContent.netheriteBlock.isBeaconBase(world, BlockPos.ORIGIN, BlockPos.ORIGIN), "netherite blocks are beacon bases");
        c.ok(NetheriteContent.ingot.isBeaconPayment(new ItemStack(NetheriteContent.ingot)), "the ingot pays a beacon");

        // Tools and armor: 1.16.5's numbers -------------------------------------------------------------
        damage(c, "netherite_sword", 7.0);
        damage(c, "netherite_pickaxe", 5.0);
        damage(c, "netherite_axe", 9.0);
        damage(c, "netherite_shovel", 5.5);
        speed(c, "netherite_sword", -2.4);
        speed(c, "netherite_axe", -3.0);
        speed(c, "netherite_hoe", 0.0);
        c.eq(2031, item("netherite_pickaxe").getMaxDamage(), "tools last 2031 uses");
        c.eq(4, item("netherite_pickaxe").getHarvestLevel(new ItemStack(item("netherite_pickaxe")), "pickaxe", null, null), "mining level 4");
        armor(c, "netherite_helmet", 3, 407);
        armor(c, "netherite_chestplate", 8, 592);
        armor(c, "netherite_leggings", 6, 555);
        armor(c, "netherite_boots", 3, 481);
        c.eq(3.0F, ((ItemArmor) item("netherite_chestplate")).toughness, "toughness 3");
        Multimap<String, AttributeModifier> mods = item("netherite_helmet").getItemAttributeModifiers(EntityEquipmentSlot.HEAD);
        double knockback = 0;
        for (AttributeModifier m : mods.get(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName())) {
            knockback += m.getAmount();
        }
        c.ok(Math.abs(knockback - 0.1) < 1e-9, "each piece gives 0.1 knockback resistance: " + knockback);
        c.ok(((ItemArmor) item("netherite_boots")).getArmorMaterial().getRepairItemStack().getItem() == NetheriteContent.ingot,
                "repaired with netherite ingots");

        // Dropped netherite does not burn ---------------------------------------------------------------
        BlockPos pool = c.area();
        // Deep enough for 1.16's physics: an item falling into lava keeps most of its speed for a while, then rises.
        c.fill(pool.add(-5, -21, -5), pool.add(5, 1, 5), Blocks.LAVA.getDefaultState());
        EntityItem plain = new EntityItem(world, pool.getX() + 0.5, pool.getY() + 0.5, pool.getZ() + 0.5, new ItemStack(NetheriteContent.ingot));
        world.spawnEntity(plain);
        List<EntityFireproofItem> found = world.getEntitiesWithinAABB(EntityFireproofItem.class, new AxisAlignedBB(pool).grow(3));
        c.eq(1, found.size(), "a dropped netherite ingot becomes a fireproof item");
        if (!found.isEmpty()) {
            EntityFireproofItem drop = found.get(0);
            int died = -1;
            for (int i = 0; i < 100 && died < 0; i++) {
                drop.onUpdate();
                if (drop.isDead) {
                    died = i;
                }
            }
            c.ok(!drop.isDead, "it survives 5 seconds in lava (died at tick " + died + ", item " + drop.getItem() + ", immune "
                    + drop.isImmuneToFire() + ", y " + drop.posY + ")");
            c.ok(drop.posY >= pool.getY() - 21, "staying in the lava, not sinking through it: y " + drop.posY);
            // Still, ten blocks down in the pool: 1.16's lava movement lifts it.
            drop.setPosition(pool.getX() + 0.5, pool.getY() - 10, pool.getZ() + 0.5);
            drop.motionX = drop.motionY = drop.motionZ = 0;
            double low = drop.posY;
            StringBuilder path = new StringBuilder();
            for (int i = 0; i < 300; i++) {
                drop.onUpdate();
                low = Math.min(low, drop.posY);
                if (i % 30 == 0) {
                    path.append(String.format(" %.2f/%.3f/%s", drop.posY, drop.motionY,
                            world.getBlockState(new BlockPos(drop)).getBlock().getRegistryName().getPath()));
                }
            }
            c.ok(drop.posY > low + 1.0, "and floating up: from y " + low + " to " + drop.posY + path);
            drop.setDead();
        }
        EntityItem gold = new EntityItem(world, pool.getX() + 0.5, pool.getY() + 0.5, pool.getZ() + 0.5, new ItemStack(Items.GOLD_INGOT));
        world.spawnEntity(gold);
        for (int i = 0; i < 100 && !gold.isDead; i++) {
            gold.onUpdate();
        }
        c.ok(gold.isDead, "a gold ingot in lava burns, as before");

        // Recipes ------------------------------------------------------------------------------------------
        c.eq(NetheriteContent.scrap, FurnaceRecipes.instance().getSmeltingResult(new ItemStack(NetheriteContent.ancientDebris)).getItem(),
                "ancient debris smelts to netherite scrap");
        c.eq(2.0F, FurnaceRecipes.instance().getSmeltingExperience(new ItemStack(NetheriteContent.scrap)), "for 2 experience");
        Recipes.checkAll(c);
        ItemStack s = new ItemStack(NetheriteContent.scrap);
        ItemStack g = new ItemStack(Items.GOLD_INGOT);
        Crafting.expect(c, Crafting.grid(3, 3, s, s, s, s, g, g, g, g, ItemStack.EMPTY), new ItemStack(NetheriteContent.ingot),
                "4 scrap and 4 gold make an ingot");
        ItemStack in = new ItemStack(NetheriteContent.ingot);
        Crafting.expect(c, Crafting.grid(3, 3, in, in, in, in, in, in, in, in, in), new ItemStack(NetheriteContent.netheriteBlock), "9 make a block");

        // The crafting fallback (no smithing table in this test game) -------------------------------------------
        c.ok(!NetheriteContent.hasSmithingTable(), "this test game has no smithing table");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setItemDamage(100);
        sword.setStackDisplayName("Old Faithful");
        EnchantmentHelper.setEnchantments(Collections.singletonMap(Enchantments.SHARPNESS, 3), sword);
        ItemStack made = net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(2, 1, sword, in), world);
        c.eq(item("netherite_sword"), made.getItem(), "a diamond sword and an ingot make a netherite sword");
        c.eq(3, EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, made), "keeping Sharpness III");
        c.eq("Old Faithful", made.getDisplayName(), "and its name");
        c.eq(100, made.getItemDamage(), "and its damage");

        // Smithing (Backport Core's registry, read by Village & Pillage's table) ------------------------------
        int ours = 0;
        for (SmithingUpgrades.Upgrade u : SmithingUpgrades.list()) {
            ours += u.result().getItem().getRegistryName().getNamespace().equals("netherupdatebackport") ? 1 : 0;
        }
        c.eq(9, ours, "all nine upgrades are in Backport Core's smithing registry");
        SmithingUpgrades.Upgrade up = SmithingUpgrades.find(new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(NetheriteContent.ingot));
        c.ok(up != null && up.result().getItem() == item("netherite_pickaxe"), "a diamond pickaxe and our ingot upgrade to our pickaxe");

        // Advancements ------------------------------------------------------------------------------------------
        for (String a : new String[] {"nether/obtain_ancient_debris", "nether/netherite_armor", "husbandry/obtain_netherite_hoe", "nether/use_lodestone"}) {
            c.ok(world.getAdvancementManager().getAdvancement(new ResourceLocation("netherupdatebackport", a)) != null, "advancement " + a + " loaded");
        }

        // Generation --------------------------------------------------------------------------------------
        WorldServer nether = world.getMinecraftServer().getWorld(-1);
        int cx = 2400;
        int cz = -2400;
        for (int x = -1; x <= 6; x++) {
            for (int z = -1; z <= 6; z++) {
                nether.getChunkProvider().provideChunk(cx + x, cz + z);
            }
        }
        int debris = 0;
        int exposed = 0;
        for (int x = 0; x <= 5; x++) {
            for (int z = 0; z <= 5; z++) {
                Chunk chunk = nether.getChunk(cx + x, cz + z);
                for (int bx = 0; bx < 16; bx++) {
                    for (int bz = 0; bz < 16; bz++) {
                        for (int y = 0; y < 128; y++) {
                            if (chunk.getBlockState(bx, y, bz).getBlock() == NetheriteContent.ancientDebris) {
                                debris++;
                                BlockPos p = new BlockPos(chunk.x * 16 + bx, y, chunk.z * 16 + bz);
                                for (EnumFacing f : EnumFacing.values()) {
                                    exposed += nether.isAirBlock(p.offset(f)) ? 1 : 0;
                                }
                            }
                        }
                    }
                }
            }
        }
        c.ok(debris > 0, "36 new nether chunks hold ancient debris: " + debris);
        c.eq(0, exposed, "none of it touching air");
    }

    private static Item item(String name) {
        return Owners.ours(name);
    }

    private static void damage(Check c, String name, double expected) {
        double total = 0;
        for (AttributeModifier m : item(name).getItemAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
            total += m.getAmount();
        }
        c.ok(Math.abs(total - expected) < 1e-6, name + " damage modifier " + expected + " (1 more with the hand): " + total);
    }

    private static void speed(Check c, String name, double expected) {
        double total = 0;
        for (AttributeModifier m : item(name).getItemAttributeModifiers(EntityEquipmentSlot.MAINHAND).get(SharedMonsterAttributes.ATTACK_SPEED.getName())) {
            total += m.getAmount();
        }
        c.ok(Math.abs(total - expected) < 1e-6, name + " speed modifier " + expected + ": " + total);
    }

    private static void armor(Check c, String name, int points, int durability) {
        ItemArmor armor = (ItemArmor) item(name);
        c.eq(points, armor.damageReduceAmount, name + " armor");
        c.eq(durability, armor.getMaxDamage(), name + " durability");
    }
}

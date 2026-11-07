package com.exiledradio.netherupdatebackport.fungus;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps vanilla's oak-only wooden recipes from taking nether planks.
 *
 * <p>Forge turns every vanilla recipe that takes "planks of any kind" into one that takes the
 * {@code plankWood} ore name, which these planks have (so sticks, crafting tables, chests and wooden tools
 * take them, as in 1.16). But some of those recipes make an oak thing that 1.16 gives a crimson and warped
 * version of: a sign, a trapdoor, a button. Vanilla's recipes come first, so crimson planks would make oak
 * signs. Those few recipes are narrowed in place, keeping their names and recipe-book unlocks, the way
 * Village &amp; Pillage narrows the sign recipe for its own woods: every slot that takes nether planks gets
 * the same ingredient without them.
 */
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID)
public final class OakRecipes {

    /** 1.12.2's names for the vanilla recipes that make a thing 1.16 also makes from nether wood. */
    private static final String[] NAMES = {"sign", "trapdoor", "wooden_button", "wooden_pressure_plate", "wooden_door",
            "fence", "fence_gate", "oak_wooden_slab", "oak_stairs", "boat"};

    private OakRecipes() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void narrow(RegistryEvent.Register<IRecipe> event) {
        if (!FungusContent.enabled()) {
            return;
        }
        final List<ItemStack> planks = new ArrayList<ItemStack>();
        for (NetherWood w : NetherWood.values()) {
            planks.add(new ItemStack(FungusContent.block(w.name + "_planks")));
        }
        List<String> changed = new ArrayList<String>();
        for (String name : NAMES) {
            IRecipe recipe = event.getRegistry().getValue(new ResourceLocation("minecraft", name));
            if (recipe == null) {
                continue;
            }
            NonNullList<Ingredient> slots = recipe.getIngredients();
            boolean any = false;
            for (int i = 0; i < slots.size(); i++) {
                Ingredient slot = slots.get(i);
                if (slot != Ingredient.EMPTY && takes(slot, planks)) {
                    try {
                        slots.set(i, new Without(slot, planks));
                        any = true;
                    } catch (UnsupportedOperationException e) {
                        NetherUpdateBackport.LOGGER.warn("minecraft:{} cannot be changed; nether planks make it too.", name);
                        break;
                    }
                }
            }
            if (any) {
                changed.add(name);
            }
        }
        NetherUpdateBackport.LOGGER.debug("Vanilla recipes no longer taking nether planks: {}", changed);
    }

    private static boolean takes(Ingredient ingredient, List<ItemStack> stacks) {
        for (ItemStack s : stacks) {
            if (ingredient.apply(s)) {
                return true;
            }
        }
        return false;
    }

    /** An ingredient minus some items. */
    static final class Without extends Ingredient {
        private final Ingredient base;
        private final List<ItemStack> excluded;

        Without(Ingredient base, List<ItemStack> excluded) {
            super(0);
            this.base = base;
            this.excluded = excluded;
        }

        @Override
        public boolean apply(ItemStack stack) {
            if (stack == null) {
                return false;
            }
            for (ItemStack e : this.excluded) {
                if (stack.getItem() == e.getItem()) {
                    return false;
                }
            }
            return this.base.apply(stack);
        }

        @Override
        public ItemStack[] getMatchingStacks() {
            List<ItemStack> out = new ArrayList<ItemStack>();
            for (ItemStack s : this.base.getMatchingStacks()) {
                if (apply(s)) {
                    out.add(s);
                }
            }
            return out.toArray(new ItemStack[0]);
        }

        @Override
        public boolean isSimple() {
            return false;
        }
    }
}

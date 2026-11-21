package com.exiledradio.netherupdatebackport.babies.client.render;

import com.exiledradio.netherupdatebackport.babies.client.model.BabyHumanoidModel;
import com.exiledradio.netherupdatebackport.netherite.NetheriteContent;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * 26.3's {@code HumanoidArmorLayer} for babies: the baby armor meshes with the {@code humanoid_baby} equipment
 * textures (leather dyed, with its overlay), and 1.12's enchantment glint. Only vanilla armor and this mod's
 * netherite: babies wearing any other armor are drawn by the adult renderer instead (see {@link #isSupported}).
 */
public final class BabyArmorLayer implements LayerRenderer<EntityLivingBase> {

    private static final EntityEquipmentSlot[] ORDER = {
        EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET, EntityEquipmentSlot.HEAD};
    private static final ResourceLocation LEATHER_OVERLAY = BabyRender.tex("equipment/humanoid_baby/leather_overlay");

    private final RenderLivingBase<?> renderer;
    private final Map<EntityEquipmentSlot, BabyHumanoidModel> models = new EnumMap<EntityEquipmentSlot, BabyHumanoidModel>(EntityEquipmentSlot.class);

    public BabyArmorLayer(RenderLivingBase<?> renderer, BabyHumanoidModel.Kind kind) {
        this.renderer = renderer;
        for (EntityEquipmentSlot slot : ORDER) {
            models.put(slot, BabyHumanoidModel.armor(slot, kind));
        }
    }

    /** Vanilla armor in vanilla materials, and this mod's netherite armor: the ones whose baby textures 26.3 has. */
    private static boolean isVanilla(ItemArmor armor) {
        return texture(armor) != null;
    }

    /** @return the {@code humanoid_baby} texture name, or null for armor 26.3 has none for */
    private static String texture(ItemArmor armor) {
        Item item = armor;
        if (item.getRegistryName() == null) {
            return null;
        }
        ItemArmor.ArmorMaterial material = armor.getArmorMaterial();
        String owner = item.getRegistryName().getNamespace();
        if (material == NetheriteContent.ARMOR && com.exiledradio.netherupdatebackport.NetherUpdateBackport.MOD_ID.equals(owner)) {
            return "netherite";
        }
        if (!"minecraft".equals(owner)) {
            return null;
        }
        if (material == ItemArmor.ArmorMaterial.LEATHER || material == ItemArmor.ArmorMaterial.CHAIN
                || material == ItemArmor.ArmorMaterial.IRON || material == ItemArmor.ArmorMaterial.GOLD
                || material == ItemArmor.ArmorMaterial.DIAMOND) {
            return material.getName();
        }
        return null;
    }

    /** False when the entity wears armor this layer can't draw as 26.3 does (modded armor). */
    public static boolean isSupported(EntityLivingBase entity) {
        for (EntityEquipmentSlot slot : ORDER) {
            ItemStack stack = entity.getItemStackFromSlot(slot);
            if (stack.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) stack.getItem();
                if (armor.getEquipmentSlot() == slot && !isVanilla(armor)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        for (EntityEquipmentSlot slot : ORDER) {
            ItemStack stack = entity.getItemStackFromSlot(slot);
            if (!(stack.getItem() instanceof ItemArmor)) {
                continue;
            }
            ItemArmor armor = (ItemArmor) stack.getItem();
            if (armor.getEquipmentSlot() != slot || !isVanilla(armor)) {
                continue;
            }
            BabyHumanoidModel model = models.get(slot);
            model.setModelAttributes(renderer.getMainModel());
            model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            String name = texture(armor);
            renderer.bindTexture(BabyRender.tex("equipment/humanoid_baby/" + name));
            if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER) {
                int color = armor.getColor(stack);
                GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
                model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
                renderer.bindTexture(LEATHER_OVERLAY);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            if (stack.hasEffect()) {
                LayerArmorBase.renderEnchantedGlint(renderer, entity, model, limbSwing, limbSwingAmount, partialTicks,
                        ageInTicks, netHeadYaw, headPitch, scale);
            }
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}

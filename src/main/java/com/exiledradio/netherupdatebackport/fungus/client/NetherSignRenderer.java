package com.exiledradio.netherupdatebackport.fungus.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.fungus.NetherSigns;
import com.exiledradio.netherupdatebackport.fungus.NetherWood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

/**
 * Draws crimson and warped signs with their 1.16 textures.
 *
 * <p>Vanilla's sign renderer decides whether a sign is standing by comparing its block to
 * {@code Blocks.STANDING_SIGN}, so it would draw these as wall signs floating where the post should be. This
 * is the family's renderer (Village &amp; Pillage's): vanilla's with that comparison made an {@code instanceof}
 * and the texture read from the block's wood, and the editing screen's own wall rotation undone for a
 * standing sign.
 */
public final class NetherSignRenderer extends TileEntitySpecialRenderer<NetherSigns.Tile> {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[NetherWood.values().length];

    static {
        for (NetherWood wood : NetherWood.values()) {
            TEXTURES[wood.ordinal()] = new ResourceLocation(NetherUpdateBackport.MOD_ID, "textures/entity/signs/" + wood.name + ".png");
        }
    }

    private final ModelSign model = new ModelSign();

    @Override
    public void render(NetherSigns.Tile te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        Block block = te.getBlockType();
        NetherWood wood = NetherSigns.woodOf(block);
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        if (block instanceof BlockStandingSign) {
            float angle = inEditScreen(x, y, z) ? wallAngle(te.getBlockMetadata()) : te.getBlockMetadata() * 360 / 16.0F;
            GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
            this.model.signStick.showModel = true;
        } else {
            GlStateManager.rotate(-wallAngle(te.getBlockMetadata()), 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
            this.model.signStick.showModel = false;
        }
        if (destroyStage >= 0) {
            bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 2.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        } else {
            bindTexture(TEXTURES[(wood != null ? wood : NetherWood.CRIMSON).ordinal()]);
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.6666667F, -0.6666667F, -0.6666667F);
        this.model.renderSign();
        GlStateManager.popMatrix();

        FontRenderer font = getFontRenderer();
        GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
        GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
        GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
        GlStateManager.depthMask(false);
        if (destroyStage < 0) {
            for (int line = 0; line < te.signText.length; ++line) {
                ITextComponent text = te.signText[line];
                if (text == null) {
                    continue;
                }
                List<ITextComponent> split = GuiUtilRenderComponents.splitText(text, 90, font, false, true);
                String s = split != null && !split.isEmpty() ? split.get(0).getFormattedText() : "";
                if (line == te.lineBeingEdited) {
                    s = "> " + s + " <";
                }
                font.drawString(s, -font.getStringWidth(s) / 2, line * 10 - te.signText.length * 5, 0);
            }
        }
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        if (destroyStage >= 0) {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }

    /** Vanilla's wall-sign angle for a facing's metadata: north 0, south 180, west 90, east -90. */
    private static float wallAngle(int meta) {
        switch (meta) {
            case 2:
                return 180.0F;
            case 4:
                return 90.0F;
            case 5:
                return -90.0F;
            default:
                return 0.0F;
        }
    }

    /** @return true when the sign editing screen is drawing this sign (it always draws at this fixed offset) */
    private static boolean inEditScreen(double x, double y, double z) {
        return Minecraft.getMinecraft().currentScreen instanceof GuiEditSign && x == -0.5D && y == -0.75D && z == -0.5D;
    }
}

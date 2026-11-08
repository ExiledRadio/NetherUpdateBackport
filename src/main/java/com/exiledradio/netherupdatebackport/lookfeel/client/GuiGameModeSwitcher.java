package com.exiledradio.netherupdatebackport.lookfeel.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;

/**
 * 1.16's game mode switcher ({@code GameModeSwitcherScreen}), opened by F3+F4: four slots, creative, survival,
 * adventure and spectator; F4 moves to the next, the mouse picks one once it has moved, and letting go of F3
 * switches to the picked mode with {@code /gamemode} and closes. Laid out from 1.16.5's numbers.
 */
@SideOnly(Side.CLIENT)
public final class GuiGameModeSwitcher extends GuiScreen {

    private static final ResourceLocation TEXTURE = new ResourceLocation(NetherUpdateBackport.MOD_ID,
            "textures/gui/container/gamemode_switcher.png");
    private static final int ALL_SLOTS_WIDTH = Icon.values().length * 30 - 5;

    /** The modes in slot order, as 1.16.5's {@code GameModeIcon}. */
    enum Icon {
        CREATIVE("gameMode.creative", "/gamemode creative", GameType.CREATIVE),
        SURVIVAL("gameMode.survival", "/gamemode survival", GameType.SURVIVAL),
        ADVENTURE("gameMode.adventure", "/gamemode adventure", GameType.ADVENTURE),
        SPECTATOR("gameMode.spectator", "/gamemode spectator", GameType.SPECTATOR);

        final String nameKey;
        final String command;
        final GameType type;

        Icon(String nameKey, String command, GameType type) {
            this.nameKey = nameKey;
            this.command = command;
            this.type = type;
        }

        ItemStack stack() {
            switch (this) {
                case CREATIVE:
                    return new ItemStack(Blocks.GRASS);
                case SURVIVAL:
                    return new ItemStack(Items.IRON_SWORD);
                case ADVENTURE:
                    return new ItemStack(Items.MAP);
                default:
                    return new ItemStack(Items.ENDER_EYE);
            }
        }

        Icon next() {
            return values()[(ordinal() + 1) % values().length];
        }

        @Nullable
        static Icon of(@Nullable GameType type) {
            for (Icon i : values()) {
                if (i.type == type) {
                    return i;
                }
            }
            return null;
        }
    }

    @Nullable
    private final Icon previousHovered;
    @Nullable
    Icon hovered;
    private int firstMouseX;
    private int firstMouseY;
    private boolean setFirstMousePos;

    public GuiGameModeSwitcher() {
        this.previousHovered = Icon.of(defaultSelected());
    }

    /** 1.16.5: the previous game mode, or survival from creative and creative from anything else. */
    private static GameType defaultSelected() {
        GameType current = currentMode();
        GameType previous = PreviousGameMode.get();
        if (previous == null || previous == GameType.NOT_SET) {
            previous = current == GameType.CREATIVE ? GameType.SURVIVAL : GameType.CREATIVE;
        }
        return previous;
    }

    @Nullable
    private static GameType currentMode() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.playerController == null ? null : mc.playerController.getCurrentGameType();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.hovered = this.previousHovered != null ? this.previousHovered : Icon.of(currentMode());
    }

    private int slotX(int index) {
        return this.width / 2 - ALL_SLOTS_WIDTH / 2 + index * 30;
    }

    private int slotY() {
        return this.height / 2 - 30;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (checkToClose()) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(this.width / 2 - 62, this.height / 2 - 30 - 27, 0.0F, 0.0F, 125, 75, 128.0F, 128.0F);
        GlStateManager.popMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (this.hovered != null) {
            drawCenteredString(this.fontRenderer, I18n.format(this.hovered.nameKey), this.width / 2, this.height / 2 - 30 - 20, -1);
        }
        drawCenteredString(this.fontRenderer, I18n.format("debug.gamemodes.select_next",
                TextFormatting.AQUA + I18n.format("debug.gamemodes.press_f4") + TextFormatting.RESET),
                this.width / 2, this.height / 2 + 5, 0xFFFFFF);
        if (!this.setFirstMousePos) {
            this.firstMouseX = mouseX;
            this.firstMouseY = mouseY;
            this.setFirstMousePos = true;
        }
        boolean mouseStill = this.firstMouseX == mouseX && this.firstMouseY == mouseY;
        Icon[] icons = Icon.values();
        for (int i = 0; i < icons.length; i++) {
            int x = slotX(i);
            int y = slotY();
            boolean selected = this.hovered == icons[i];
            drawSlot(icons[i], x, y, selected);
            boolean over = mouseX >= x && mouseY >= y && mouseX < x + 25 && mouseY < y + 25;
            if (!mouseStill && over) {
                this.hovered = icons[i];
            }
        }
    }

    private void drawSlot(Icon icon, int x, int y, boolean selected) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        this.mc.getTextureManager().bindTexture(TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 75.0F, 25, 25, 128.0F, 128.0F);
        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemAndEffectIntoGUI(icon.stack(), x + 5, y + 5);
        RenderHelper.disableStandardItemLighting();
        if (selected) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            this.mc.getTextureManager().bindTexture(TEXTURE);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 25.0F, 75.0F, 25, 25, 128.0F, 128.0F);
            GlStateManager.enableDepth();
        }
    }

    @Override
    public void updateScreen() {
        checkToClose();
    }

    /** 1.16.5: once F3 is up, switch to the picked mode and close. */
    private boolean checkToClose() {
        if (!Keyboard.isKeyDown(Keyboard.KEY_F3)) {
            switchToHovered();
            this.mc.displayGuiScreen(null);
            return true;
        }
        return false;
    }

    private void switchToHovered() {
        Icon current = Icon.of(currentMode());
        if (this.hovered != null && current != null && this.mc.player != null
                && this.mc.player.canUseCommand(2, "") && this.hovered != current) {
            this.mc.player.sendChatMessage(this.hovered.command);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (keyCode == Keyboard.KEY_F4 && this.hovered != null) {
            this.setFirstMousePos = false;
            this.hovered = this.hovered.next();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

package com.exiledradio.netherupdatebackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.PresentationHooks;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.lookfeel.LookAndFeel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.MobEffects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * Stream Lf's client side: 1.16's F3 keys, FOV Effects, the nausea overlay that Distortion Effects brings, and the
 * title screen's Singleplayer button going straight to Create New World when there is no world.
 */
@SideOnly(Side.CLIENT)
public final class LookAndFeelClient {

    static final ResourceLocation NAUSEA = new ResourceLocation(NetherUpdateBackport.MOD_ID, "textures/misc/nausea.png");

    /** Counters for the client self-test. */
    public static volatile int nauseaFrames;
    public static volatile int createWorldRedirects;

    private LookAndFeelClient() {
    }

    public static void init() {
        Contribution v116 = Presentation.contribution("1.16");
        if (v116 != null) {
            v116.debugKey(Keyboard.KEY_F4, "debug.gamemodes.help", LookAndFeelClient::gameModeSwitcher);
            v116.debugKey(Keyboard.KEY_N, "debug.creative_spectator.help", LookAndFeelClient::spectatorToggle);
        }
        MinecraftForge.EVENT_BUS.register(new LookAndFeelClient());
        MinecraftForge.EVENT_BUS.register(new PreviousGameMode());
        com.exiledradio.backportcore.presentation.client.DebugText.add("1.16", "spawn counts, targeted block coordinates", LookAndFeelClient::debugText);
    }

    /** For the client self-test. */
    public static volatile int debugEdits;

    /**
     * 1.16.5's {@code DebugScreenOverlay} edits of the F3 text (20w18a): "Looking at" leaves the left column, the
     * right column's targeted block gets a "Targeted Block: x, y, z" heading (underlined), and in singleplayer the
     * left column gains the spawn line after Local Difficulty: "SC: " the chunks the last spawn pass could use, then
     * each spawn category's count by its initial (1.12.2's four: monster, creature, ambient, water creature).
     */
    static void debugText(com.exiledradio.backportcore.presentation.client.DebugText.Lines lines) {
        Minecraft mc = lines.mc;
        // 1.12.2's "Looking at: ", or 1.13's "Looking at block: " and "Looking at liquid: " when 1.13's step ran.
        for (int looking; (looking = com.exiledradio.backportcore.presentation.client.DebugText.Lines.find(lines.left, "Looking at")) >= 0; ) {
            lines.left.remove(looking);
        }
        net.minecraft.util.math.RayTraceResult hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.BLOCK && hit.getBlockPos() != null && mc.world != null) {
            net.minecraft.util.math.BlockPos pos = hit.getBlockPos();
            String name = String.valueOf(net.minecraft.block.Block.REGISTRY.getNameForObject(mc.world.getBlockState(pos).getBlock()));
            String heading = net.minecraft.util.text.TextFormatting.UNDERLINE + "Targeted Block: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
            int at = lines.right.indexOf(name);
            if (at > 0) {
                String before = lines.right.get(at - 1);
                if (before != null && before.contains("Targeted Block")) {
                    lines.right.set(at - 1, heading);
                } else {
                    lines.right.add(at, heading);
                }
            }
        }
        // 1.13's "Targeted Fluid" heading (when its step ran) gets the fluid's coordinates too.
        int fluid = com.exiledradio.backportcore.presentation.client.DebugText.Lines.find(lines.right,
                net.minecraft.util.text.TextFormatting.UNDERLINE + "Targeted Fluid");
        if (fluid >= 0 && mc.getRenderViewEntity() != null && mc.world != null) {
            net.minecraft.entity.Entity eye = mc.getRenderViewEntity();
            net.minecraft.util.math.Vec3d from = eye.getPositionEyes(lines.partialTicks);
            net.minecraft.util.math.Vec3d look = eye.getLook(lines.partialTicks);
            net.minecraft.util.math.RayTraceResult wet = mc.world.rayTraceBlocks(from, from.add(look.x * 20.0, look.y * 20.0, look.z * 20.0), true, false, false);
            if (wet != null && wet.getBlockPos() != null) {
                net.minecraft.util.math.BlockPos p = wet.getBlockPos();
                lines.right.set(fluid, net.minecraft.util.text.TextFormatting.UNDERLINE + "Targeted Fluid: " + p.getX() + ", " + p.getY() + ", " + p.getZ());
            }
        }
        net.minecraft.server.integrated.IntegratedServer server = mc.getIntegratedServer();
        int difficulty = com.exiledradio.backportcore.presentation.client.DebugText.Lines.find(lines.left, "Local Difficulty: ");
        if (server != null && mc.player != null && difficulty >= 0) {
            String spawn = spawnLine(server.getWorld(mc.player.dimension));
            if (spawn != null) {
                lines.left.add(difficulty + 1, spawn);
            }
        }
        debugEdits++;
    }

    private static java.lang.reflect.Field spawner;
    private static java.lang.reflect.Field eligible;

    /** @return "SC: n, M: a, C: b, A: c, W: d" for the integrated server's world, or null */
    static String spawnLine(net.minecraft.world.WorldServer world) {
        if (world == null) {
            return null;
        }
        try {
            if (spawner == null) {
                spawner = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(net.minecraft.world.WorldServer.class, "entitySpawner", "field_175742_R");
                eligible = net.minecraftforge.fml.relauncher.ReflectionHelper.findField(net.minecraft.world.WorldEntitySpawner.class,
                        "eligibleChunksForSpawning", "field_77193_b");
            }
            int chunks = ((java.util.Set<?>) eligible.get(spawner.get(world))).size();
            StringBuilder s = new StringBuilder("SC: ").append(chunks);
            for (net.minecraft.entity.EnumCreatureType type : new net.minecraft.entity.EnumCreatureType[] {
                    net.minecraft.entity.EnumCreatureType.MONSTER, net.minecraft.entity.EnumCreatureType.CREATURE,
                    net.minecraft.entity.EnumCreatureType.AMBIENT, net.minecraft.entity.EnumCreatureType.WATER_CREATURE}) {
                s.append(", ").append(Character.toUpperCase(type.name().charAt(0))).append(": ").append(world.countEntities(type, true));
            }
            return s.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** 1.16.5 {@code KeyboardHandler}, F3+F4. */
    static boolean gameModeSwitcher() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return false;
        }
        if (!mc.player.canUseCommand(2, "")) {
            PresentationHooks.debugFeedback("debug.gamemodes.error");
        } else {
            mc.displayGuiScreen(new GuiGameModeSwitcher());
        }
        return true;
    }

    /**
     * 1.16.5 {@code KeyboardHandler}, F3+N: spectator, and back to the previous mode. Before any change has been
     * seen this session the previous mode is unknown (1.12.2's server never sends it), and creative, 1.12.2's
     * answer, is used.
     */
    static boolean spectatorToggle() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return false;
        }
        if (!mc.player.canUseCommand(2, "")) {
            PresentationHooks.debugFeedback("debug.creative_spectator.error");
        } else if (!mc.player.isSpectator()) {
            mc.player.sendChatMessage("/gamemode spectator");
        } else {
            GameType previous = PreviousGameMode.get();
            if (previous == null || previous == GameType.NOT_SET || previous == GameType.SPECTATOR) {
                previous = GameType.CREATIVE;
            }
            mc.player.sendChatMessage("/gamemode " + previous.getName());
        }
        return true;
    }

    /** 1.16.2 {@code AbstractClientPlayer.getFieldOfViewModifier}: {@code lerp(fovEffectScale, 1, fov)}. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onFov(FOVUpdateEvent event) {
        float scale = LookAndFeel.fovEffectScale();
        if (scale < 1.0F) {
            event.setNewfov(1.0F + scale * (event.getNewfov() - 1.0F));
        }
    }

    /**
     * 1.16.2 {@code GameRenderer.render}: with nausea and Distortion Effects below 100%, the wobble that P3 takes
     * away is shown as a green overlay ({@code renderConfusionOverlay}) before the HUD.
     */
    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        float scale = LookAndFeel.screenEffectScale();
        if (player == null || scale >= 1.0F || !player.isPotionActive(MobEffects.NAUSEA)) {
            return;
        }
        float portal = player.prevTimeInPortal + (player.timeInPortal - player.prevTimeInPortal) * event.getPartialTicks();
        if (portal > 0.0F) {
            drawConfusion(mc, event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(), portal * (1.0F - scale));
        }
    }

    private static void drawConfusion(Minecraft mc, int width, int height, float strength) {
        double size = 2.0 + (1.0 - 2.0) * strength; // Mth.lerp(strength, 2.0, 1.0)
        double w = width * size;
        double h = height * size;
        double x = (width - w) / 2.0;
        double y = (height - h) / 2.0;
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        GlStateManager.color(0.2F * strength, 0.4F * strength, 0.2F * strength, 1.0F);
        mc.getTextureManager().bindTexture(NAUSEA);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + h, -90.0).tex(0.0, 1.0).endVertex();
        buffer.pos(x + w, y + h, -90.0).tex(1.0, 1.0).endVertex();
        buffer.pos(x + w, y, -90.0).tex(1.0, 0.0).endVertex();
        buffer.pos(x, y, -90.0).tex(0.0, 0.0).endVertex();
        tessellator.draw();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        nauseaFrames++;
    }

    /** 1.16 {@code WorldSelectionList}: with no world to select, Singleplayer opens Create New World. */
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.getGui() instanceof GuiWorldSelection) || !(Minecraft.getMinecraft().currentScreen instanceof GuiMainMenu)) {
            return;
        }
        if (noWorlds(Minecraft.getMinecraft())) {
            event.setGui(new GuiCreateWorld(null));
            createWorldRedirects++;
        }
    }

    /** @return true when the saves folder has no world the world list would show */
    public static boolean noWorlds(Minecraft mc) {
        try {
            return mc.getSaveLoader().getSaveList().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}

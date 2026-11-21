package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.babies.client.BabyModels;
import com.exiledradio.netherupdatebackport.babies.client.render.BabyWrapper;
import com.exiledradio.netherupdatebackport.hoglin.EntityHoglin;
import com.exiledradio.netherupdatebackport.hoglin.EntityZoglin;
import com.exiledradio.netherupdatebackport.piglin.EntityPiglin;
import com.exiledradio.netherupdatebackport.strider.EntityStrider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@code runClient -Pselftest=babyrender}: Game Drops Backport's request 1 seen in a client. Opens a fresh flat
 * creative world, stands each of this mod's mobs as an adult (back row) and a baby (front row) in front of the
 * player, checks the babies go to the baby renderer, and saves {@code screenshots/babies-26.png} (26.1's models,
 * forced on) and {@code babies-old.png} (forced off, 1.16's shrunken adults), then quits.
 */
public final class ClientBabyRenderTest {

    private static final String WORLD = "babyrender";

    private int ticks;
    private int stage;
    private int pass;
    private int fail;
    private final List<EntityLiving> babies = new ArrayList<EntityLiving>();

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientBabyRenderTest());
        NetherUpdateBackport.LOGGER.info("BABYRENDER registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            pass++;
        } else {
            fail++;
            NetherUpdateBackport.LOGGER.error("BABYRENDER FAIL {}", what);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            step(mc);
        } catch (Throwable t) {
            NetherUpdateBackport.LOGGER.error("BABYRENDER threw", t);
            fail++;
            finish(mc);
        }
    }

    private void step(Minecraft mc) throws Exception {
        ticks++;
        switch (stage) {
            case 0:
                if (mc.currentScreen instanceof GuiMainMenu && ticks > 100) {
                    mc.gameSettings.pauseOnLostFocus = false;
                    FileUtils.deleteDirectory(new File(mc.gameDir, "saves/" + WORLD));
                    WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
                    mc.launchIntegratedServer(WORLD, WORLD, settings);
                    next();
                }
                break;
            case 1:
                if (mc.player != null && mc.world != null && mc.getIntegratedServer() != null && ticks > 60) {
                    spawn(mc.getIntegratedServer());
                    next();
                }
                break;
            case 2:
                if (ticks > 60) {
                    mc.gameSettings.hideGUI = true;
                    findBabies(mc.world);
                    ok(babies.size() == 5, "five babies reached the client: " + babies.size());
                    for (EntityLiving baby : babies) {
                        Render<EntityLiving> r = mc.getRenderManager().getEntityRenderObject(baby);
                        ok(r instanceof BabyWrapper, baby.getName() + " has the baby wrapper: " + r);
                        ok(baby.isChild(), baby.getName() + " is a baby on the client");
                    }
                    BabyModels.force(Boolean.TRUE);
                    next();
                }
                break;
            case 3:
                if (ticks > 20) {
                    shot(mc, "babies-26.png");
                    BabyModels.force(Boolean.FALSE);
                    next();
                }
                break;
            case 4:
                if (ticks > 20) {
                    shot(mc, "babies-old.png");
                    BabyModels.force(null);
                    ok(!BabyModels.active(), "without Game Drops Backport and below a 26.1 look, babies keep 1.16's models");
                    finish(mc);
                }
                break;
            default:
                break;
        }
    }

    private void next() {
        stage++;
        ticks = 0;
    }

    private void shot(Minecraft mc, String name) {
        ITextComponent msg = ScreenShotHelper.saveScreenshot(mc.gameDir, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        NetherUpdateBackport.LOGGER.info("BABYRENDER shot {}: {}", name, msg.getUnformattedText());
        ok(new File(mc.gameDir, "screenshots/" + name).isFile(), "saved " + name);
    }

    private void findBabies(World world) {
        babies.clear();
        for (Object o : world.loadedEntityList) {
            if (o instanceof EntityLiving && ((EntityLiving) o).isChild()) {
                babies.add((EntityLiving) o);
            }
        }
    }

    private void finish(Minecraft mc) {
        stage = 99;
        NetherUpdateBackport.LOGGER.info("BABYRENDER DONE pass={} fail={}", pass, fail);
        mc.shutdown();
    }

    /** On the server thread: the five mobs, adults behind, babies in front, all facing the player without AI. */
    private static void spawn(final IntegratedServer server) {
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                WorldServer w = server.getWorld(0);
                // Everything around the player's spawn: the player 3.5 blocks south of the front row, looking north.
                EntityPlayerMP player = server.getPlayerList().getPlayers().get(0);
                int bx = MathHelper.floor(player.posX);
                int bz = MathHelper.floor(player.posZ);
                double by = player.posY;
                player.capabilities.isFlying = true;
                player.connection.setPlayerLocation(bx + 0.5D, by, bz - 3.5D, 0.0F, 22.0F);
                w.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                w.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
                w.setWorldTime(6000L);
                List<Function<World, EntityLiving>> kinds = new ArrayList<Function<World, EntityLiving>>();
                kinds.add(EntityHoglin::new);
                kinds.add(EntityZoglin::new);
                kinds.add(EntityStrider::new);
                kinds.add(EntityPiglin::new);
                kinds.add(EntityPigZombie::new);
                double x = -4.0D;
                for (Function<World, EntityLiving> kind : kinds) {
                    place(w, kind.apply(w), bx + x, by, bz + 6.0D, false);
                    place(w, kind.apply(w), bx + x, by, bz + 2.0D, true);
                    x += 2.0D;
                }
            }
        });
    }

    private static void place(WorldServer w, EntityLiving e, double x, double y, double z, boolean baby) {
        if (baby) {
            if (e instanceof EntityAgeable) {
                ((EntityAgeable) e).setGrowingAge(-24000);
            } else if (e instanceof EntityPiglin) {
                ((EntityPiglin) e).setChild(true);
                e.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            } else if (e instanceof EntityZoglin) {
                ((EntityZoglin) e).setChild(true);
            } else if (e instanceof EntityPigZombie) {
                ((EntityPigZombie) e).setChild(true);
            }
        }
        if (e instanceof EntityPigZombie) {
            e.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.GOLDEN_SWORD));
        }
        e.setNoAI(true);
        e.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 180.0F, 0.0F);
        e.rotationYawHead = 180.0F;
        e.renderYawOffset = 180.0F;
        e.enablePersistence();
        w.spawnEntity(e);
    }
}

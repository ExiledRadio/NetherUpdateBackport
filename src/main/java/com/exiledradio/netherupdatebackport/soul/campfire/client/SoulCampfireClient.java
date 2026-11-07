package com.exiledradio.netherupdatebackport.soul.campfire.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.soul.campfire.BlockSoulCampfire;
import com.exiledradio.netherupdatebackport.soul.campfire.SoulCampfireContent;
import com.exiledradio.netherupdatebackport.soul.campfire.TileEntitySoulCampfire;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The campfire's client setup: its models, the renderer for the food on it, and the smoke textures.
 *
 * <p>Subscribed for the client side only, so a dedicated server never loads it. Everything is done
 * from the model event rather than from {@code ClientSetup}, which keeps this stream's client code in
 * its own package.
 *
 * <h2>Smoke textures</h2>
 * 1.14's twelve {@code big_smoke} frames are stitched into the block atlas, which is the one texture
 * sheet 1.12.2's particle manager will draw a mod's particle from (layer 1). Each plume picks one frame
 * at random and keeps it, as 1.14's {@code pickSprite} does.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NetherUpdateBackport.MOD_ID, value = Side.CLIENT)
public final class SoulCampfireClient {

    static final TextureAtlasSprite[] SMOKE = new TextureAtlasSprite[12];

    private SoulCampfireClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        BlockSoulCampfire campfire = SoulCampfireContent.campfire;
        if (campfire == null) {
            return;
        }
        // The state mapper (signal_fire ignored) and the item model come from common/ClientSetup.
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntitySoulCampfire.class, new TileEntitySoulCampfireRenderer());
    }

    @SubscribeEvent
    public static void stitch(TextureStitchEvent.Pre event) {
        if (SoulCampfireContent.campfire == null) {
            return;
        }
        for (int i = 0; i < SMOKE.length; i++) {
            SMOKE[i] = event.getMap().registerSprite(
                    new ResourceLocation(NetherUpdateBackport.MOD_ID, "particle/big_smoke_" + i));
        }
    }
}

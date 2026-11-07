package com.exiledradio.netherupdatebackport.client;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;

/**
 * Client-only setup. A dedicated server never loads this class: the mod class reaches it through a
 * no-argument call, which names no client type.
 *
 * <ul>
 *   <li>A model for every item model request in {@link Content#itemModels()} - by exact name, never by
 *       walking the model registry's keys, which some performance mods bake lazily.</li>
 *   <li>One model name per multipart block ({@link Content#multipart}).</li>
 *   <li>Properties a block's blockstate file leaves out ({@link Content#ignoredProperties}), such as a
 *       fence gate's {@code powered} or a leaf-like block's decay flags.</li>
 * </ul>
 * Streams with renderers of their own register them from a {@code client} class of their own package,
 * called from {@link #init()}.
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientSetup());
        com.exiledradio.netherupdatebackport.blocks.client.BlocksClient.init();
        com.exiledradio.netherupdatebackport.soul.client.SoulClient.init();
        com.exiledradio.netherupdatebackport.hoglin.client.HoglinClient.init();
        com.exiledradio.netherupdatebackport.strider.client.StriderClient.init();
        com.exiledradio.netherupdatebackport.fungus.client.FungusClient.init();
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (Map.Entry<Item, Map<Integer, String>> entry : Content.itemModels().entrySet()) {
            for (Map.Entry<Integer, String> meta : entry.getValue().entrySet()) {
                ModelLoader.setCustomModelResourceLocation(entry.getKey(), meta.getKey(),
                        new ModelResourceLocation(NetherUpdateBackport.MOD_ID + ":" + meta.getValue(), "inventory"));
            }
        }
        for (final Block block : Content.multipartBlocks()) {
            final ModelResourceLocation one = new ModelResourceLocation(block.getRegistryName(), "multipart");
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return one;
                }
            });
        }
        for (Map.Entry<Block, String[]> entry : Content.ignoredProperties().entrySet()) {
            StateMap.Builder builder = new StateMap.Builder();
            for (String name : entry.getValue()) {
                builder.ignore(entry.getKey().getBlockState().getProperty(name));
            }
            ModelLoader.setCustomStateMapper(entry.getKey(), builder.build());
        }
    }
}

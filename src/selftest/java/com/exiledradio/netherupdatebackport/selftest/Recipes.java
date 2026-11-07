package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/** Every recipe file of this mod whose items exist is a registered recipe (a parse error would drop it). */
final class Recipes {

    private Recipes() {
    }

    static void checkAll(Check c) throws Exception {
        URL url = Recipes.class.getResource("/assets/" + NetherUpdateBackport.MOD_ID + "/recipes/_factories.json");
        if (url == null) {
            c.fail("recipes/_factories.json is missing");
            return;
        }
        URI uri = url.toURI();
        FileSystem fs = null;
        Path dir;
        if ("jar".equals(uri.getScheme())) {
            fs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
            dir = fs.getPath("/assets/" + NetherUpdateBackport.MOD_ID + "/recipes");
        } else {
            dir = Paths.get(uri).getParent();
        }
        int files = 0;
        int active = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : stream) {
                String file = p.getFileName().toString();
                if (file.startsWith("_")) {
                    continue;
                }
                files++;
                String name = file.substring(0, file.length() - ".json".length());
                boolean wanted = true;
                try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                    JsonObject json = new JsonParser().parse(r).getAsJsonObject();
                    if (json.has("conditions")) {
                        for (JsonElement cond : json.getAsJsonArray("conditions")) {
                            JsonArray items = cond.getAsJsonObject().getAsJsonArray("items");
                            for (JsonElement e : items) {
                                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(e.getAsString()));
                                wanted &= item != null && new ResourceLocation(e.getAsString()).equals(item.getRegistryName());
                            }
                        }
                    }
                }
                ResourceLocation id = new ResourceLocation(NetherUpdateBackport.MOD_ID, name);
                if (wanted) {
                    active++;
                    c.ok(CraftingManager.REGISTRY.containsKey(id), "recipe " + name + " is registered");
                } else {
                    c.ok(!CraftingManager.REGISTRY.containsKey(id), "recipe " + name + " is skipped with its items missing");
                }
            }
        } finally {
            if (fs != null) {
                fs.close();
            }
        }
        NetherUpdateBackport.LOGGER.info("SELFTEST recipes: {} files, {} active", files, active);
    }
}

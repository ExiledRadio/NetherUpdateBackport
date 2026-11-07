package com.exiledradio.netherupdatebackport.selftest;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.world.WorldServer;

import java.util.UUID;

/**
 * A real server player with a connection that goes nowhere, for tests that go through the server's own
 * right-click path (which sends packets a fake player cannot take).
 */
final class Players {

    private Players() {
    }

    static EntityPlayerMP real(WorldServer world, String name) {
        MinecraftServer server = world.getMinecraftServer();
        EntityPlayerMP player = new EntityPlayerMP(server, world,
                new GameProfile(UUID.nameUUIDFromBytes(("selftest:" + name).getBytes()), "Test" + name),
                new PlayerInteractionManager(world));
        player.connection = new NetHandlerPlayServer(server, new NetworkManager(EnumPacketDirection.SERVERBOUND), player);
        return player;
    }
}

package com.exiledradio.netherupdatebackport.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/** The mod's screens, by id. Client screens are made by name-free calls into client classes. */
public final class GuiHandler implements IGuiHandler {

    /** The smithing table copy's upgrade screen. */
    public static final int SMITHING = 0;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == SMITHING) {
            return new com.exiledradio.netherupdatebackport.owned.ContainerSmithingTable(player.inventory, world, new BlockPos(x, y, z));
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == SMITHING) {
            return com.exiledradio.netherupdatebackport.owned.client.SmithingClient.screen(player, world, new BlockPos(x, y, z));
        }
        return null;
    }
}

package com.exiledradio.netherupdatebackport.owned.client;

import com.exiledradio.netherupdatebackport.owned.ContainerSmithingTable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Builds the smithing screen; reached only from the client side of the gui handler. */
public final class SmithingClient {

    private SmithingClient() {
    }

    public static Object screen(EntityPlayer player, World world, BlockPos pos) {
        return new GuiSmithingTable(player.inventory, new ContainerSmithingTable(player.inventory, world, pos));
    }
}

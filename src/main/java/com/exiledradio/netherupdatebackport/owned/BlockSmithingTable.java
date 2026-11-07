package com.exiledradio.netherupdatebackport.owned;

import com.exiledradio.backportcore.smithing.SmithingUpgrades;
import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.common.GuiHandler;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The smithing table, as Village &amp; Pillage registers it: an identical copy for games without that mod
 * (Rule 1). Same single state (metadata 0), wood, hardness 2.5, resistance as Village &amp; Pillage sets it
 * ({@code setResistance(hardness * 5)}), wood sounds; right-clicked it opens 1.16's upgrade screen.
 *
 * <p>Village &amp; Pillage owns this block; with it installed this copy is never registered and its table
 * does the upgrades, reading the same Backport Core registry.
 */
public final class BlockSmithingTable extends Block {

    BlockSmithingTable() {
        super(Material.WOOD);
        setHardness(2.5F);
        setResistance(2.5F * 5.0F);
        setSoundType(SoundType.WOOD);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!SmithingUpgrades.isActive()) {
            return false;
        }
        if (!world.isRemote) {
            player.openGui(NetherUpdateBackport.instance, GuiHandler.SMITHING, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}

package com.exiledradio.netherupdatebackport.selftest;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Counting (and clearing) dropped items. */
final class Drops {

    private Drops() {
    }

    /** @return how many of an item lie within 2 blocks of a position; removes every item there */
    static int count(WorldServer world, BlockPos at, Item want) {
        int n = 0;
        for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(2))) {
            if (!e.isDead && e.getItem().getItem() == want) {
                n += e.getItem().getCount();
            }
            e.setDead();
        }
        return n;
    }
}

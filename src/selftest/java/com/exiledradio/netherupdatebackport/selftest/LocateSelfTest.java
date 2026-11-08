package com.exiledradio.netherupdatebackport.selftest;

import com.exiledradio.backportcore.locate.LocateChecks;
import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.NetherStructureSlots;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;

import com.exiledradio.netherupdatebackport.NetherUpdateBackport;
import com.exiledradio.netherupdatebackport.structure.BastionStructure;
import com.exiledradio.netherupdatebackport.structure.NetherFossils;
import com.exiledradio.netherupdatebackport.structure.RuinedPortals;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Bastions, nether fossils and ruined portals in Backport Core's {@code /locate}: each id finds a start, in today's
 * order, that the generator then lays out as named (a ruined portal of that type); bastions and Core's fortresses
 * on the shared grid; the tag, the command and tab completion.
 */
final class LocateSelfTest implements SelfTest {

    @Override
    public String name() {
        return "locate";
    }

    @Override
    public void run(Check c) throws Exception {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer overworld = server.getWorld(0);
        WorldServer nether = server.getWorld(-1);
        BlockPos origin = new BlockPos(0, 64, 0);
        nether.getChunk(0, 0);

        StructureLocate.Found bastion = located(c, nether, origin, "minecraft:bastion_remnant", Structures.BASTION_REMNANT);
        if (bastion != null) {
            StructureStart start = start(nether, Structures.BASTION_REMNANT, bastion);
            c.ok(start instanceof BastionStructure.Start, "the bastion is a bastion start: " + start);
        }
        StructureLocate.Found fortress = StructureLocate.nearest(nether, origin, "minecraft:fortress");
        if (c.ok(fortress != null, "Core finds a fortress: " + fortress) && bastion != null) {
            c.ok(!fortress.pos.equals(bastion.pos), "a fortress and a bastion never share a slot");
            int fx = fortress.pos.getX() >> 4;
            int fz = fortress.pos.getZ() >> 4;
            c.eq(NetherStructureSlots.Slot.FORTRESS, NetherStructureSlots.startAt(nether, fx, fz), "the fortress is on a fortress slot");
            c.eq(NetherStructureSlots.Slot.BASTION, NetherStructureSlots.startAt(nether, bastion.pos.getX() >> 4, bastion.pos.getZ() >> 4),
                    "the bastion is on a bastion slot");
        }

        StructureLocate.Found fossil = located(c, nether, origin, "minecraft:nether_fossil", Structures.NETHER_FOSSIL);
        if (fossil != null) {
            c.ok(start(nether, Structures.NETHER_FOSSIL, fossil) instanceof NetherFossils.Start, "the fossil is a fossil start");
        }

        portal(c, nether, origin, RuinedPortals.Type.NETHER);
        c.ok(StructureLocate.nearest(overworld, overworld.getSpawnPoint(), "minecraft:ruined_portal_nether") == null,
                "no nether ruined portal in the overworld");
        BlockPos spawn = overworld.getSpawnPoint();
        for (RuinedPortals.Type type : RuinedPortals.Type.values()) {
            if (type != RuinedPortals.Type.NETHER) {
                portal(c, overworld, spawn, type);
            }
        }

        for (WorldServer world : new WorldServer[]{overworld, nether}) {
            BlockPos from = world == nether ? origin : spawn;
            StructureLocate.Found any = StructureLocate.nearest(world, from, "#minecraft:ruined_portal");
            if (c.ok(any != null && any.id.startsWith("minecraft:ruined_portal"), "#minecraft:ruined_portal finds one: " + any)) {
                String why = LocateChecks.checkRingOrder(world, from, "#minecraft:ruined_portal", any);
                c.ok(why == null, "#minecraft:ruined_portal is today's ring order: " + why);
                List<String> said = LocateChecks.run(server, world, from, "locate structure #ruined_portal");
                c.ok(said.size() == 1 && said.get(0).startsWith("The nearest #minecraft:ruined_portal (" + any.id + ") is at ["
                        + any.pos.getX() + ", ~, " + any.pos.getZ() + "]"), "the tag's reply: " + said);
            }
        }
        c.ok(StructureLocate.nearest(overworld, spawn, "minecraft:bastion_remnant") == null, "no bastions in the overworld");
        List<String> said = LocateChecks.run(server, nether, origin, "locate structure minecraft:bastion_remnant");
        c.ok(bastion != null && said.size() == 1 && said.get(0).startsWith("The nearest minecraft:bastion_remnant is at ["
                + bastion.pos.getX() + ", ~, " + bastion.pos.getZ() + "]"), "the bastion's reply: " + said);

        List<String> ids = LocateChecks.complete(server, nether, origin, "locate structure ");
        c.ok(ids.containsAll(Arrays.asList("minecraft:bastion_remnant", "minecraft:nether_fossil", "minecraft:fortress",
                "#minecraft:ruined_portal", "minecraft:ruined_portal", "minecraft:ruined_portal_nether")), "completion lists the ids: " + ids);
        List<String> portals = LocateChecks.complete(server, nether, origin, "locate structure ruined_portal");
        c.eq(7, portals.size(), "completion lists the seven ruined portals: " + portals);
    }

    private static void portal(Check c, WorldServer world, BlockPos from, RuinedPortals.Type type) {
        String id = type == RuinedPortals.Type.STANDARD ? "minecraft:ruined_portal" : "minecraft:ruined_portal_" + type.name().toLowerCase(Locale.ROOT);
        StructureLocate.Found found = located(c, world, from, id, Structures.RUINED_PORTAL);
        if (found != null) {
            StructureStart start = start(world, Structures.RUINED_PORTAL, found);
            String built = start == null || start.getComponents().isEmpty() ? null
                    : start.getComponents().get(0).createStructureBaseNBT().getString("PortalType");
            c.ok(start instanceof RuinedPortals.Start && type.id().equals(built),
                    "the generator built a " + type + " ruined portal where " + id + " was located: " + start);
        }
    }

    /** Locates, checks today's order and the corner, and generates around the start. */
    private static StructureLocate.Found located(Check c, WorldServer world, BlockPos from, String id, String kind) {
        StructureLocate.Found found;
        try {
            found = StructureLocate.nearest(world, from, id);
        } catch (StructureLocate.Unavailable u) {
            c.fail(id + ": " + u.getMessage());
            return null;
        }
        if (!c.ok(found != null && id.equals(found.id), "locate finds " + id + ": " + found)) {
            return null;
        }
        NetherUpdateBackport.LOGGER.info("SELFTEST locate {} from {} in {}", found, from, world.provider.getDimension());
        String why = LocateChecks.checkRingOrder(world, from, id, found);
        c.ok(why == null, id + " is today's ring order: " + why);
        c.ok((found.pos.getX() & 15) == 0 && (found.pos.getZ() & 15) == 0, id + " is reported at its chunk's corner");
        LocateChecks.generateAround(world, found.pos.getX() >> 4, found.pos.getZ() >> 4, 2);
        StructureStart start = start(world, kind, found);
        c.ok(start != null && start.isSizeableStructure(), "the generator laid out a start where " + id + " was located");
        return found;
    }

    private static StructureStart start(WorldServer world, String kind, StructureLocate.Found found) {
        ScatteredStructure structure = ScatteredStructure.of(world, kind);
        return structure == null ? null : LocateChecks.savedStart(structure, found.pos.getX() >> 4, found.pos.getZ() >> 4);
    }
}

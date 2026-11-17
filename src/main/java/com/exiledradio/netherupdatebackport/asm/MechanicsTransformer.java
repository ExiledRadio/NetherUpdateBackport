package com.exiledradio.netherupdatebackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Patches P5 to P11 (docs/PATCHES.md), stream C: 1.16's changes to how existing things behave, where no Forge
 * event comes at the right moment. Every hook is in {@code changes.ChangeHooks} and reads its setting each call,
 * so a switched-off change runs vanilla's code.
 *
 * <ul>
 *   <li><b>P5</b>, {@code EntityLivingBase.knockBack}: a head guard; the hook fires Forge's event and applies
 *       1.16's knockback, scaled by {@code 1 - knockback resistance}.</li>
 *   <li><b>P6</b>, {@code EntityXPOrb.onCollideWithPlayer}: its one {@code EnchantmentHelper.getEnchantedItem}
 *       call goes to a hook of the same signature that picks only among damaged Mending items.</li>
 *   <li><b>P7</b>, {@code Teleporter.placeInExistingPortal}: the search radius 128 (three {@code sipush 128},
 *       two {@code bipush -128}) becomes the hook's 16 for the nether, 128 elsewhere.</li>
 *   <li><b>P8</b>, {@code Entity.onEntityUpdate}: right after its {@code handleWaterMovement()}, a call that
 *       pushes the entity along flowing lava.</li>
 *   <li><b>P9</b>, {@code EntityLivingBase.checkTotemDeathProtection}: after its one {@code setEntityState}
 *       (the totem has worked), a call that adds Fire Resistance.</li>
 *   <li><b>P10</b>, {@code EntityLiving.despawnEntity}: a head guard; a mob riding anything does not despawn.</li>
 *   <li><b>P11</b>, {@code BlockRedstoneWire}: a head guard on {@code getWeakPower} (1.16's sides), and
 *       {@code getActualState}'s result passed through a hook (an unconnected wire is drawn as a cross).</li>
 * </ul>
 */
public final class MechanicsTransformer implements IClassTransformer {

    private static final String HOOKS = "com/exiledradio/netherupdatebackport/changes/ChangeHooks";
    private static final String LIVING_BASE = "net.minecraft.entity.EntityLivingBase";
    private static final String LIVING = "net.minecraft.entity.EntityLiving";
    private static final String ENTITY = "net.minecraft.entity.Entity";
    private static final String ORB = "net.minecraft.entity.item.EntityXPOrb";
    private static final String TELEPORTER = "net.minecraft.world.Teleporter";
    private static final String WIRE = "net.minecraft.block.BlockRedstoneWire";

    private static final String D_ENTITY = "Lnet/minecraft/entity/Entity;";
    private static final String D_LIVING_BASE = "Lnet/minecraft/entity/EntityLivingBase;";
    private static final String D_STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String D_ACCESS = "Lnet/minecraft/world/IBlockAccess;";
    private static final String D_POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String D_FACING = "Lnet/minecraft/util/EnumFacing;";

    /** P5 to P11, set once each is patched; the self-test reads them. */
    public static volatile boolean appliedP5;
    public static volatile boolean appliedP6;
    public static volatile boolean appliedP7;
    public static volatile boolean appliedP8;
    public static volatile boolean appliedP9;
    public static volatile boolean appliedP10;
    public static volatile boolean appliedP11;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (LIVING_BASE.equals(transformedName)) {
            return livingBase(basicClass);
        }
        if (ORB.equals(transformedName)) {
            return orb(basicClass);
        }
        if (TELEPORTER.equals(transformedName)) {
            return teleporter(basicClass);
        }
        if (ENTITY.equals(transformedName)) {
            return entity(basicClass);
        }
        if (LIVING.equals(transformedName)) {
            return living(basicClass);
        }
        if (WIRE.equals(transformedName)) {
            return wire(basicClass);
        }
        return basicClass;
    }

    // P5 and P9 ---------------------------------------------------------------------------------------------

    private static byte[] livingBase(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        boolean changed = false;

        MethodNode knock = Asm.method(node, "knockBack", "func_70653_a", "(" + D_ENTITY + "FDD)V");
        if (knock == null) {
            Asm.LOG.warn("P5 knockback resistance: {}.knockBack not found; patch skipped.", LIVING_BASE);
        } else {
            InsnList body = new InsnList();
            body.add(new VarInsnNode(Opcodes.ALOAD, 0));
            body.add(new VarInsnNode(Opcodes.ALOAD, 1));
            body.add(new VarInsnNode(Opcodes.FLOAD, 2));
            body.add(new VarInsnNode(Opcodes.DLOAD, 3));
            body.add(new VarInsnNode(Opcodes.DLOAD, 5));
            body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "knockBack", "(" + D_LIVING_BASE + D_ENTITY + "FDD)V", false));
            body.add(new InsnNode(Opcodes.RETURN));
            if (Asm.guardAtHead(node, knock, HOOKS, "knockbackActive", body)) {
                appliedP5 = true;
                changed = true;
                Asm.LOG.info("P5 knockback resistance: patched {}.knockBack.", LIVING_BASE);
            } else {
                Asm.LOG.warn("P5 knockback resistance: {}.knockBack starts on a frame; patch skipped.", LIVING_BASE);
            }
        }

        MethodNode totem = Asm.method(node, "checkTotemDeathProtection", "func_190628_d", "(Lnet/minecraft/util/DamageSource;)Z");
        List<MethodInsnNode> states = totem == null ? new ArrayList<MethodInsnNode>()
                : calls(totem, "net/minecraft/world/World", "setEntityState", "func_72960_a", "(" + D_ENTITY + "B)V");
        if (states.size() != 1) {
            Asm.LOG.warn("P9 totem fire resistance: {}.checkTotemDeathProtection not of the expected shape; patch skipped.", LIVING_BASE);
        } else {
            InsnList call = new InsnList();
            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "totemUsed", "(" + D_LIVING_BASE + ")V", false));
            // After the call, where the method returns true: 1.16 adds the effect just before this broadcast.
            totem.instructions.insert(states.get(0), call);
            appliedP9 = true;
            changed = true;
            Asm.LOG.info("P9 totem fire resistance: patched {}.checkTotemDeathProtection.", LIVING_BASE);
        }
        return changed ? Asm.write(node) : bytes;
    }

    // P6 ----------------------------------------------------------------------------------------------------

    private static byte[] orb(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "onCollideWithPlayer", "func_70100_b_", "(Lnet/minecraft/entity/player/EntityPlayer;)V");
        String desc = "(Lnet/minecraft/enchantment/Enchantment;" + D_LIVING_BASE + ")Lnet/minecraft/item/ItemStack;";
        List<MethodInsnNode> found = m == null ? new ArrayList<MethodInsnNode>()
                : calls(m, "net/minecraft/enchantment/EnchantmentHelper", "getEnchantedItem", "func_92099_a", desc);
        if (found.size() != 1) {
            Asm.LOG.warn("P6 mending: {}.onCollideWithPlayer calls getEnchantedItem {} times, not once; patch skipped.", ORB, found.size());
            return bytes;
        }
        MethodInsnNode call = found.get(0);
        call.owner = HOOKS;
        call.name = "mendingItem";
        appliedP6 = true;
        Asm.LOG.info("P6 mending: patched {}.onCollideWithPlayer.", ORB);
        return Asm.write(node);
    }

    // P7 ----------------------------------------------------------------------------------------------------

    private static byte[] teleporter(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "placeInExistingPortal", "func_180620_b", "(" + D_ENTITY + "F)Z");
        FieldNode world = null;
        for (FieldNode f : node.fields) {
            if (("world".equals(f.name) || "field_85192_a".equals(f.name)) && "Lnet/minecraft/world/WorldServer;".equals(f.desc)) {
                world = f;
            }
        }
        if (m == null || world == null) {
            Asm.LOG.warn("P7 portal search radius: {}.placeInExistingPortal or its world not found; patch skipped.", TELEPORTER);
            return bytes;
        }
        List<IntInsnNode> plus = new ArrayList<IntInsnNode>();
        List<IntInsnNode> minus = new ArrayList<IntInsnNode>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof IntInsnNode) {
                IntInsnNode push = (IntInsnNode) i;
                if (push.getOpcode() == Opcodes.SIPUSH && push.operand == 128) {
                    plus.add(push);
                } else if (push.getOpcode() == Opcodes.BIPUSH && push.operand == -128) {
                    minus.add(push);
                }
            }
        }
        if (plus.size() != 3 || minus.size() != 2) {
            Asm.LOG.warn("P7 portal search radius: {}.placeInExistingPortal has {} x 128 and {} x -128, not 3 and 2; patch skipped.",
                    TELEPORTER, plus.size(), minus.size());
            return bytes;
        }
        for (IntInsnNode push : plus) {
            m.instructions.insertBefore(push, radius(node.name, world));
            m.instructions.remove(push);
        }
        for (IntInsnNode push : minus) {
            InsnList negative = radius(node.name, world);
            negative.add(new InsnNode(Opcodes.INEG));
            m.instructions.insertBefore(push, negative);
            m.instructions.remove(push);
        }
        appliedP7 = true;
        Asm.LOG.info("P7 portal search radius: patched {}.placeInExistingPortal.", TELEPORTER);
        return Asm.write(node);
    }

    private static InsnList radius(String owner, FieldNode world) {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, owner, world.name, world.desc));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "portalRadius", "(Lnet/minecraft/world/World;)I", false));
        return list;
    }

    // P8 ----------------------------------------------------------------------------------------------------

    private static byte[] entity(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "onEntityUpdate", "func_70030_z", "()V");
        List<MethodInsnNode> found = m == null ? new ArrayList<MethodInsnNode>()
                : calls(m, node.name, "handleWaterMovement", "func_70072_I", "()Z");
        AbstractInsnNode pop = found.size() == 1 ? next(found.get(0)) : null;
        if (pop == null || pop.getOpcode() != Opcodes.POP) {
            Asm.LOG.warn("P8 lava pushes: {}.onEntityUpdate not of the expected shape; patch skipped.", ENTITY);
            return bytes;
        }
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "lavaPush", "(" + D_ENTITY + ")V", false));
        m.instructions.insert(pop, call);
        appliedP8 = true;
        Asm.LOG.info("P8 lava pushes: patched {}.onEntityUpdate.", ENTITY);
        return Asm.write(node);
    }

    // P10 ---------------------------------------------------------------------------------------------------

    private static byte[] living(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "despawnEntity", "func_70623_bb", "()V");
        if (m == null) {
            Asm.LOG.warn("P10 riders stay: {}.despawnEntity not found; patch skipped.", LIVING);
            return bytes;
        }
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 0));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "keepRider", "(Lnet/minecraft/entity/EntityLiving;)Z", false));
        InsnList body = new InsnList();
        body.add(new InsnNode(Opcodes.RETURN));
        if (!Asm.ifAtHead(node, m, test, body)) {
            Asm.LOG.warn("P10 riders stay: {}.despawnEntity starts on a frame; patch skipped.", LIVING);
            return bytes;
        }
        appliedP10 = true;
        Asm.LOG.info("P10 riders stay: patched {}.despawnEntity.", LIVING);
        return Asm.write(node);
    }

    // P11 ---------------------------------------------------------------------------------------------------

    private static byte[] wire(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode weak = Asm.method(node, "getWeakPower", "func_180656_a", "(" + D_STATE + D_ACCESS + D_POS + D_FACING + ")I");
        MethodNode actual = Asm.method(node, "getActualState", "func_176221_a", "(" + D_STATE + D_ACCESS + D_POS + ")" + D_STATE);
        List<AbstractInsnNode> returns = new ArrayList<AbstractInsnNode>();
        if (actual != null) {
            for (AbstractInsnNode i = actual.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i.getOpcode() == Opcodes.ARETURN) {
                    returns.add(i);
                }
            }
        }
        if (weak == null || actual == null || returns.size() != 1) {
            Asm.LOG.warn("P11 redstone dot and cross: {}.getWeakPower or getActualState not of the expected shape; patch skipped.", WIRE);
            return bytes;
        }
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new VarInsnNode(Opcodes.ALOAD, 2));
        body.add(new VarInsnNode(Opcodes.ALOAD, 3));
        body.add(new VarInsnNode(Opcodes.ALOAD, 4));
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "weakPower",
                "(Lnet/minecraft/block/BlockRedstoneWire;" + D_STATE + D_ACCESS + D_POS + D_FACING + ")I", false));
        body.add(new InsnNode(Opcodes.IRETURN));
        if (!Asm.guardAtHead(node, weak, HOOKS, "redstoneActive", body)) {
            Asm.LOG.warn("P11 redstone dot and cross: {}.getWeakPower starts on a frame; patch skipped.", WIRE);
            return bytes;
        }
        InsnList pass = new InsnList();
        pass.add(new VarInsnNode(Opcodes.ALOAD, 2));
        pass.add(new VarInsnNode(Opcodes.ALOAD, 3));
        pass.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "actualState", "(" + D_STATE + D_ACCESS + D_POS + ")" + D_STATE, false));
        actual.instructions.insertBefore(returns.get(0), pass);
        appliedP11 = true;
        Asm.LOG.info("P11 redstone dot and cross: patched {}.", WIRE);
        return Asm.write(node);
    }

    // -------------------------------------------------------------------------------------------------------

    private static List<MethodInsnNode> calls(MethodNode m, String owner, String mcp, String srg, String desc) {
        List<MethodInsnNode> out = new ArrayList<MethodInsnNode>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode) {
                MethodInsnNode c = (MethodInsnNode) i;
                if (owner.equals(c.owner) && desc.equals(c.desc) && (mcp.equals(c.name) || srg.equals(c.name))) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    /** @return the next real instruction (labels, lines and frames skipped) */
    private static AbstractInsnNode next(AbstractInsnNode i) {
        AbstractInsnNode n = i.getNext();
        while (n != null && n.getOpcode() < 0) {
            n = n.getNext();
        }
        return n;
    }
}

package com.exiledradio.netherupdatebackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patches P3 and P4 (docs/PATCHES.md), stream Lf: two 1.16 options that act inside render code no Forge event
 * reaches. Each passes one value through a hook, an owner swap of nothing: a static call taking the value and
 * returning it scaled, with no jump and no new local.
 *
 * <ul>
 *   <li><b>P3</b>, {@code EntityRenderer.setupCameraTransform}: the portal wobble strength
 *       {@code f1 = prevTimeInPortal + (timeInPortal - prevTimeInPortal) * partialTicks} becomes
 *       {@code LookHooks.portalWobble(...)} before it is stored (Distortion Effects, 1.16.2).</li>
 *   <li><b>P4</b>, {@code RenderGlobal.setupTerrain}: the argument of its one
 *       {@code Entity.setRenderDistanceWeight} call becomes {@code LookHooks.entityDistance(...)} (Entity Distance,
 *       1.16).</li>
 * </ul>
 */
public final class LookTransformer implements IClassTransformer {

    private static final String HOOKS = "com/exiledradio/netherupdatebackport/lookfeel/LookHooks";
    private static final String RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String GLOBAL = "net.minecraft.client.renderer.RenderGlobal";

    /** Set once each class has been patched; the self-test reads them. */
    public static volatile boolean appliedP3;
    public static volatile boolean appliedP4;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (RENDERER.equals(transformedName)) {
            return portalWobble(basicClass);
        }
        if (GLOBAL.equals(transformedName)) {
            return entityDistance(basicClass);
        }
        return basicClass;
    }

    private static byte[] portalWobble(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "setupCameraTransform", "func_78479_a", "(FI)V");
        if (m == null) {
            Asm.LOG.warn("P3 distortion effects: {}.setupCameraTransform not found; patch skipped, vanilla behavior kept.", RENDERER);
            return bytes;
        }
        // The one FSTORE that ends the expression starting at the first read of prevTimeInPortal.
        AbstractInsnNode store = null;
        int reads = 0;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof FieldInsnNode && i.getOpcode() == Opcodes.GETFIELD && isPrevTimeInPortal((FieldInsnNode) i)) {
                reads++;
                if (reads == 1) {
                    for (AbstractInsnNode j = i.getNext(); j != null; j = j.getNext()) {
                        if (j.getOpcode() == Opcodes.FSTORE) {
                            store = j;
                            break;
                        }
                        if (j instanceof MethodInsnNode || j.getOpcode() < 0 && !(j instanceof org.objectweb.asm.tree.LineNumberNode)) {
                            break;
                        }
                    }
                }
            }
        }
        if (reads != 2 || !(store instanceof VarInsnNode)) {
            Asm.LOG.warn("P3 distortion effects: {}.setupCameraTransform is not of the expected shape (prevTimeInPortal read {} times); "
                    + "patch skipped, vanilla behavior kept.", RENDERER, reads);
            return bytes;
        }
        m.instructions.insertBefore(store, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "portalWobble", "(F)F", false));
        appliedP3 = true;
        Asm.LOG.info("P3 distortion effects: patched {}.", RENDERER);
        return Asm.write(node);
    }

    private static boolean isPrevTimeInPortal(FieldInsnNode f) {
        return ("prevTimeInPortal".equals(f.name) || "field_71080_cy".equals(f.name)) && "F".equals(f.desc);
    }

    private static byte[] entityDistance(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "setupTerrain", "func_174970_a",
                "(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V");
        if (m == null) {
            Asm.LOG.warn("P4 entity distance: {}.setupTerrain not found; patch skipped, vanilla behavior kept.", GLOBAL);
            return bytes;
        }
        MethodInsnNode call = null;
        int calls = 0;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode && i.getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode c = (MethodInsnNode) i;
                if ("net/minecraft/entity/Entity".equals(c.owner) && "(D)V".equals(c.desc)
                        && ("setRenderDistanceWeight".equals(c.name) || "func_184227_b".equals(c.name))) {
                    call = c;
                    calls++;
                }
            }
        }
        if (calls != 1) {
            Asm.LOG.warn("P4 entity distance: {}.setupTerrain calls setRenderDistanceWeight {} times, not once; "
                    + "patch skipped, vanilla behavior kept.", GLOBAL, calls);
            return bytes;
        }
        m.instructions.insertBefore(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "entityDistance", "(D)D", false));
        appliedP4 = true;
        Asm.LOG.info("P4 entity distance: patched {}.", GLOBAL);
        return Asm.write(node);
    }

    /** For the self-test: runs the patches on raw class bytes without marking them applied. */
    public static byte[] patchForTest(String transformedName, byte[] bytes) {
        boolean p3 = appliedP3;
        boolean p4 = appliedP4;
        byte[] out = RENDERER.equals(transformedName) ? portalWobble(bytes) : entityDistance(bytes);
        appliedP3 = p3;
        appliedP4 = p4;
        return out;
    }
}

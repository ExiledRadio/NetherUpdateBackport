package com.exiledradio.netherupdatebackport.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Small helpers shared by the patches. Every member is matched by its MCP name (the dev environment) or its
 * SRG name (a real instance), and by descriptor, never by position alone.
 */
final class Asm {

    static final Logger LOG = LogManager.getLogger("Nether Update Backport Core");

    private Asm() {
    }

    static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    /**
     * Writes a class back without recomputing frames: computing them would load classes from inside a
     * transformer. A patch that adds a jump adds its own frame ({@link #guardAtHead}).
     */
    static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    static MethodNode method(ClassNode node, String mcp, String srg, String desc) {
        for (MethodNode m : node.methods) {
            if ((mcp.equals(m.name) || srg.equals(m.name)) && desc.equals(m.desc)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Puts {@code if (Hooks.active()) { <body> }} at the very start of a method, where {@code body} must end
     * in a return. The rest of the method is untouched, so with the switch off it runs as vanilla's.
     *
     * <p>The jump lands on the method's first original instruction with the method's entry frame, which a
     * {@code F_SAME} frame describes exactly (no locals but the arguments, an empty stack). Frames already
     * in the method are relative to the one before them, and this one equals the implicit entry frame, so
     * they stay right.
     *
     * @return false (and nothing changed) if the method already starts on a frame, a shape no vanilla
     * method patched here has
     */
    static boolean guardAtHead(ClassNode owner, MethodNode m, String hooks, InsnList body) {
        AbstractInsnNode first = m.instructions.getFirst();
        for (AbstractInsnNode i = first; i != null && i.getOpcode() < 0; i = i.getNext()) {
            if (i instanceof FrameNode) {
                return false;
            }
        }
        LabelNode vanilla = new LabelNode();
        InsnList head = new InsnList();
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, hooks, "active", "()Z", false));
        head.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        head.add(body);
        head.add(vanilla);
        if ((owner.version & 0xFFFF) >= Opcodes.V1_6) {
            head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        }
        m.instructions.insert(head);
        return true;
    }
}

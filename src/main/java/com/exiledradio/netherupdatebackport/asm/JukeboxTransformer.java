package com.exiledradio.netherupdatebackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P2 (docs/PATCHES.md): a jukebox playing one of this mod's discs gives 1.16's comparator signal (Pigstep: 13).
 *
 * <p>1.12.2's {@code BlockJukebox.getComparatorInputOverride} counts the signal from the disc's item id
 * ({@code id + 1 - id(record_13)}), right for vanilla's twelve discs only. The head of the method becomes
 * <pre>
 * if (MusicHooks.active()) {
 *     int signal = MusicHooks.jukeboxSignal(world, pos);   // -1 unless the disc is ours
 *     if (signal >= 0) return signal;
 * }
 * ...vanilla's code, untouched
 * </pre>
 * so every other disc, and the jukebox with stream M off, is answered by vanilla's code.
 */
public final class JukeboxTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.block.BlockJukebox";
    private static final String HOOKS = "com/exiledradio/netherupdatebackport/music/MusicHooks";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode m = Asm.method(node, "getComparatorInputOverride", "func_180641_l",
                "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I");
        if (m == null || m.instructions.size() == 0 || startsOnFrame(m)) {
            Asm.LOG.warn("P2 jukebox comparator signal: {}.getComparatorInputOverride not found or of an unexpected shape; patch skipped.", TARGET);
            return basicClass;
        }
        boolean frames = (node.version & 0xFFFF) >= Opcodes.V1_6;
        LabelNode vanilla = new LabelNode();
        LabelNode notOurs = new LabelNode();
        InsnList head = new InsnList();
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "active", "()Z", false));
        head.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        head.add(new VarInsnNode(Opcodes.ALOAD, 2));
        head.add(new VarInsnNode(Opcodes.ALOAD, 3));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "jukeboxSignal",
                "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I", false));
        head.add(new InsnNode(Opcodes.DUP));
        head.add(new JumpInsnNode(Opcodes.IFLT, notOurs));
        head.add(new InsnNode(Opcodes.IRETURN));
        head.add(notOurs);
        if (frames) {
            // The entry frame with the signal still on the stack.
            head.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[] {Opcodes.INTEGER}));
        }
        head.add(new InsnNode(Opcodes.POP));
        head.add(vanilla);
        if (frames) {
            // Exactly the method's entry frame, so the method's own frames, relative to it, stay right.
            head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        }
        m.instructions.insert(head);
        applied = true;
        Asm.LOG.info("P2 jukebox comparator signal: patched {}.", TARGET);
        return Asm.write(node);
    }

    private static boolean startsOnFrame(MethodNode m) {
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null && i.getOpcode() < 0; i = i.getNext()) {
            if (i instanceof FrameNode) {
                return true;
            }
        }
        return false;
    }
}

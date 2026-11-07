package com.exiledradio.netherupdatebackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Patch P1 (docs/PATCHES.md): the zombie pigman's own anger code steps aside for 1.16's zombified piglin rules
 * (stream Z, {@code mobs.Anger}).
 *
 * <p>Six methods of {@code EntityPigZombie} get {@code if (PigmanHooks.active()) ...} at their head; the rest
 * of each method is vanilla's, so with stream Z switched off nothing changes:
 * <ul>
 *   <li>{@code becomeAngryAt(Entity)} returns at once. It is 1.12.2's whole anger: 20-40 s of anger at
 *       every player, started by any hit, and by the alarm of its own hurt-by task.</li>
 *   <li>{@code isAngry()} answers from 1.16's anger state (vanilla's own speed boost reads it).</li>
 *   <li>{@code isPreventingPlayerRest(EntityPlayer)} is 1.16's {@code isAngryAt(player)}, not "angry at
 *       anyone".</li>
 *   <li>{@code getAmbientSound()} is 1.16's: the angry grunt while angry, otherwise the ambient one;
 *       {@code getHurtSound} and {@code getDeathSound} are the zombified piglin's.</li>
 * </ul>
 */
public final class PigZombieTransformer implements IClassTransformer {

    private static final String TARGET = "net.minecraft.entity.monster.EntityPigZombie";
    private static final String HOOKS = "com/exiledradio/netherupdatebackport/mobs/PigmanHooks";
    private static final String PIGMAN = "Lnet/minecraft/entity/monster/EntityPigZombie;";
    private static final String PLAYER = "Lnet/minecraft/entity/player/EntityPlayer;";
    private static final String SOUND = "Lnet/minecraft/util/SoundEvent;";

    /** Set once the class has been patched; the self-test reads it. */
    public static volatile boolean applied;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!TARGET.equals(transformedName) || basicClass == null) {
            return basicClass;
        }
        ClassNode node = Asm.read(basicClass);
        MethodNode angryAt = Asm.method(node, "becomeAngryAt", "func_70835_c", "(Lnet/minecraft/entity/Entity;)V");
        MethodNode isAngry = Asm.method(node, "isAngry", "func_175457_ck", "()Z");
        MethodNode rest = Asm.method(node, "isPreventingPlayerRest", "func_191990_c", "(" + PLAYER + ")Z");
        MethodNode ambient = Asm.method(node, "getAmbientSound", "func_184639_G", "()" + SOUND);
        MethodNode hurt = Asm.method(node, "getHurtSound", "func_184601_bQ", "(Lnet/minecraft/util/DamageSource;)" + SOUND);
        MethodNode death = Asm.method(node, "getDeathSound", "func_184615_bR", "()" + SOUND);
        if (angryAt == null || isAngry == null || rest == null || ambient == null || hurt == null || death == null) {
            Asm.LOG.warn("P1 zombified piglin anger: a method of {} was not found; patch skipped.", TARGET);
            return basicClass;
        }

        InsnList ret = new InsnList();
        ret.add(new InsnNode(Opcodes.RETURN));

        InsnList angry = new InsnList();
        angry.add(new VarInsnNode(Opcodes.ALOAD, 0));
        angry.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "isAngry", "(" + PIGMAN + ")Z", false));
        angry.add(new InsnNode(Opcodes.IRETURN));

        InsnList angryAtPlayer = new InsnList();
        angryAtPlayer.add(new VarInsnNode(Opcodes.ALOAD, 0));
        angryAtPlayer.add(new VarInsnNode(Opcodes.ALOAD, 1));
        angryAtPlayer.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "isAngryAt", "(" + PIGMAN + PLAYER + ")Z", false));
        angryAtPlayer.add(new InsnNode(Opcodes.IRETURN));

        InsnList ambientSound = new InsnList();
        ambientSound.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ambientSound.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "ambientSound", "(" + PIGMAN + ")" + SOUND, false));
        ambientSound.add(new InsnNode(Opcodes.ARETURN));

        InsnList hurtSound = new InsnList();
        hurtSound.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "hurtSound", "()" + SOUND, false));
        hurtSound.add(new InsnNode(Opcodes.ARETURN));

        InsnList deathSound = new InsnList();
        deathSound.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "deathSound", "()" + SOUND, false));
        deathSound.add(new InsnNode(Opcodes.ARETURN));

        // Check every method first, so the class is either fully patched or left as it was.
        MethodNode[] methods = {angryAt, isAngry, rest, ambient, hurt, death};
        InsnList[] bodies = {ret, angry, angryAtPlayer, ambientSound, hurtSound, deathSound};
        for (MethodNode m : methods) {
            if (m.instructions.size() == 0 || startsOnFrame(m)) {
                Asm.LOG.warn("P1 zombified piglin anger: {} has an unexpected shape; patch skipped.", m.name);
                return basicClass;
            }
        }
        for (int i = 0; i < methods.length; i++) {
            Asm.guardAtHead(node, methods[i], HOOKS, bodies[i]);
        }
        applied = true;
        Asm.LOG.info("P1 zombified piglin anger: patched {}.", TARGET);
        return Asm.write(node);
    }

    private static boolean startsOnFrame(MethodNode m) {
        for (org.objectweb.asm.tree.AbstractInsnNode i = m.instructions.getFirst(); i != null && i.getOpcode() < 0; i = i.getNext()) {
            if (i instanceof org.objectweb.asm.tree.FrameNode) {
                return true;
            }
        }
        return false;
    }
}

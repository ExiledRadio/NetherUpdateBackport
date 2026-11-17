package com.exiledradio.netherupdatebackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Patches P12 to P26 (docs/PATCHES.md), stream C: 1.16's smaller changes to how existing things behave, the low
 * impact half of the 2026-11-16 audit. Every hook is in {@code changes.SmallHooks} and reads its setting on each
 * call, so a switched-off change runs vanilla's code. Each patch is skipped, with a warning, when its method is
 * not of the expected shape.
 *
 * <ul>
 *   <li><b>P12</b>, {@code BlockSkull}: the wither patterns' soul sand matcher also takes soul soil.</li>
 *   <li><b>P13</b>, {@code WalkNodeProcessor.checkNeighborBlocks}: magma next to a walkable node is a fire danger.</li>
 *   <li><b>P14</b>, {@code EnumEnchantmentType.DIGGER}: hoes take Efficiency, Fortune and Silk Touch; and
 *       {@code ItemHoe} gets a {@code getItemEnchantability} (its material's), so the table enchants it.</li>
 *   <li><b>P15</b>, {@code Enchantment.canApply}: Curse of Vanishing goes on compasses.</li>
 *   <li><b>P16</b>, {@code EntityParrot}: 1.16's imitation rate, on its own and on a shoulder.</li>
 *   <li><b>P17</b>, {@code EntityPotion.isWaterSensitiveEntity}: snow golems too.</li>
 *   <li><b>P18</b>, {@code AbstractHorse.canBeLeashedTo}: undead horses can be leashed.</li>
 *   <li><b>P19</b>, {@code BlockWall.canConnectTo}: walls connect to panes and iron bars (and so they to walls).</li>
 *   <li><b>P20</b>, {@code BlockFurnace.breakBlock}: the output's XP comes out with the items.</li>
 *   <li><b>P21</b>, {@code BlockFarmland} and {@code BlockGrassPath}: a {@code causesSuffocation} of their own.</li>
 *   <li><b>P22</b>, {@code EntityGhast.getSoundVolume}: 5, not 10.</li>
 *   <li><b>P23</b>, {@code ItemArmorStand.onItemUse}: grass or snow it is placed in stays.</li>
 *   <li><b>P24</b>, {@code EntityAIHurtByTarget.shouldExecute}: with {@code universalAnger}, a player's hit is
 *       left to the anger reset.</li>
 *   <li><b>P25</b>, {@code Entity.move}: climbing counts as steps on every climbable block, not ladders only.</li>
 * </ul>
 */
public final class SmallChangesTransformer implements IClassTransformer {

    private static final String HOOKS = "com/exiledradio/netherupdatebackport/changes/SmallHooks";

    private static final String D_STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String D_ACCESS = "Lnet/minecraft/world/IBlockAccess;";
    private static final String D_POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String D_FACING = "Lnet/minecraft/util/EnumFacing;";
    private static final String D_WORLD = "Lnet/minecraft/world/World;";
    private static final String D_ENTITY = "Lnet/minecraft/entity/Entity;";
    private static final String D_BLOCK = "Lnet/minecraft/block/Block;";
    private static final String D_STACK = "Lnet/minecraft/item/ItemStack;";

    /** Set once each patch is in; the self-test reads them. */
    public static volatile boolean appliedP12;
    public static volatile boolean appliedP13;
    public static volatile boolean appliedP14;
    public static volatile boolean appliedP15;
    public static volatile boolean appliedP16;
    public static volatile boolean appliedP17;
    public static volatile boolean appliedP18;
    public static volatile boolean appliedP19;
    public static volatile boolean appliedP20;
    /** P14's second half: {@code ItemHoe.getItemEnchantability}. */
    public static volatile boolean appliedP14Hoe;
    /** P21 has two classes; one count per class patched. */
    public static volatile int appliedP21;
    public static volatile boolean appliedP22;
    public static volatile boolean appliedP23;
    public static volatile boolean appliedP24;
    public static volatile boolean appliedP25;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null) {
            return basicClass;
        }
        switch (transformedName) {
            case "net.minecraft.block.BlockSkull":
                return skull(basicClass);
            case "net.minecraft.pathfinding.WalkNodeProcessor":
                return walkNodes(basicClass);
            case "net.minecraft.enchantment.Enchantment":
                return enchantment(basicClass);
            case "net.minecraft.entity.passive.EntityParrot":
                return parrot(basicClass);
            case "net.minecraft.entity.projectile.EntityPotion":
                return potion(basicClass);
            case "net.minecraft.entity.passive.AbstractHorse":
                return horse(basicClass);
            case "net.minecraft.block.BlockWall":
                return wall(basicClass);
            case "net.minecraft.block.BlockFurnace":
                return furnace(basicClass);
            case "net.minecraft.block.BlockFarmland":
            case "net.minecraft.block.BlockGrassPath":
                return suffocation(transformedName, basicClass);
            case "net.minecraft.entity.monster.EntityGhast":
                return ghast(basicClass);
            case "net.minecraft.item.ItemArmorStand":
                return armorStand(basicClass);
            case "net.minecraft.entity.ai.EntityAIHurtByTarget":
                return hurtByTarget(basicClass);
            case "net.minecraft.entity.Entity":
                return entity(basicClass);
            case "net.minecraft.item.ItemHoe":
                return hoe(basicClass);
            default:
                if (transformedName.startsWith("net.minecraft.enchantment.EnumEnchantmentType$")) {
                    return enchantmentType(transformedName, basicClass);
                }
                return basicClass;
        }
    }

    // P12 ---------------------------------------------------------------------------------------------------

    private static byte[] skull(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        int found = 0;
        for (String[] names : new String[][] {{"getWitherBasePattern", "func_176414_j"}, {"getWitherPattern", "func_176416_l"}}) {
            MethodNode m = Asm.method(node, names[0], names[1], "()Lnet/minecraft/block/state/pattern/BlockPattern;");
            if (m == null) {
                continue;
            }
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof MethodInsnNode && isCall((MethodInsnNode) i, "net/minecraft/block/state/pattern/BlockStateMatcher",
                        "forBlock", "func_177638_a", "(" + D_BLOCK + ")Lnet/minecraft/block/state/pattern/BlockStateMatcher;")) {
                    AbstractInsnNode before = previous(i);
                    if (before instanceof FieldInsnNode && ((FieldInsnNode) before).owner.equals("net/minecraft/init/Blocks")
                            && (((FieldInsnNode) before).name.equals("SOUL_SAND") || ((FieldInsnNode) before).name.equals("field_150425_aM"))) {
                        MethodInsnNode call = (MethodInsnNode) i;
                        call.owner = HOOKS;
                        call.name = "witherBase";
                        call.desc = "(" + D_BLOCK + ")Lcom/google/common/base/Predicate;";
                        found++;
                    }
                }
            }
        }
        if (found != 2) {
            Asm.LOG.warn("P12 withers on soul soil: found {} soul sand matchers in BlockSkull, not 2; patch skipped.", found);
            return bytes;
        }
        appliedP12 = true;
        Asm.LOG.info("P12 withers on soul soil: patched BlockSkull.");
        return Asm.write(node);
    }

    // P13 ---------------------------------------------------------------------------------------------------

    private static byte[] walkNodes(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "checkNeighborBlocks", "func_193578_a",
                "(" + D_ACCESS + "IIILnet/minecraft/pathfinding/PathNodeType;)Lnet/minecraft/pathfinding/PathNodeType;");
        List<AbstractInsnNode> returns = m == null ? new ArrayList<AbstractInsnNode>() : opcodes(m, Opcodes.ARETURN);
        if (returns.size() != 1) {
            Asm.LOG.warn("P13 mobs avoid magma: WalkNodeProcessor.checkNeighborBlocks not of the expected shape; patch skipped.");
            return bytes;
        }
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 1));
        call.add(new VarInsnNode(Opcodes.ILOAD, 2));
        call.add(new VarInsnNode(Opcodes.ILOAD, 3));
        call.add(new VarInsnNode(Opcodes.ILOAD, 4));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "neighbors",
                "(Lnet/minecraft/pathfinding/PathNodeType;" + D_ACCESS + "III)Lnet/minecraft/pathfinding/PathNodeType;", false));
        m.instructions.insertBefore(returns.get(0), call);
        appliedP13 = true;
        Asm.LOG.info("P13 mobs avoid magma: patched WalkNodeProcessor.checkNeighborBlocks.");
        return Asm.write(node);
    }

    // P14 ---------------------------------------------------------------------------------------------------

    /** The enum constant whose test is {@code item instanceof ItemTool} is DIGGER; the others are left alone. */
    private static byte[] enchantmentType(String name, byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "canEnchantItem", "func_77557_a", "(Lnet/minecraft/item/Item;)Z");
        if (m == null) {
            return bytes;
        }
        boolean digger = false;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getOpcode() == Opcodes.INSTANCEOF && "net/minecraft/item/ItemTool".equals(((TypeInsnNode) i).desc)) {
                digger = true;
            }
        }
        if (!digger) {
            return bytes;
        }
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 1));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "hoeDigger", "(Lnet/minecraft/item/Item;)Z", false));
        if (!Asm.ifAtHead(node, m, test, returnTrue())) {
            Asm.LOG.warn("P14 hoe enchantments: {}.canEnchantItem starts on a frame; patch skipped.", name);
            return bytes;
        }
        appliedP14 = true;
        Asm.LOG.info("P14 hoe enchantments: patched {} (DIGGER).", name);
        return Asm.write(node);
    }

    /**
     * 1.16's hoe is a {@code TieredItem}: its enchantability is its tier's. 1.12.2's {@code ItemHoe} keeps
     * {@code Item}'s 0, which the enchanting table takes as "cannot be enchanted". Named as the class's own
     * {@code onItemUse} shows the environment to be.
     */
    private static byte[] hoe(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode use = null;
        for (MethodNode m : node.methods) {
            if (("onItemUse".equals(m.name) || "func_180614_a".equals(m.name)) && m.desc.endsWith(")Lnet/minecraft/util/EnumActionResult;")) {
                use = m;
            }
        }
        if (use == null || Asm.method(node, "getItemEnchantability", "func_77619_b", "()I") != null) {
            Asm.LOG.warn("P14 hoe enchantments: ItemHoe has no onItemUse or already an enchantability; enchantability skipped.");
            return bytes;
        }
        String name = use.name.startsWith("func_") ? "func_77619_b" : "getItemEnchantability";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, name, "()I", null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "hoeEnchantability", "(Lnet/minecraft/item/ItemHoe;)I", false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        m.maxStack = 1;
        m.maxLocals = 1;
        node.methods.add(m);
        appliedP14Hoe = true;
        Asm.LOG.info("P14 hoe enchantments: added ItemHoe.{}.", name);
        return Asm.write(node);
    }

    // P15 ---------------------------------------------------------------------------------------------------

    private static byte[] enchantment(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "canApply", "func_92089_a", "(" + D_STACK + ")Z");
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 0));
        test.add(new VarInsnNode(Opcodes.ALOAD, 1));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "vanishingCompass",
                "(Lnet/minecraft/enchantment/Enchantment;" + D_STACK + ")Z", false));
        if (m == null || !Asm.ifAtHead(node, m, test, returnTrue())) {
            Asm.LOG.warn("P15 compass Curse of Vanishing: Enchantment.canApply not of the expected shape; patch skipped.");
            return bytes;
        }
        appliedP15 = true;
        Asm.LOG.info("P15 compass Curse of Vanishing: patched Enchantment.canApply.");
        return Asm.write(node);
    }

    // P16 ---------------------------------------------------------------------------------------------------

    private static byte[] parrot(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode living = Asm.method(node, "onLivingUpdate", "func_70636_d", "()V");
        MethodNode ambient = Asm.method(node, "playAmbientSound", "func_192005_a", "(" + D_WORLD + D_ENTITY + ")V");
        List<MethodInsnNode> mimics = living == null ? new ArrayList<MethodInsnNode>()
                : calls(living, node.name, "playMimicSound", "func_192006_b", "(" + D_WORLD + D_ENTITY + ")Z");
        if (ambient == null || mimics.size() != 1) {
            Asm.LOG.warn("P16 parrot imitation: EntityParrot not of the expected shape; patch skipped.");
            return bytes;
        }
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new VarInsnNode(Opcodes.ALOAD, 1));
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "shoulderParrot", "(" + D_WORLD + D_ENTITY + ")V", false));
        body.add(new InsnNode(Opcodes.RETURN));
        if (!Asm.guardAtHead(node, ambient, HOOKS, "parrotActive", body)) {
            Asm.LOG.warn("P16 parrot imitation: EntityParrot.playAmbientSound starts on a frame; patch skipped.");
            return bytes;
        }
        MethodInsnNode call = mimics.get(0);
        call.owner = HOOKS;
        call.name = "parrotTick";
        appliedP16 = true;
        Asm.LOG.info("P16 parrot imitation: patched EntityParrot.");
        return Asm.write(node);
    }

    // P17 ---------------------------------------------------------------------------------------------------

    private static byte[] potion(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "isWaterSensitiveEntity", "func_190544_c", "(Lnet/minecraft/entity/EntityLivingBase;)Z");
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 0));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "waterSensitive", "(Lnet/minecraft/entity/EntityLivingBase;)Z", false));
        if (m == null || !Asm.ifAtHead(node, m, test, returnTrue())) {
            Asm.LOG.warn("P17 snow golems and water bottles: EntityPotion.isWaterSensitiveEntity not of the expected shape; patch skipped.");
            return bytes;
        }
        appliedP17 = true;
        Asm.LOG.info("P17 snow golems and water bottles: patched EntityPotion.");
        return Asm.write(node);
    }

    // P18 ---------------------------------------------------------------------------------------------------

    private static byte[] horse(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "canBeLeashedTo", "func_184652_a", "(Lnet/minecraft/entity/player/EntityPlayer;)Z");
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "canLeash", "(Lnet/minecraft/entity/passive/AbstractHorse;)Z", false));
        body.add(new InsnNode(Opcodes.IRETURN));
        if (m == null || !Asm.guardAtHead(node, m, HOOKS, "leashActive", body)) {
            Asm.LOG.warn("P18 undead horse leash: AbstractHorse.canBeLeashedTo not of the expected shape; patch skipped.");
            return bytes;
        }
        appliedP18 = true;
        Asm.LOG.info("P18 undead horse leash: patched AbstractHorse.canBeLeashedTo.");
        return Asm.write(node);
    }

    // P19 ---------------------------------------------------------------------------------------------------

    private static byte[] wall(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "canConnectTo", "func_176253_e", "(" + D_ACCESS + D_POS + D_FACING + ")Z");
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 1));
        test.add(new VarInsnNode(Opcodes.ALOAD, 2));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "wallToPane", "(" + D_ACCESS + D_POS + ")Z", false));
        if (m == null || !Asm.ifAtHead(node, m, test, returnTrue())) {
            Asm.LOG.warn("P19 walls and panes: BlockWall.canConnectTo not of the expected shape; patch skipped.");
            return bytes;
        }
        appliedP19 = true;
        Asm.LOG.info("P19 walls and panes: patched BlockWall.canConnectTo.");
        return Asm.write(node);
    }

    // P20 ---------------------------------------------------------------------------------------------------

    private static byte[] furnace(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "breakBlock", "func_180663_b", "(" + D_WORLD + D_POS + D_STATE + ")V");
        List<MethodInsnNode> drops = m == null ? new ArrayList<MethodInsnNode>()
                : calls(m, "net/minecraft/inventory/InventoryHelper", "dropInventoryItems", "func_180175_a",
                "(" + D_WORLD + D_POS + "Lnet/minecraft/inventory/IInventory;)V");
        if (drops.size() != 1) {
            Asm.LOG.warn("P20 furnace XP: BlockFurnace.breakBlock not of the expected shape; patch skipped.");
            return bytes;
        }
        // Before the items drop (they are still in the furnace then), inside vanilla's !keepInventory branch.
        AbstractInsnNode start = drops.get(0);
        for (int n = 0; n < 6 && !(start instanceof VarInsnNode && start.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) start).var == 1); n++) {
            start = previous(start);
        }
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 1));
        call.add(new VarInsnNode(Opcodes.ALOAD, 2));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "furnaceBroken", "(" + D_WORLD + D_POS + ")V", false));
        if (!(start instanceof VarInsnNode) || ((VarInsnNode) start).var != 1) {
            Asm.LOG.warn("P20 furnace XP: BlockFurnace.breakBlock's drop does not start with the world; patch skipped.");
            return bytes;
        }
        m.instructions.insertBefore(start, call);
        appliedP20 = true;
        Asm.LOG.info("P20 furnace XP: patched BlockFurnace.breakBlock.");
        return Asm.write(node);
    }

    // P21 ---------------------------------------------------------------------------------------------------

    /**
     * Adds {@code causesSuffocation(IBlockState)}, named as the class's own {@code isFullCube} override shows the
     * environment to be (MCP or SRG).
     */
    private static byte[] suffocation(String name, byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode full = Asm.method(node, "isFullCube", "func_149686_d", "(" + D_STATE + ")Z");
        if (full == null || Asm.method(node, "causesSuffocation", "func_176214_u", "(" + D_STATE + ")Z") != null) {
            Asm.LOG.warn("P21 suffocation: {} has no isFullCube or already a causesSuffocation; patch skipped.", name);
            return bytes;
        }
        String method = full.name.startsWith("func_") ? "func_176214_u" : "causesSuffocation";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, method, "(" + D_STATE + ")Z", null, null);
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "suffocates", "()Z", false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        m.maxStack = 1;
        m.maxLocals = 2;
        node.methods.add(m);
        appliedP21++;
        Asm.LOG.info("P21 suffocation: added {}.{}.", name, method);
        return Asm.write(node);
    }

    // P22 ---------------------------------------------------------------------------------------------------

    private static byte[] ghast(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "getSoundVolume", "func_70599_aP", "()F");
        InsnList body = new InsnList();
        body.add(new LdcInsnNode(5.0F));
        body.add(new InsnNode(Opcodes.FRETURN));
        if (m == null || !Asm.guardAtHead(node, m, HOOKS, "ghastActive", body)) {
            Asm.LOG.warn("P22 ghast sound range: EntityGhast.getSoundVolume not of the expected shape; patch skipped.");
            return bytes;
        }
        appliedP22 = true;
        Asm.LOG.info("P22 ghast sound range: patched EntityGhast.getSoundVolume.");
        return Asm.write(node);
    }

    // P23 ---------------------------------------------------------------------------------------------------

    private static byte[] armorStand(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "onItemUse", "func_180614_a",
                "(Lnet/minecraft/entity/player/EntityPlayer;" + D_WORLD + D_POS + "Lnet/minecraft/util/EnumHand;" + D_FACING
                        + "FFF)Lnet/minecraft/util/EnumActionResult;");
        List<MethodInsnNode> clears = m == null ? new ArrayList<MethodInsnNode>()
                : calls(m, "net/minecraft/world/World", "setBlockToAir", "func_175698_g", "(" + D_POS + ")Z");
        if (clears.size() != 2) {
            Asm.LOG.warn("P23 armor stands in grass and snow: ItemArmorStand.onItemUse not of the expected shape; patch skipped.");
            return bytes;
        }
        for (MethodInsnNode call : clears) {
            call.setOpcode(Opcodes.INVOKESTATIC);
            call.owner = HOOKS;
            call.name = "armorStandClear";
            call.desc = "(" + D_WORLD + D_POS + ")Z";
            call.itf = false;
        }
        appliedP23 = true;
        Asm.LOG.info("P23 armor stands in grass and snow: patched ItemArmorStand.onItemUse.");
        return Asm.write(node);
    }

    // P24 ---------------------------------------------------------------------------------------------------

    private static byte[] hurtByTarget(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "shouldExecute", "func_75250_a", "()Z");
        FieldInsnNode owner = null;
        if (m != null) {
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null && owner == null; i = i.getNext()) {
                if (i.getOpcode() == Opcodes.GETFIELD && "Lnet/minecraft/entity/EntityCreature;".equals(((FieldInsnNode) i).desc)) {
                    owner = (FieldInsnNode) i;
                }
            }
        }
        if (owner == null) {
            Asm.LOG.warn("P24 universal anger: EntityAIHurtByTarget.shouldExecute not of the expected shape; patch skipped.");
            return bytes;
        }
        InsnList test = new InsnList();
        test.add(new VarInsnNode(Opcodes.ALOAD, 0));
        test.add(new FieldInsnNode(Opcodes.GETFIELD, owner.owner, owner.name, owner.desc));
        test.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "ignoreHurtByPlayer", "(Lnet/minecraft/entity/EntityCreature;)Z", false));
        InsnList body = new InsnList();
        body.add(new InsnNode(Opcodes.ICONST_0));
        body.add(new InsnNode(Opcodes.IRETURN));
        if (!Asm.ifAtHead(node, m, test, body)) {
            Asm.LOG.warn("P24 universal anger: EntityAIHurtByTarget.shouldExecute starts on a frame; patch skipped.");
            return bytes;
        }
        appliedP24 = true;
        Asm.LOG.info("P24 universal anger: patched EntityAIHurtByTarget.shouldExecute.");
        return Asm.write(node);
    }

    // P25 ---------------------------------------------------------------------------------------------------

    /** {@code if (block != Blocks.LADDER) d16 = 0}: the comparison becomes {@code SmallHooks.climbable(block, LADDER)}. */
    private static byte[] entity(byte[] bytes) {
        ClassNode node = Asm.read(bytes);
        MethodNode m = Asm.method(node, "move", "func_70091_d", "(Lnet/minecraft/entity/MoverType;DDD)V");
        List<JumpInsnNode> found = new ArrayList<JumpInsnNode>();
        if (m != null) {
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof FieldInsnNode && i.getOpcode() == Opcodes.GETSTATIC && "net/minecraft/init/Blocks".equals(((FieldInsnNode) i).owner)
                        && ("LADDER".equals(((FieldInsnNode) i).name) || "field_150468_ap".equals(((FieldInsnNode) i).name))) {
                    AbstractInsnNode next = next(i);
                    if (next != null && next.getOpcode() == Opcodes.IF_ACMPEQ) {
                        found.add((JumpInsnNode) next);
                    }
                }
            }
        }
        if (found.size() != 1) {
            Asm.LOG.warn("P25 climbing steps: Entity.move has {} ladder tests, not 1; patch skipped.", found.size());
            return bytes;
        }
        JumpInsnNode jump = found.get(0);
        m.instructions.insertBefore(jump, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "climbable", "(" + D_BLOCK + D_BLOCK + ")Z", false));
        jump.setOpcode(Opcodes.IFNE);
        appliedP25 = true;
        Asm.LOG.info("P25 climbing steps: patched Entity.move.");
        return Asm.write(node);
    }

    // -------------------------------------------------------------------------------------------------------

    private static InsnList returnTrue() {
        InsnList body = new InsnList();
        body.add(new InsnNode(Opcodes.ICONST_1));
        body.add(new InsnNode(Opcodes.IRETURN));
        return body;
    }

    private static boolean isCall(MethodInsnNode c, String owner, String mcp, String srg, String desc) {
        return owner.equals(c.owner) && desc.equals(c.desc) && (mcp.equals(c.name) || srg.equals(c.name));
    }

    private static List<MethodInsnNode> calls(MethodNode m, String owner, String mcp, String srg, String desc) {
        List<MethodInsnNode> out = new ArrayList<MethodInsnNode>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof MethodInsnNode && isCall((MethodInsnNode) i, owner, mcp, srg, desc)) {
                out.add((MethodInsnNode) i);
            }
        }
        return out;
    }

    private static List<AbstractInsnNode> opcodes(MethodNode m, int opcode) {
        List<AbstractInsnNode> out = new ArrayList<AbstractInsnNode>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getOpcode() == opcode) {
                out.add(i);
            }
        }
        return out;
    }

    /** @return the previous real instruction (labels, lines and frames skipped) */
    private static AbstractInsnNode previous(AbstractInsnNode i) {
        AbstractInsnNode p = i.getPrevious();
        while (p != null && p.getOpcode() < 0) {
            p = p.getPrevious();
        }
        return p;
    }

    /** @return the next real instruction */
    private static AbstractInsnNode next(AbstractInsnNode i) {
        AbstractInsnNode n = i.getNext();
        while (n != null && n.getOpcode() < 0) {
            n = n.getNext();
        }
        return n;
    }
}

package org.edtp.entitycollisionoptimizer.collision.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.service.MixinService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uniform field access boundary; consumer mixins declare coverage, not per-consumer behavior.
 * Matches both Mojang (dev) and intermediary (production) names — string constants are not remapped.
 */
public final class BodyFieldAccess {
    private static final String ENTITY_NAMED = "net/minecraft/world/entity/Entity";
    private static final String ENTITY_INTERMEDIARY = "net/minecraft/class_1297";
    private static final String VECTOR_NAMED = "Lnet/minecraft/world/phys/Vec3;";
    private static final String VECTOR_INTERMEDIARY = "Lnet/minecraft/class_243;";
    private static final String AABB_NAMED = "Lnet/minecraft/world/phys/AABB;";
    private static final String AABB_INTERMEDIARY = "Lnet/minecraft/class_238;";
    private static final String ACCESS = "org/edtp/entitycollisionoptimizer/collision/CollisionBodyAccess";
    private static final Map<String, Boolean> ENTITY_TYPES = new ConcurrentHashMap<>();

    private static boolean isEntityName(String name) {
        return name.equals(ENTITY_NAMED) || name.equals(ENTITY_INTERMEDIARY);
    }

    private static boolean isVectorDesc(String desc) {
        return desc.equals(VECTOR_NAMED) || desc.equals(VECTOR_INTERMEDIARY);
    }

    private static boolean isAabbDesc(String desc) {
        return desc.equals(AABB_NAMED) || desc.equals(AABB_INTERMEDIARY);
    }

    private static boolean isPositionName(String name) {
        return name.equals("position") || name.equals("field_22467");
    }

    private static boolean isDeltaMovementName(String name) {
        return name.equals("deltaMovement") || name.equals("field_18276");
    }

    private static boolean isBbName(String name) {
        return name.equals("bb") || name.equals("field_6005");
    }

    private static boolean isNoPhysicsName(String name) {
        return name.equals("noPhysics") || name.equals("field_5960");
    }

    private static boolean isNeedsSyncName(String name) {
        return name.equals("needsSync");
    }

    public static void rewrite(ClassNode node) {
        boolean entityClass = isEntityName(node.name);
        if (entityClass && node.fields.stream().noneMatch(field ->
                isDeltaMovementName(field.name) && isVectorDesc(field.desc)
                        && (field.access & Opcodes.ACC_PRIVATE) != 0)) {
            throw new IllegalStateException("Expected private Entity velocity field (pos=" + node.name + ")");
        }
        if (entityClass && node.fields.stream().noneMatch(field ->
                isPositionName(field.name) && isVectorDesc(field.desc)
                        && (field.access & Opcodes.ACC_PRIVATE) != 0)) {
            throw new IllegalStateException("Expected private Entity position field (pos=" + node.name + ")");
        }
        int writes = 0, positionWrites = 0, boundsWrites = 0;
        for (var method : node.methods) {
            // Only these storage primitives may physically touch the unbound field.
            if (entityClass && (method.name.equals("eco$readVelocity") || method.name.equals("eco$writeVelocity")
                    || method.name.equals("eco$publishVelocity")
                    || method.name.equals("eco$readNeedsSync") || method.name.equals("eco$writeNeedsSync")
                    || method.name.equals("eco$writeNoPhysics") || method.name.equals("eco$writePosition")
                    || method.name.equals("eco$readPosition")
                    || method.name.equals("eco$readBounds") || method.name.equals("eco$writeBounds")
                    || method.name.equals("eco$detachBody"))) continue;
            for (var instruction : method.instructions.toArray()) {
                if (!(instruction instanceof FieldInsnNode field)) continue;
                // Reads stay as GETFIELD (fast path). Only writes are redirected so the native row stays in sync.
                if (field.getOpcode() == Opcodes.GETFIELD) continue;
                boolean ownerEntity = isEntityName(field.owner);
                boolean velocity = ownerEntity && isDeltaMovementName(field.name) && isVectorDesc(field.desc);
                boolean position = ownerEntity && isPositionName(field.name) && isVectorDesc(field.desc);
                boolean bounds = ownerEntity && isBbName(field.name) && isAabbDesc(field.desc);
                boolean sync = isNeedsSyncName(field.name) && field.desc.equals("Z");
                boolean physics = isNoPhysicsName(field.name) && field.desc.equals("Z");
                if (!velocity && !position && !bounds && !((sync || physics) && isEntity(field.owner, node))) continue;
                if (field.getOpcode() != Opcodes.PUTFIELD) {
                    throw new IllegalStateException("Unexpected collision body field opcode");
                }
                String name = velocity ? "eco$writeVelocity"
                        : position ? "eco$writePosition"
                        : bounds ? "eco$writeBounds"
                        : sync ? "eco$writeNeedsSync" : "eco$writeNoPhysics";
                // Keep the runtime descriptor so the call matches the remapped interface.
                method.instructions.set(field, new MethodInsnNode(Opcodes.INVOKEINTERFACE, ACCESS,
                        name, "(" + field.desc + ")V", true));
                if (velocity) writes++;
                if (position) positionWrites++;
                if (bounds) boundsWrites++;
            }
        }
        // Constructor/setters must cover the stores; missing coverage is not a fallback.
        if (entityClass && writes < 2) throw new IllegalStateException("Incomplete Entity velocity access rewrite");
        if (entityClass && positionWrites < 2) throw new IllegalStateException("Incomplete Entity position write rewrite");
        if (entityClass && boundsWrites < 2) throw new IllegalStateException("Incomplete Entity bounding box write rewrite");
    }

    private static boolean isEntity(String type, ClassNode current) {
        if (isEntityName(type)) return true;
        if (type.equals("java/lang/Object")) return false;
        Boolean cached = ENTITY_TYPES.get(type);
        if (cached != null) return cached;
        try {
            ClassNode owner = type.equals(current.name) ? current : MixinService.getService()
                    .getBytecodeProvider().getClassNode(type, false);
            boolean result = owner.superName != null && isEntity(owner.superName, current);
            ENTITY_TYPES.put(type, result);
            return result;
        } catch (java.io.IOException | ClassNotFoundException failure) {
            throw new IllegalStateException("Cannot resolve collision field owner " + type, failure);
        }
    }

    private BodyFieldAccess() {}
}

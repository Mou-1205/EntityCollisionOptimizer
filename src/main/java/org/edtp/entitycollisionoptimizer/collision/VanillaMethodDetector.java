package org.edtp.entitycollisionoptimizer.collision;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Detects vanilla collision method ownership via reflection.
 * Accepts both Mojang (dev) and intermediary (production) names — remapJar does not rename string constants.
 */
public final class VanillaMethodDetector {

    private VanillaMethodDetector() {
    }

    private static final ClassValue<Boolean> USE_VANILLA_GET_TEAM =
            declaringClass(Entity.class, new Class<?>[0], "getTeam", "method_5781");
    private static final ClassValue<Boolean> USE_VANILLA_CAN_BE_COLLIDED_WITH =
            declaringClass(Entity.class, new Class<?>[]{Entity.class}, "canBeCollidedWith", "method_30948");
    private static final ClassValue<Boolean> USE_VANILLA_CAN_COLLIDE_WITH =
            declaringClass(Entity.class, new Class<?>[]{Entity.class}, "canCollideWith", "method_30949");
    private static final ClassValue<Boolean> USE_VANILLA_DO_PUSH =
            declaringClass(LivingEntity.class, new Class<?>[]{Entity.class}, "doPush", "method_6087");
    private static final ClassValue<Boolean> USE_VANILLA_VECTOR_PUSH =
            declaringClass(Entity.class, new Class<?>[]{double.class, double.class, double.class},
                    "push", "method_5762");
    private static final ClassValue<Boolean> USE_VANILLA_VELOCITY_GETTER =
            declaringClass(Entity.class, new Class<?>[0], "getDeltaMovement", "method_18798");
    private static final ClassValue<Boolean> USE_VANILLA_VELOCITY_SETTER =
            declaringClass(Entity.class, new Class<?>[]{Vec3.class}, "setDeltaMovement", "method_18799");

    private static final ClassValue<Boolean> USE_VANILLA_ENTITY_PUSH = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            // LivingEntity only adds a sleeping guard, which the native batch applies live.
            Class<?> owner = findDeclaringClass(type, new Class<?>[]{Entity.class}, "push", "method_5697");
            return owner == Entity.class || owner == LivingEntity.class;
        }
    };

    /** Only Entity's scoreboard lookup is revision-cached; derived vanilla teams are read live. */
    public static boolean usesVanillaGetTeam(Entity entity) {
        return USE_VANILLA_GET_TEAM.get(entity.getClass());
    }

    /** Entity's default canBeCollidedWith is always false; boats/shulkers/etc. override it. */
    public static boolean usesVanillaCanBeCollidedWith(Entity entity) {
        return USE_VANILLA_CAN_BE_COLLIDED_WITH.get(entity.getClass());
    }

    /** Entity.canCollideWith only keeps hard targets; boats also keep pushable entities. */
    public static boolean usesVanillaCanCollideWith(Entity entity) {
        return USE_VANILLA_CAN_COLLIDE_WITH.get(entity.getClass());
    }

    public static boolean usesVanillaDoPush(LivingEntity source) {
        return USE_VANILLA_DO_PUSH.get(source.getClass());
    }

    public static boolean usesVanillaEntityPush(Entity entity) {
        return USE_VANILLA_ENTITY_PUSH.get(entity.getClass());
    }

    public static boolean allowsDeferredVelocityWrites(Entity entity) {
        Class<?> type = entity.getClass();
        // A native run may defer writes only across ordinary, non-observing velocity accessors.
        return USE_VANILLA_VECTOR_PUSH.get(type) && USE_VANILLA_VELOCITY_GETTER.get(type)
                && USE_VANILLA_VELOCITY_SETTER.get(type);
    }

    private static Class<?> findDeclaringClass(Class<?> type, Class<?>[] parameterTypes, String... candidateNames) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (String name : candidateNames) {
                try {
                    return current.getDeclaredMethod(name, parameterTypes).getDeclaringClass();
                } catch (NoSuchMethodException ignored) {
                }
            }
        }
        return null;
    }

    private static ClassValue<Boolean> declaringClass(
            Class<?> expectedOwner,
            Class<?>[] parameterTypes,
            String... candidateNames
    ) {
        return new ClassValue<>() {
            @Override
            protected Boolean computeValue(Class<?> type) {
                return findDeclaringClass(type, parameterTypes, candidateNames) == expectedOwner;
            }
        };
    }
}

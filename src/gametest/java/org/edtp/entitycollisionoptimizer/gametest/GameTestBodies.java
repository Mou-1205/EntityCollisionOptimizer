package org.edtp.entitycollisionoptimizer.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.edtp.entitycollisionoptimizer.collision.CollisionBodyAccess;

/** 1.21.1 test shims: Entity has no needsSync field and no TeleportTransition API. */
public final class GameTestBodies {
    private GameTestBodies() {}

    public static boolean get(Entity entity) {
        return ((CollisionBodyAccess) entity).eco$readNeedsSync();
    }

    public static void set(Entity entity, boolean value) {
        ((CollisionBodyAccess) entity).eco$writeNeedsSync(value);
    }

    public static Entity teleport(Entity entity, ServerLevel dest, double x, double y, double z) {
        return teleport(entity, dest, x, y, z, false);
    }

    /** rotateMomentum applies the nether-portal XZ scale used by the mode-2 fixture. */
    public static Entity teleport(Entity entity, ServerLevel dest, double x, double y, double z, boolean rotateMomentum) {
        Vec3 velocity = entity.getDeltaMovement();
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        if (entity.level() != dest) {
            if (rotateMomentum) {
                double scale = dest.dimension() == net.minecraft.world.level.Level.NETHER ? 8.0
                        : entity.level().dimension() == net.minecraft.world.level.Level.NETHER ? 0.125 : 1.0;
                velocity = new Vec3(velocity.x * scale, velocity.y, velocity.z * scale);
            }
            entity.unRide();
            var transition = new net.minecraft.world.level.portal.DimensionTransition(
                    dest, new Vec3(x, y, z), velocity, yRot, xRot,
                    net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING);
            Entity moved = entity.changeDimension(transition);
            if (moved == null) return null;
            entity = moved;
        }
        entity.teleportTo(x, y, z);
        // Portal processing may scale or zero components; fixtures expect the requested momentum.
        ((CollisionBodyAccess) entity).eco$publishVelocity(velocity);
        entity.setDeltaMovement(velocity);
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        return entity;
    }

    /**
     * 1.21.1 Piglin/Hoglin expose no setTimeInOverworld.
     * Force the overworld conversion timer so the next AI tick finishes conversion.
     */
    public static void skipOverworldCountdown(Entity entity) {
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            for (String name : new String[]{"timeInOverworld", "conversionTime", "field_21345"}) {
                try {
                    var field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    if (field.getType() == int.class) {
                        field.setInt(entity, 300);
                        return;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
    }
}

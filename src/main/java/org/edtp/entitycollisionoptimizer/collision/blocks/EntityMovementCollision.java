package org.edtp.entitycollisionoptimizer.collision.blocks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.edtp.entitycollisionoptimizer.natives.CollisionFrame;
import org.edtp.entitycollisionoptimizer.natives.NativeMovement;
import org.edtp.entitycollisionoptimizer.natives.NativeShapeBatch;

import java.util.List;

/** Java supplies context-dependent shapes; native owns clipping, stepping and integration. */
public final class EntityMovementCollision {
    private EntityMovementCollision() {}

    public static Vec3 collide(Entity entity, Vec3 requested) {
        try (NativeMovement movement = solve(entity, requested)) { return movement.displacement(); }
    }

    /** Caller owns the transaction through movement publication, including exceptional exits. */
    public static NativeMovement solve(Entity entity, Vec3 requested) {
        org.edtp.entitycollisionoptimizer.NativePathStats.MOVEMENT_SOLVES.incrementAndGet();
        var level = entity.level();
        NativeMovement movement = new NativeMovement(entity, requested, null, true);
        try {
            AABB scan = movement.stepScan();
            int[] hardIds = CollisionFrame.hardCollisionIds(entity, scan);
            movement.steppingState();
            try (NativeShapeBatch shapes = new NativeShapeBatch()) {
                if (requested.lengthSqr() != 0.0) {
                    long t0 = System.nanoTime();
                    OrderedBlockColliders.collectNative(level, CollisionContext.of(entity), entity,
                            scan, hardIds, shapes);
                    org.edtp.entitycollisionoptimizer.NativePathStats.COLLECT_NANOS
                            .addAndGet(System.nanoTime() - t0);
                }
                long t1 = System.nanoTime();
                movement.solve(shapes, false);
                org.edtp.entitycollisionoptimizer.NativePathStats.SOLVE_NANOS
                        .addAndGet(System.nanoTime() - t1);
            }
            if (movement.needsStep()) collectStep(entity, hardIds, movement);
            return movement;
        } catch (RuntimeException | Error failure) {
            movement.close();
            throw failure;
        }
    }

    private static void collectStep(Entity entity, int[] hardIds, NativeMovement movement) {
        try (NativeShapeBatch shapes = new NativeShapeBatch()) {
            OrderedBlockColliders.collectNative(entity.level(), CollisionContext.of(entity), entity,
                    movement.stepScan(), hardIds, shapes);
            movement.solve(shapes, true);
        }
    }

    public static Vec3 collideBox(Level level, CollisionContext context, Entity entity,
                                  Vec3 requested, AABB box, List<VoxelShape> entities) {
        org.edtp.entitycollisionoptimizer.NativePathStats.BOX_SOLVES.incrementAndGet();
        try (NativeMovement movement = new NativeMovement(entity, requested, box, false);
             NativeShapeBatch shapes = new NativeShapeBatch()) {
            OrderedBlockColliders.collectNative(level, context, entity, movement.stepScan(), entities, shapes);
            movement.solve(shapes, false);
            return movement.displacement();
        }
    }
}

package org.edtp.entitycollisionoptimizer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.edtp.entitycollisionoptimizer.collision.blocks.EntityMovementCollision;
import org.edtp.entitycollisionoptimizer.natives.NativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = Entity.class, priority = 1100)
public abstract class EntityMovementMixin {
    @WrapMethod(method = "move")
    private void eco$movementLifetime(MoverType type, Vec3 requested, Operation<Void> original,
                                       @Share("eco$movement") LocalRef<NativeMovement> transaction) {
        try {
            original.call(type, requested);
        } finally {
            NativeMovement movement = transaction.get();
            if (movement != null) movement.close();
        }
    }

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 eco$solveMovement(Entity entity, Vec3 requested, Operation<Vec3> original,
                                   @Share("eco$movement") LocalRef<NativeMovement> transaction) {
        if (!(entity.level() instanceof ServerLevel level)
                || !org.edtp.entitycollisionoptimizer.natives.CollisionFrame.isFrameActive(level)) {
            return original.call(entity, requested);
        }
        var result = EntityMovementCollision.solve(entity, requested);
        transaction.set(result);
        return result.displacement();
    }

    /**
     * 1.21.1 publishes the post-collide position via {@code setPos(DDD)} component adds,
     * not {@code Vec3.add}. Slice after {@code collide} so the early {@code setPos} is untouched.
     */
    @WrapOperation(
            method = "move",
            slice = @Slice(from = @At(value = "INVOKE", target =
                    "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")),
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"))
    private void eco$movementDestination(Entity instance, double x, double y, double z, Operation<Void> original,
                                         @Share("eco$movement") LocalRef<NativeMovement> transaction) {
        NativeMovement result = transaction.get();
        if (result == null) {
            original.call(instance, x, y, z);
            return;
        }
        try {
            Vec3 dest = result.destination(new Vec3(instance.getX(), instance.getY(), instance.getZ()));
            original.call(instance, dest.x, dest.y, dest.z);
        } catch (IllegalStateException ex) {
            // Third-party mixins may move the entity between solve and publish; keep vanilla coordinates.
            org.edtp.entitycollisionoptimizer.NativePathStats.DEST_FALLBACKS.incrementAndGet();
            org.edtp.entitycollisionoptimizer.EntityCollisionOptimizer.LOGGER
                    .warn("Movement destination check failed; using vanilla setPos coordinates", ex);
            original.call(instance, x, y, z);
        }
    }

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void eco$ownMovement(Vec3 requested, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level() instanceof ServerLevel level
                && org.edtp.entitycollisionoptimizer.natives.CollisionFrame.isFrameActive(level)) {
            cir.setReturnValue(EntityMovementCollision.collide(entity, requested));
        }
    }

    @Inject(method = "collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), cancellable = true)
    private static void eco$ownEntityBox(Entity entity, Vec3 requested, AABB box, Level level,
                                         List<VoxelShape> entities, CallbackInfoReturnable<Vec3> cir) {
        if (level instanceof ServerLevel serverLevel
                && org.edtp.entitycollisionoptimizer.natives.CollisionFrame.isFrameActive(serverLevel)) {
            CollisionContext context = entity == null ? CollisionContext.empty() : CollisionContext.of(entity);
            cir.setReturnValue(EntityMovementCollision.collideBox(level, context, entity, requested, box, entities));
        }
    }
}

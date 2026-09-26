package org.edtp.entitycollisionoptimizer.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.edtp.entitycollisionoptimizer.collision.CollisionBodyAccess;
import org.edtp.entitycollisionoptimizer.natives.CollisionStateTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Position and velocity use vanilla fields only while the entity is unbound. */
@Mixin(Entity.class)
public abstract class EntityBodyMixin implements CollisionBodyAccess {
    @Shadow private Vec3 position;
    @Shadow private net.minecraft.world.phys.AABB bb;
    @Shadow private Vec3 deltaMovement;
    // 1.21.1 Entity has no needsSync field; keep the flag in the native table / this unique slot.
    @Unique private boolean eco$needsSync;
    @Shadow public boolean noPhysics;
    @Unique private CollisionStateTable eco$bodyTable;
    @Unique private int eco$bodySlot;

    /** Java field is the canonical copy for vanilla getters; native row mirrors every write. */
    @Override public final Vec3 eco$readVelocity() {
        return deltaMovement;
    }

    @Override public final void eco$writeVelocity(Vec3 value) {
        // Match native setDeltaMovement: reject non-finite sums entirely (no store, no dirty bit).
        if (!Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z)) return;
        eco$publishVelocity(value);
        eco$writeNeedsSync(true);
    }

    @Override public final void eco$publishVelocity(Vec3 value) {
        deltaMovement = value;
        if (eco$bodyTable != null) eco$bodyTable.velocity(eco$bodySlot, value);
    }

    @Override public final boolean eco$readNeedsSync() {
        // The merged unique field is the public authority (kernel SYNC is mirrored on write).
        return eco$needsSync;
    }

    @Override public final void eco$writeNeedsSync(boolean value) {
        eco$needsSync = value;
        if (eco$bodyTable != null) eco$bodyTable.needsSync(eco$bodySlot, value);
    }

    @Override public final void eco$writeNoPhysics(boolean value) {
        noPhysics = value;
        eco$invalidatePushState();
    }

    @Override public final Vec3 eco$readPosition() {
        if (eco$bodyTable != null) {
            Vec3 shared = eco$bodyTable.position(eco$bodySlot);
            position = shared;
            return shared;
        }
        return position;
    }

    @Override public final void eco$writePosition(Vec3 value) {
        position = value;
        if (eco$bodyTable != null) eco$bodyTable.position(eco$bodySlot, value);
    }

    @Override public final java.lang.foreign.MemorySegment eco$positionBody() {
        return eco$bodyTable == null ? java.lang.foreign.MemorySegment.NULL : eco$bodyTable.row(eco$bodySlot);
    }

    @Override public final net.minecraft.world.phys.AABB eco$readBounds() {
        if (eco$bodyTable != null) {
            net.minecraft.world.phys.AABB shared = eco$bodyTable.bounds(eco$bodySlot);
            bb = shared;
            return shared;
        }
        return bb;
    }

    @Override public final void eco$writeBounds(net.minecraft.world.phys.AABB value) {
        bb = value;
        if (eco$bodyTable != null) eco$bodyTable.bounds(eco$bodySlot, value);
    }

    @Override public final java.lang.foreign.MemorySegment eco$movementBody() {
        return eco$bodyTable == null ? java.lang.foreign.MemorySegment.NULL : eco$bodyTable.movementRow(eco$bodySlot);
    }

    @Override public final void eco$invalidatePushState() {
        if (eco$bodyTable != null) eco$bodyTable.invalidatePushState(eco$bodySlot);
    }

    // Shared row is authoritative while bound: vanilla getters must see native writes.
    @Inject(method = "getX", at = @At("HEAD"), cancellable = true)
    private void eco$getX(CallbackInfoReturnable<Double> cir) {
        if (eco$bodyTable != null) cir.setReturnValue(eco$readPosition().x);
    }

    @Inject(method = "getY", at = @At("HEAD"), cancellable = true)
    private void eco$getY(CallbackInfoReturnable<Double> cir) {
        if (eco$bodyTable != null) cir.setReturnValue(eco$readPosition().y);
    }

    @Inject(method = "getZ", at = @At("HEAD"), cancellable = true)
    private void eco$getZ(CallbackInfoReturnable<Double> cir) {
        if (eco$bodyTable != null) cir.setReturnValue(eco$readPosition().z);
    }

    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    private void eco$getBoundingBox(CallbackInfoReturnable<AABB> cir) {
        if (eco$bodyTable != null) cir.setReturnValue(eco$readBounds());
    }

    @Override public final void eco$bindBody(CollisionStateTable table, int slot) {
        if (eco$bodyTable != table || eco$bodySlot != slot) {
            if (eco$bodyTable != null) {
                // Rebind must materialize the former owner's native row first.
                int oldSlot = eco$bodySlot;
                deltaMovement = eco$bodyTable.velocity(oldSlot);
                eco$needsSync = eco$bodyTable.needsSync(oldSlot);
                position = eco$bodyTable.position(oldSlot);
                bb = eco$bodyTable.bounds(oldSlot);
                eco$bodyTable.unbind(oldSlot);
            }
            table.velocity(slot, deltaMovement);
            table.needsSync(slot, eco$needsSync);
            table.position(slot, position);
            table.bounds(slot, bb);
            eco$bodyTable = table;
            eco$bodySlot = slot;
            table.bind(slot);
        }
    }

    @Override public final void eco$detachBody(CollisionStateTable table, int slot) {
        if (eco$bodyTable == table && eco$bodySlot == slot) {
            deltaMovement = table.velocity(slot);
            eco$needsSync = table.needsSync(slot);
            position = table.position(slot);
            bb = table.bounds(slot);
            table.unbind(slot);
            eco$bodyTable = null;
        }
    }
}

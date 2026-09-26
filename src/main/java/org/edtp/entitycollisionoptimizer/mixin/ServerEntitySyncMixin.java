package org.edtp.entitycollisionoptimizer.mixin;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.edtp.entitycollisionoptimizer.collision.CollisionBodyAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 ServerEntity has no needsSync field. The tracker still consumes the
 * merged dirty bit after it observes movement, matching 26.2 semantics.
 */
@Mixin(ServerEntity.class)
public abstract class ServerEntitySyncMixin {
    @Shadow private Entity entity;

    @Inject(method = "sendChanges", at = @At("RETURN"))
    private void entityCollisionOptimizer$consumeSync(CallbackInfo ci) {
        if (entity != null) {
            org.edtp.entitycollisionoptimizer.collision.CollisionBodyAccess access =
                    (org.edtp.entitycollisionoptimizer.collision.CollisionBodyAccess) entity;
            // Clear only; velocity is already canonical. Do not pull the table here.
            if (access.eco$readNeedsSync()) {
                access.eco$writeNeedsSync(false);
            }
        }
    }
}

package org.edtp.entitycollisionoptimizer.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.edtp.entitycollisionoptimizer.natives.CollisionFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Track query visibility via the entity's level (1.21.1 has no reliable this$0 shadow). */
@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public abstract class EntityTrackingMixin {
    @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void eco$start(Entity entity, CallbackInfo ci) {
        if (entity.level() instanceof ServerLevel level) CollisionFrame.trackingStarted(level, entity);
    }

    @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void eco$end(Entity entity, CallbackInfo ci) {
        if (entity.level() instanceof ServerLevel level) CollisionFrame.trackingEnded(level, entity);
    }

    @Inject(method = "onSectionChange(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void eco$section(Entity entity, CallbackInfo ci) {
        if (entity.level() instanceof ServerLevel level) CollisionFrame.sectionChanged(level, entity);
    }
}

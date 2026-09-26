package org.edtp.entitycollisionoptimizer.mixin;

import org.edtp.entitycollisionoptimizer.natives.CollisionFrame;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityCollisionOptimizer$attachEntityStorage(CallbackInfo ci) {
        CollisionFrame.attach((ServerLevel) (Object) this);
    }
}

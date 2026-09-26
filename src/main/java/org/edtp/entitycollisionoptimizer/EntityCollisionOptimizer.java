package org.edtp.entitycollisionoptimizer;

import com.mojang.logging.LogUtils;
import org.edtp.entitycollisionoptimizer.commands.CollisionOptimizerCommand;
import org.edtp.entitycollisionoptimizer.natives.CollisionFrame;
import org.edtp.entitycollisionoptimizer.natives.FFMBackend;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

public class EntityCollisionOptimizer implements ModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                CollisionOptimizerCommand.register(dispatcher));
        // Frame lifecycle via Fabric events, not ServerLevel.tick mixins.
        // Ixeris/Lithium rewrite tick internals; event hooks survive that.
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                CollisionFrame.begin(level);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                CollisionFrame.end(level);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            CollisionFrame.destroy();
            FFMBackend.destroy();
        });
    }
}

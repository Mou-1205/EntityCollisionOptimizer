package org.edtp.entitycollisionoptimizer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.edtp.entitycollisionoptimizer.natives.FFMBackend;

/** Explicit public controls; internal config fields are never reflected into commands. */
public final class CollisionOptimizerCommand {
    private CollisionOptimizerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eco")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(CollisionOptimizerCommand::status)
                .then(Commands.literal("check").executes(CollisionOptimizerCommand::status)));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        FFMBackend.initialize();
        String stats = org.edtp.entitycollisionoptimizer.NativePathStats.report();
        String line = "Entity Collision Optimizer: FFM initialized=" + FFMBackend.isInitialized()
                + " | " + stats;
        org.edtp.entitycollisionoptimizer.EntityCollisionOptimizer.LOGGER.info("[ECO] {}", line);
        context.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }
}

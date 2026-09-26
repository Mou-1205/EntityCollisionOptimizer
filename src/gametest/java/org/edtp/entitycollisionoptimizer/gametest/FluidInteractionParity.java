package org.edtp.entitycollisionoptimizer.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.edtp.entitycollisionoptimizer.EntityCollisionOptimizer;
import org.edtp.entitycollisionoptimizer.natives.CollisionFrame;

import java.util.ArrayList;
import java.util.List;

/** Fluid current/immersion and block effects run through Minecraft's physics, not synthetic impulses. */
final class FluidInteractionParity {
    static void verify(GameTestHelper helper) {
        int cases = 0;
        for (var type : List.of(EntityType.ITEM, EntityType.ZOMBIE, EntityType.PLAYER,
                EntityType.BOAT, EntityType.MAGMA_CUBE, EntityType.STRIDER)) {
            for (int fixture = 0; fixture < 5; fixture++) {
                var expected = run(helper, false, type, fixture);
                var actual = run(helper, true, type, fixture);
                for (int i = 0; i < expected.size(); i++) actual.get(i).compare(helper, expected.get(i),
                        "fluid fixture=" + fixture + " type=" + type + " step=" + i);
                if (fixture == 1 && type == EntityType.ITEM) {
                    helper.assertTrue(expected.getLast().position().x > expected.getFirst().position().x
                                    || expected.stream().anyMatch(s -> s.velocity().x > 1e-4),
                            "water gradient must actually transport " + type);
                }
                if (fixture != 2) helper.assertTrue(expected.stream().anyMatch(s -> (s.flags() & 32) != 0),
                        "water fixture must immerse " + type);
                if (type == EntityType.ITEM && (fixture == 3 || fixture == 4)) {
                    boolean upward = fixture == 3;
                    helper.assertTrue(expected.stream().anyMatch(s -> upward ? s.velocity().y > 0 : s.velocity().y < 0),
                            "bubble column must actually " + (upward ? "push up " : "pull down ") + type);
                }
                cases++;
            }
        }
        EntityCollisionOptimizer.LOGGER.info("ECO_FLUID_INTERACTIONS cases={} steps_per_case=12 result=passed", cases);
    }

    private static List<InteractionScene.State> run(GameTestHelper helper, boolean enabled, EntityType<?> type, int fixture) {
        try (var scene = new InteractionScene(helper)) {
            scene.floor(Blocks.STONE);
            for (int x = 1; x <= 10; x++) for (int y = 1; y <= 2; y++) {
                scene.block(x, y, 2, Blocks.STONE);
                scene.block(x, y, 6, Blocks.STONE);
                for (int z = 3; z <= 5; z++) {
                    var state = switch (fixture) {
                        case 1 -> Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, Math.min(7, x - 1));
                        case 2 -> Blocks.LAVA.defaultBlockState();
                        case 3, 4 -> Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(net.minecraft.world.level.block.BubbleColumnBlock.DRAG_DOWN, fixture == 4);
                        default -> Blocks.WATER.defaultBlockState();
                    };
                    scene.block(x, y, z, state);
                }
            }
            var entity = scene.spawn(type, new Vec3(3.5, 1.05, 4.5));
            if (entity instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
            entity.setDeltaMovement(Vec3.ZERO);
            List<InteractionScene.State> states = new ArrayList<>();
            for (int step = 0; step < 24; step++) {
                CollisionFrame.begin(helper.getLevel());
                InteractionScene.prepareTick(entity);
                // Full vanilla tick keeps fluid current integration.
                entity.tick();
                try {
                    var current = net.minecraft.world.entity.Entity.class
                            .getDeclaredMethod("updateInWaterStateAndDoWaterCurrentPushing");
                    current.setAccessible(true);
                    current.invoke(entity);
                    var fluid = net.minecraft.world.entity.Entity.class
                            .getDeclaredMethod("updateInWaterStateAndDoFluidPushing");
                    fluid.setAccessible(true);
                    fluid.invoke(entity);
                } catch (ReflectiveOperationException failure) {
                    throw new AssertionError(failure);
                }
                states.add(InteractionScene.State.of(entity));
                CollisionFrame.end(helper.getLevel());
            }
            return states;
        }
    }
}

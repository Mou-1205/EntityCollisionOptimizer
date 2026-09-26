package org.edtp.entitycollisionoptimizer.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ClimbingCollisionBenchmark {
    // The shared runner is serial, so this timeout also covers earlier scenarios.
    @GameTest(timeoutTicks = 1800)
    public void denseScaffolding(GameTestHelper helper) {
        CollisionBenchmarkRunner.run(helper, new ClimbingCollisionChamber(helper));
    }
}

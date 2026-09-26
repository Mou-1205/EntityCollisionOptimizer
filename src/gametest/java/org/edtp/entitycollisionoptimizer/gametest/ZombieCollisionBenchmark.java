package org.edtp.entitycollisionoptimizer.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ZombieCollisionBenchmark {
    static final int DURATION_TICKS = CollisionBenchmarkRunner.DURATION_TICKS;
    @GameTest(timeoutTicks = 500)
    public void fallingZombies(GameTestHelper helper) {
        CollisionBenchmarkRunner.run(helper, new ZombieBenchmarkChamber(helper));
    }
}

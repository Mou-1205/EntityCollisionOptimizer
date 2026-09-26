package org.edtp.entitycollisionoptimizer.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class ElderGuardianBenchmark {
    // The benchmark intentionally runs a 100-tick post-window drain.  Leave
    // headroom for heavily loaded profiling hosts that fall behind wall time.
    @GameTest(timeoutTicks = 1600)
    public void voidPipe(GameTestHelper helper) {
        CollisionBenchmarkRunner.run(helper, new ElderGuardianPipe(helper));
    }
}

package org.edtp.entitycollisionoptimizer;

import java.util.concurrent.atomic.AtomicLong;

/** Live counters so `/eco` can prove the native path is actually running. */
public final class NativePathStats {
    public static final AtomicLong MOVEMENT_SOLVES = new AtomicLong();
    public static final AtomicLong BOX_SOLVES = new AtomicLong();
    public static final AtomicLong PUSH_QUERIES = new AtomicLong();
    public static final AtomicLong NATIVE_PUSH_RUNS = new AtomicLong();
    public static final AtomicLong DEST_FALLBACKS = new AtomicLong();
    public static final AtomicLong COLLECT_NANOS = new AtomicLong();
    public static final AtomicLong SOLVE_NANOS = new AtomicLong();
    public static final AtomicLong INSERT_COUNT = new AtomicLong();
    public static final AtomicLong INSERT_NANOS = new AtomicLong();
    public static final AtomicLong BEGIN_COUNT = new AtomicLong();
    public static final AtomicLong BEGIN_NANOS = new AtomicLong();
    public static final AtomicLong DIRTY_FLUSH_COUNT = new AtomicLong();
    public static final AtomicLong DIRTY_FLUSH_NANOS = new AtomicLong();
    public static final AtomicLong FORCE_BEGIN_COUNT = new AtomicLong();
    public static final AtomicLong BOOTSTRAP_COUNT = new AtomicLong();
    public static final AtomicLong BOOTSTRAP_NANOS = new AtomicLong();

    public static String report() {
        long solves = MOVEMENT_SOLVES.get() + BOX_SOLVES.get();
        long collectMs = COLLECT_NANOS.get() / 1_000_000L;
        long solveMs = SOLVE_NANOS.get() / 1_000_000L;
        return "moves=" + MOVEMENT_SOLVES.get()
                + " boxes=" + BOX_SOLVES.get()
                + " pushQueries=" + PUSH_QUERIES.get()
                + " pushRuns=" + NATIVE_PUSH_RUNS.get()
                + " destFallbacks=" + DEST_FALLBACKS.get()
                + " collectMs=" + collectMs
                + " solveMs=" + solveMs
                + " solves=" + solves
                + " inserts=" + INSERT_COUNT.get()
                + " insertMs=" + (INSERT_NANOS.get() / 1_000_000L)
                + " begins=" + BEGIN_COUNT.get()
                + " beginMs=" + (BEGIN_NANOS.get() / 1_000_000L)
                + " dirtyFlush=" + DIRTY_FLUSH_COUNT.get()
                + " dirtyFlushMs=" + (DIRTY_FLUSH_NANOS.get() / 1_000_000L)
                + " forceBegin=" + FORCE_BEGIN_COUNT.get()
                + " bootstrapMs=" + (BOOTSTRAP_NANOS.get() / 1_000_000L);
    }

    private NativePathStats() {}
}

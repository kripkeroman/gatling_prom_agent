package org.example.gatling.agent.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe statistics aggregator for recording and computing basic metrics
 * (min, max, mean, standard deviation) over a stream of observed values.
 * <p>
 * This class implements an online algorithm (Welford's method) for calculating
 * mean and variance incrementally, without storing all values in memory.
 * <p>
 * Features:
 * <ul>
 *   <li>Tracks minimum and maximum values using {@link AtomicLong}.</li>
 *   <li>Computes mean and standard deviation with numerical stability.</li>
 *   <li>Thread-safe: uses atomic updates for min/max and a synchronized block for variance/mean.</li>
 * </ul>
 *
 * <h3>Intended usage</h3>
 * This aggregator is used inside the Gatling → Prometheus bridge
 * to calculate aggregate response-time metrics (min, max, mean, stddev)
 * across all recorded responses.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * Aggregator agg = new Aggregator();
 * agg.record(120);
 * agg.record(80);
 * agg.record(100);
 *
 * System.out.println("Min: " + agg.getMin());    // 80
 * System.out.println("Max: " + agg.getMax());    // 120
 * System.out.println("Mean: " + agg.getMean());  // 100
 * System.out.println("Std: " + agg.getStddev()); // ~20
 * }</pre>
 */
public class Aggregator {

    /** Lock for updating mean and variance (Welford algorithm). */
    private final Object lock = new Object();

    /** Number of recorded values. */
    private long n = 0;

    /** Current running mean of values. */
    private double mean = 0.0;

    /** Sum of squares of differences from the current mean (for variance). */
    private double m2 = 0.0;

    /** Minimum observed value. */
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);

    /** Maximum observed value. */
    private final AtomicLong max = new AtomicLong(0);

    /**
     * Records a new value into the aggregator, updating min, max, mean, and variance.
     *
     * @param value the observed value (e.g. response time in ms)
     */
    public void record(long value) {
        min.getAndUpdate(p -> Math.min(p, value));
        max.getAndUpdate(p -> Math.max(p, value));
        synchronized (lock) {
            n++;
            double delta = value - mean;
            mean += delta / n;
            m2 += delta * (value - mean);
        }
    }

    /**
     * @return the minimum observed value, or {@link Long#MAX_VALUE} if none recorded
     */
    public long getMin() {
        return min.get();
    }

    /**
     * @return the maximum observed value, or 0 if none recorded
     */
    public long getMax() {
        return max.get();
    }

    /**
     * @return the running mean of observed values, or 0.0 if none recorded
     */
    public double getMean() {
        return mean;
    }

    /**
     * @return the sample standard deviation of observed values,
     *         or 0.0 if fewer than two values have been recorded
     */
    public double getStddev() {
        return Math.sqrt(n > 1 ? m2 / (n - 1) : 0.0);
    }
}

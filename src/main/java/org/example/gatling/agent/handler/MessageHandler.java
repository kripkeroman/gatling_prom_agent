package org.example.gatling.agent.handler;

import lombok.RequiredArgsConstructor;
import org.example.gatling.agent.ReflectionHelper;
import org.example.gatling.agent.metrics.Aggregator;
import org.example.gatling.agent.metrics.Metrics;

/**
 * Handles Gatling load test events and updates Prometheus metrics accordingly.
 * <p>
 * This class receives raw event objects produced by Gatling's DataWriterMessage
 * (such as {@code UserStart}, {@code UserEnd}, {@code Response}) and translates them
 * into Prometheus metrics. It also updates aggregated statistics such as min, max,
 * mean, and standard deviation of response times.
 * <p>
 * The class relies on:
 * <ul>
 *   <li>{@link Metrics} – a factory/wrapper around Prometheus metric collectors
 *       (counters, gauges, histograms).</li>
 *   <li>{@link Aggregator} – for incremental computation of response-time aggregates
 *       using Welford’s algorithm.</li>
 *   <li>{@link ReflectionHelper} – utility to safely call methods on Gatling’s
 *       internal Scala case classes via reflection, avoiding compile-time dependency
 *       on Gatling internals.</li>
 * </ul>
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Increment user start/finish counters and active user gauges.</li>
 *   <li>Track request counts, errors, and response-time distributions.</li>
 *   <li>Update statistical gauges for min, max, mean, and stddev response times.</li>
 * </ul>
 *
 * <p>Example usage (simplified):</p>
 * <pre>{@code
 * MessageHandler handler = new MessageHandler(metrics, aggregator);
 * handler.handle(gatlingEvent);
 * }</pre>
 */
@RequiredArgsConstructor
public class MessageHandler {

    /** Provides access to Prometheus metrics collectors. */
    private final Metrics metrics;

    /** Aggregator for computing mean, variance, min, and max response times. */
    private final Aggregator aggregator;

    /** Reflection utility for accessing Gatling’s case-class fields. */
    private final ReflectionHelper refl = new ReflectionHelper();

    /**
     * Processes a Gatling event object and updates Prometheus metrics.
     * <p>
     * Supported events:
     * <ul>
     *   <li>{@code UserStart} – increments user start counter and active user gauge.</li>
     *   <li>{@code UserEnd} – increments user finish counter and decrements active user gauge.</li>
     *   <li>{@code Response} – records request outcome, response time histogram,
     *       errors (if status is KO), and updates aggregated statistics.</li>
     * </ul>
     *
     * @param msg the Gatling event object (UserStart, UserEnd, Response, etc.)
     */
    public void handle(Object msg) {
        String cls = msg.getClass().getName();

        // UserStart event
        if (cls.endsWith("UserStart")) {
            String scenario = (String) refl.invoke(msg, "scenario");
            metrics.usersStarted().labels(scenario).inc();
            metrics.activeUsers().labels(scenario).inc();

            // UserEnd event
        } else if (cls.endsWith("UserEnd")) {
            String scenario = (String) refl.invoke(msg, "scenario");
            metrics.usersFinished().labels(scenario).inc();
            metrics.activeUsers().labels(scenario).dec();

            // Response event
        } else if (cls.endsWith("Response")) {
            String name = (String) refl.invoke(msg, "name");
            String group = ""; // TODO: implement scalaSeqToPath translation if needed
            String status = String.valueOf(refl.invoke(msg, "status"));
            long start = (Long) refl.invoke(msg, "startTimestamp");
            long end = (Long) refl.invoke(msg, "endTimestamp");
            long rt = end - start;

            // Counters and histogram
            metrics.requestsTotal().labels(name, group, status).inc();
            metrics.responseSeconds().labels(name, group, status).observe(rt / 1000.0);
            if ("KO".equalsIgnoreCase(status)) {
                metrics.errorsTotal().labels(name, group).inc();
            }

            // Aggregated stats
            aggregator.record(rt);
            metrics.minResponseMs().set(aggregator.getMin());
            metrics.maxResponseMs().set(aggregator.getMax());
            metrics.meanResponseMs().set(aggregator.getMean());
            metrics.stdDevResponseMs().set(aggregator.getStddev());
        }
    }
}

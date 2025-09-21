package org.example.gatling.agent.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

/**
 * Immutable record that groups together all Prometheus metrics used
 * for exposing Gatling simulation statistics.
 * <p>
 * An instance of this record is created by {@link MetricRegistryFactory#create(java.util.List)}
 * and holds a shared {@link CollectorRegistry} plus all counters, gauges,
 * and histograms registered within it.
 * <p>
 * This design centralizes metric definitions in one place, making them
 * easy to pass around between components like the {@code MessageHandler},
 * {@code Aggregator}, and {@code PushGatewayService}.
 *
 * <h3>Metrics contained:</h3>
 * <ul>
 *   <li>{@link #requestsTotal()} – Counter of total requests by name, group, and status.</li>
 *   <li>{@link #responseSeconds()} – Histogram of response times in seconds by name, group, and status.</li>
 *   <li>{@link #usersStarted()} – Counter of users started per scenario.</li>
 *   <li>{@link #usersFinished()} – Counter of users finished per scenario.</li>
 *   <li>{@link #activeUsers()} – Gauge of currently active users per scenario.</li>
 *   <li>{@link #errorsTotal()} – Counter of failed (KO) responses by name and group.</li>
 *   <li>{@link #minResponseMs()} – Gauge for minimum observed response time in milliseconds.</li>
 *   <li>{@link #maxResponseMs()} – Gauge for maximum observed response time in milliseconds.</li>
 *   <li>{@link #meanResponseMs()} – Gauge for mean response time in milliseconds.</li>
 *   <li>{@link #stdDevResponseMs()} – Gauge for standard deviation of response times in milliseconds.</li>
 *   <li>{@link #heartbeat()} – Gauge updated periodically to indicate writer liveness (heartbeat).</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * Metrics metrics = MetricRegistryFactory.create(Arrays.asList(50,100,200,500,1000));
 *
 * // increment counters
 * metrics.requestsTotal().labels("GET /api", "root", "OK").inc();
 * metrics.usersStarted().labels("Scenario1").inc();
 *
 * // record latency
 * metrics.responseSeconds().labels("GET /api", "root", "OK").observe(0.245);
 *
 * // update aggregates
 * metrics.minResponseMs().set(15);
 * metrics.maxResponseMs().set(980);
 * }</pre>
 */
public record Metrics(
        CollectorRegistry registry,
        Counter requestsTotal,
        Histogram responseSeconds,
        Counter usersStarted,
        Counter usersFinished,
        Gauge activeUsers,
        Counter errorsTotal,
        Gauge minResponseMs,
        Gauge maxResponseMs,
        Gauge meanResponseMs,
        Gauge stdDevResponseMs,
        Gauge heartbeat
) {
}

package org.example.gatling.agent.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

import java.util.List;

/**
 * Factory for creating a {@link Metrics} instance with a fully initialized Prometheus {@link CollectorRegistry}.
 * <p>
 * This class encapsulates the creation of all Prometheus counters, gauges, and histograms
 * used by the Gatling → Prometheus Pushgateway integration.
 * It ensures that metrics are consistently named, labeled, and registered in a single {@link CollectorRegistry}.
 * <p>
 * Metrics created:
 * <ul>
 *   <li><b>gatling_requests_total</b> — total requests by name, group, and status.</li>
 *   <li><b>gatling_response_time_seconds</b> — histogram of response times in seconds by name, group, and status.</li>
 *   <li><b>gatling_users_started_total</b> — counter of started users per scenario.</li>
 *   <li><b>gatling_users_finished_total</b> — counter of finished users per scenario.</li>
 *   <li><b>gatling_active_users</b> — gauge of currently active users per scenario.</li>
 *   <li><b>gatling_errors_total</b> — counter of failed (KO) responses by name and group.</li>
 *   <li><b>gatling_response_time_min_ms</b> — gauge for minimum observed response time (ms).</li>
 *   <li><b>gatling_response_time_max_ms</b> — gauge for maximum observed response time (ms).</li>
 *   <li><b>gatling_response_time_mean_ms</b> — gauge for mean observed response time (ms).</li>
 *   <li><b>gatling_response_time_stddev_ms</b> — gauge for standard deviation of response times (ms).</li>
 *   <li><b>gatling_writer_heartbeat</b> — gauge updated periodically to indicate liveness of the writer.</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * List<Integer> bucketsMs = Arrays.asList(50, 100, 200, 500, 1000);
 * Metrics metrics = MetricRegistryFactory.create(bucketsMs);
 *
 * // Record a request
 * metrics.requestsTotal().labels("GET /api", "root", "OK").inc();
 * metrics.responseSeconds().labels("GET /api", "root", "OK").observe(0.120);
 * }</pre>
 */
public class MetricRegistryFactory {

    /**
     * Creates a new {@link Metrics} object with all required Prometheus metrics registered.
     *
     * @param bucketsMs the histogram bucket boundaries in milliseconds (converted to seconds for Prometheus)
     * @return fully initialized {@link Metrics} instance containing all counters, gauges, and histograms
     */
    public static Metrics create(List<Integer> bucketsMs) {
        CollectorRegistry registry = new CollectorRegistry();
        double[] bucketsSec = bucketsMs.stream().mapToDouble(v -> v / 1000.0).toArray();

        return new Metrics(
                registry,
                Counter.build().name("gatling_requests_total")
                        .help("Total requests").labelNames("name","group","status").register(registry),
                Histogram.build().name("gatling_response_time_seconds")
                        .help("Request duration").labelNames("name","group","status")
                        .buckets(bucketsSec).register(registry),
                Counter.build().name("gatling_users_started_total")
                        .help("Users started").labelNames("scenario").register(registry),
                Counter.build().name("gatling_users_finished_total")
                        .help("Users finished").labelNames("scenario").register(registry),
                Gauge.build().name("gatling_active_users")
                        .help("Active users").labelNames("scenario").register(registry),
                Counter.build().name("gatling_errors_total")
                        .help("KO responses").labelNames("name","group").register(registry),
                Gauge.build().name("gatling_response_time_min_ms")
                        .help("Min response time").register(registry),
                Gauge.build().name("gatling_response_time_max_ms")
                        .help("Max response time").register(registry),
                Gauge.build().name("gatling_response_time_mean_ms")
                        .help("Mean response time").register(registry),
                Gauge.build().name("gatling_response_time_stddev_ms")
                        .help("Stddev response time").register(registry),
                Gauge.build().name("gatling_writer_heartbeat")
                        .help("Writer heartbeat").register(registry)
        );
    }
}

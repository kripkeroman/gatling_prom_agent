package org.example.gatling.agent.push;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import lombok.extern.slf4j.Slf4j;
import org.example.gatling.agent.config.PrometheusConfig;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Service wrapper around Prometheus {@link PushGateway}, responsible for pushing
 * or deleting collected metrics to/from a configured Pushgateway instance.
 * <p>
 * This class encapsulates job and grouping key management, as well as error handling,
 * providing safe and convenient push operations for the agent.
 *
 * <h3>Initialization:</h3>
 * Constructed using {@link PrometheusConfig}, which defines the target Pushgateway
 * URL, job name, and instance label. A {@code run_start_epoch_ms} label is also
 * automatically added to the grouping key at construction time.
 *
 * <h3>Grouping Key:</h3>
 * <ul>
 *   <li><b>instance</b> – provided by config (usually hostname or logical instance id)</li>
 *   <li><b>run_start_epoch_ms</b> – timestamp in milliseconds when this service was created</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * PrometheusConfig config = PrometheusConfig.load();
 * PushGatewayService pushService = new PushGatewayService(config);
 *
 * // Push current metrics
 * pushService.push(metrics.registry());
 *
 * // On stop, optionally delete metrics
 * if (config.deleteOnStop()) {
 *     pushService.delete(metrics.registry());
 * }
 * }</pre>
 *
 * <h3>Thread-Safety:</h3>
 * This class itself is stateless except for its internal {@link PushGateway} instance
 * and grouping key map, which are safely initialized once at construction.
 *
 * @see PushGateway
 * @see CollectorRegistry
 */
@Slf4j
public class PushGatewayService {

    private final PushGateway gateway;
    private final String job;
    private final Map<String, String> groupingKey;

    /**
     * Creates a new {@code PushGatewayService} based on the provided configuration.
     * Initializes the {@link PushGateway} and sets up the grouping key.
     *
     * @param cfg Prometheus configuration loaded from system/env/config file.
     * @throws Exception if the PushGateway URL is malformed.
     */
    public PushGatewayService(PrometheusConfig cfg) throws Exception {
        this.job = cfg.job();
        this.groupingKey = new HashMap<>();
        this.groupingKey.put("instance", cfg.instance());
        this.groupingKey.put("run_start_epoch_ms", String.valueOf(System.currentTimeMillis()));

        if (cfg.url().startsWith("http")) {
            this.gateway = new PushGateway(new URL(cfg.url()));
        } else {
            this.gateway = new PushGateway(cfg.url());
        }
    }

    /**
     * Pushes all metrics in the given registry to the Pushgateway,
     * merging them with any previously pushed metrics.
     *
     * @param registry the Prometheus collector registry containing metrics
     */
    public void push(CollectorRegistry registry) {
        try { gateway.pushAdd(registry, job, groupingKey); }
        catch (Exception e) { log.warn("Push failed: {}", e.getMessage()); }
    }

    /**
     * Deletes the metrics associated with the current job and grouping key
     * from the Pushgateway.
     *
     * @param registry the Prometheus collector registry containing metrics
     */
    public void delete(CollectorRegistry registry) {
        try { gateway.delete(job, groupingKey); }
        catch (Exception e) { log.warn("Delete failed: {}", e.getMessage()); }
    }

    /**
     * Safe version of {@link #push(CollectorRegistry)} that performs
     * null checks and catches exceptions to prevent agent crashes.
     *
     * @param registry the Prometheus collector registry containing metrics
     */
    public void pushSafe(CollectorRegistry registry) {
        if (gateway == null || registry == null) return;
        try {
            gateway.pushAdd(registry, job, groupingKey);
        } catch (Exception e) {
            log.warn("[GatlingPromAgent] push failed: {}", e.getMessage());
        }
    }
}

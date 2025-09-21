package org.example.gatling.agent.config;

/**
 * Defines constants for configuration keys used by the Prometheus Gatling Agent.
 *
 * <p>This class is a central place to store all system property and environment
 * variable keys that control the agent's behavior and its connection to a
 * Prometheus PushGateway. The constants are referenced throughout the codebase
 * (e.g., {@link PrometheusConfig} and {@link ConfigLoader}) to standardize
 * configuration handling.</p>
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Provide a single source of truth for all supported configuration keys.</li>
 *   <li>Reduce risk of typos or inconsistent property names in different classes.</li>
 *   <li>Improve maintainability when adding new configuration options.</li>
 * </ul>
 *
 * <h2>Supported Keys</h2>
 * <ul>
 *   <li>{@link #URL} – PushGateway URL (system property: {@code prom.pushgateway.url}).</li>
 *   <li>{@link #JOB} – Job name label for PushGateway metrics ({@code prom.job}).</li>
 *   <li>{@link #INSTANCE} – Instance label ({@code prom.instance}).</li>
 *   <li>{@link #USER} – Username for basic authentication with PushGateway.</li>
 *   <li>{@link #PASSWORD} – Password for basic authentication with PushGateway.</li>
 *   <li>{@link #DELETE_ON_STOP} – Whether metrics should be deleted on simulation stop.</li>
 *   <li>{@link #PUSH_PERIOD_SECONDS} – Period (in seconds) between pushes.</li>
 *   <li>{@link #HISTO_BUCKETS_MS} – Histogram bucket boundaries in milliseconds.</li>
 * </ul>
 *
 * <h2>Alternative Keys</h2>
 * <p>In addition to the canonical {@code prom.*} keys, some alternative keys are
 * supported for backward compatibility:</p>
 * <ul>
 *   <li>{@link #URL_ALT} – Alternative for {@link #URL}.</li>
 *   <li>{@link #JOB_ALT} – Alternative for {@link #JOB}.</li>
 * </ul>
 *
 * <p>This class cannot be instantiated.</p>
 */
final class ConfigKeys {

    private ConfigKeys() {}

    /** Common prefix for all agent-related properties. */
    static final String PROM_PREFIX = "prom";

    /** PushGateway URL key. */
    static final String URL = "prom.pushgateway.url";

    /** Job name key. */
    static final String JOB = "prom.job";

    /** Instance label key. */
    static final String INSTANCE = "prom.instance";

    /** PushGateway basic auth user key. */
    static final String USER = "prom.pushgateway.user";

    /** PushGateway basic auth password key. */
    static final String PASSWORD = "prom.pushgateway.password";

    /** Flag to delete metrics on stop. */
    static final String DELETE_ON_STOP = "prom.delete.on.stop";

    /** Push period (seconds) key. */
    static final String PUSH_PERIOD_SECONDS = "prom.push.period.seconds";

    /** Histogram bucket boundaries key (milliseconds). */
    static final String HISTO_BUCKETS_MS = "prom.histogram.buckets.ms";

    /** Alternative PushGateway URL key. */
    static final String URL_ALT = "pushgateway.url";

    /** Alternative job name key. */
    static final String JOB_ALT = "pushgateway.job";
}

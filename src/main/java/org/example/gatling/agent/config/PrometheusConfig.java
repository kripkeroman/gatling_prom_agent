package org.example.gatling.agent.config;

import java.util.List;

/**
 * Configuration record for the Prometheus Pushgateway integration.
 * <p>
 * This record encapsulates all relevant settings required to connect and push metrics
 * from Gatling to a Prometheus Pushgateway instance. The configuration values can be
 * loaded from a YAML/properties file, system properties, or environment variables.
 * Precedence is as follows:
 * <ul>
 *   <li>System properties (-D flags) override everything</li>
 *   <li>Environment variables override configuration file values</li>
 *   <li>Configuration file values (YAML or .properties) are used as defaults</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 *     PrometheusConfig config = PrometheusConfig.load();
 *     String url = config.url();
 *     int pushPeriod = config.pushPeriodSec();
 * </pre>
 *
 * @param url           The URL of the Prometheus Pushgateway (e.g. http://localhost:9091).
 * @param job           The job label used for grouping metrics in Prometheus.
 * @param instance      The instance label (e.g. hostname or unique identifier for this run).
 * @param pushPeriodSec The push interval in seconds for sending metrics to the Pushgateway.
 * @param deleteOnStop  Whether metrics should be deleted from the Pushgateway when Gatling stops.
 * @param bucketsMs     Histogram bucket definitions in milliseconds, used for response time metrics.
 * @param user          Optional username for basic authentication with the Pushgateway.
 * @param password      Optional password for basic authentication with the Pushgateway.
 */
public record PrometheusConfig(
        String url,
        String job,
        String instance,
        int pushPeriodSec,
        boolean deleteOnStop,
        List<Integer> bucketsMs,
        String user,
        String password
) {

    /**
     * Loads the Prometheus configuration by merging values from system properties,
     * environment variables, and a configuration file.
     * <p>
     * The default configuration file path is {@code src/test/resources/config.yml},
     * unless overridden by the system property {@code -Dprom.config.file}.
     *
     * @return a fully resolved {@link PrometheusConfig} instance.
     */
    public static PrometheusConfig load() {
        String configPath = System.getProperty("prom.config.file", "src/test/resources/config.yml");
        ConfigLoader cfg = new ConfigLoader(configPath);

        String url = sysOrEnv("prom.pushgateway.url", "PROM_PUSHGATEWAY_URL", cfg.getUrl());
        String job = sysOrEnv("prom.job", "PROM_JOB", cfg.getJobName());
        String instance = sysOrEnv("prom.instance", "PROM_INSTANCE", cfg.getInstance());
        int pushPeriod = parseInt(sysOrEnv("prom.push.period.seconds", "PROM_PUSH_PERIOD_SECONDS",
                String.valueOf(cfg.getPushPeriodSeconds())), 5);
        boolean deleteOnStop = Boolean.parseBoolean(
                sysOrEnv("prom.delete.on.stop", "PROM_DELETE_ON_STOP", String.valueOf(cfg.isDeleteOnStop()))
        );
        List<Integer> buckets = cfg.getHistogramBucketsMs();

        return new PrometheusConfig(url, job, instance, pushPeriod, deleteOnStop, buckets, cfg.getUser(), cfg.getPassword());
    }

    /**
     * Utility method that resolves a configuration value from system properties,
     * environment variables, or a default.
     *
     * @param prop the name of the system property (e.g. {@code prom.job})
     * @param env  the name of the environment variable (e.g. {@code PROM_JOB})
     * @param def  the default value if neither property nor environment variable is set
     * @return the resolved string value
     */
    private static String sysOrEnv(String prop, String env, String def) {
        String v = System.getProperty(prop);
        if (v != null && !v.isBlank()) return v;
        v = System.getenv(env);
        return (v != null && !v.isBlank()) ? v : def;
    }

    /**
     * Parses an integer from a string, returning a default if parsing fails.
     *
     * @param s   the string to parse
     * @param def the default value to return on failure
     * @return the parsed integer, or {@code def} if parsing fails
     */
    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}

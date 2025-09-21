package org.example.gatling.agent.config;

import java.util.*;

import static org.example.gatling.agent.config.ConfigKeys.*;

/**
 * Immutable configuration holder for the Gatling → Prometheus Pushgateway agent.
 *
 * <p>{@code PromConfig} encapsulates all configuration options required by the agent
 * to connect to Prometheus Pushgateway and to control push/delete behavior.
 * It is typically created from a {@link java.util.Properties} object loaded by
 * {@link ConfigLoader}, and provides default values when some properties are missing.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Store connection details such as Pushgateway URL, job name, and instance label.</li>
 *   <li>Store authentication information (optional user and password).</li>
 *   <li>Configure runtime behavior (push period, delete-on-stop flag).</li>
 *   <li>Configure histogram buckets for response-time metrics.</li>
 *   <li>Provide a factory method {@link #from(Properties)} that builds a config
 *       with sensible defaults and type-safe parsing.</li>
 * </ul>
 *
 * <h2>Supported properties</h2>
 * <ul>
 *   <li>{@code prom.pushgateway.url} / {@code pushgateway.url} – Pushgateway base URL (default: {@code http://localhost:9091})</li>
 *   <li>{@code prom.job} / {@code pushgateway.job} – Job name label (default: {@code gatling})</li>
 *   <li>{@code prom.instance} – Instance label (default: {@code default})</li>
 *   <li>{@code prom.pushgateway.user} – Username for Pushgateway authentication (optional)</li>
 *   <li>{@code prom.pushgateway.password} – Password for Pushgateway authentication (optional)</li>
 *   <li>{@code prom.delete.on.stop} – Whether to delete metrics on stop (default: {@code true})</li>
 *   <li>{@code prom.push.period.seconds} – Period between pushes in seconds (default: {@code 5})</li>
 *   <li>{@code prom.histogram.buckets.ms} – Histogram bucket boundaries in milliseconds,
 *       specified as comma/semicolon/space separated list or array notation (default: {@code 50,100,200,500,1000})</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Properties props = new Properties();
 * props.setProperty("prom.pushgateway.url", "http://localhost:9091");
 * props.setProperty("prom.job", "loadtest");
 *
 * PromConfig cfg = PromConfig.from(props);
 * System.out.println(cfg.getUrl());        // "http://localhost:9091"
 * System.out.println(cfg.getJob());        // "loadtest"
 * }</pre>
 *
 * <h2>Thread-safety</h2>
 * <p>{@code PromConfig} is immutable and therefore safe to share across threads.</p>
 *
 * @see ConfigLoader
 * @see ConfigSource
 */
public final class PromConfig {

    /** Base URL of the Pushgateway (e.g. {@code http://localhost:9091}). */
    private final String url;

    /** Job name under which metrics are grouped in Prometheus. */
    private final String job;

    /** Instance label for distinguishing different test runs or hosts. */
    private final String instance;

    /** Optional Pushgateway username for basic authentication. */
    private final String user;

    /** Optional Pushgateway password for basic authentication. */
    private final String password;

    /** Whether to delete metrics from Pushgateway upon simulation stop. */
    private final boolean deleteOnStop;

    /** Interval in seconds between metric pushes. */
    private final int pushPeriodSeconds;

    /** Histogram bucket boundaries in milliseconds for response times. */
    private final List<Integer> histogramBucketsMs;

    /**
     * Private constructor. Instances are created via {@link Builder}.
     */
    private PromConfig(Builder b) {
        this.url = b.url;
        this.job = b.job;
        this.instance = b.instance;
        this.user = b.user;
        this.password = b.password;
        this.deleteOnStop = b.deleteOnStop;
        this.pushPeriodSeconds = b.pushPeriodSeconds;
        this.histogramBucketsMs = Collections.unmodifiableList(new ArrayList<>(b.histogramBucketsMs));
    }

    /** @return the Pushgateway URL */
    public String getUrl() { return url; }

    /** @return the job label */
    public String getJob() { return job; }

    /** @return the instance label */
    public String getInstance() { return instance; }

    /** @return the configured username (may be empty) */
    public String getUser() { return user; }

    /** @return the configured password (may be empty) */
    public String getPassword() { return password; }

    /** @return true if metrics should be deleted from Pushgateway on stop */
    public boolean isDeleteOnStop() { return deleteOnStop; }

    /** @return push period in seconds */
    public int getPushPeriodSeconds() { return pushPeriodSeconds; }

    /** @return histogram bucket boundaries in milliseconds */
    public List<Integer> getHistogramBucketsMs() { return histogramBucketsMs; }

    /**
     * Creates a {@code PromConfig} from {@link Properties}, applying defaults where necessary.
     *
     * @param p input properties (usually loaded from config file or environment)
     * @return immutable configuration instance
     */
    public static PromConfig from(Properties p) {
        Builder b = new Builder();

        b.url = firstNonBlank(p, URL, URL_ALT, "http://localhost:9091");
        b.job = firstNonBlank(p, JOB, JOB_ALT, "gatling");
        b.instance = p.getProperty(INSTANCE, "default");

        b.user = p.getProperty(USER, "").trim();
        b.password = p.getProperty(PASSWORD, "").trim();

        b.deleteOnStop = parseBool(p.getProperty(DELETE_ON_STOP, "true"));
        b.pushPeriodSeconds = parseIntSafe(p.getProperty(PUSH_PERIOD_SECONDS, "5"), 5);

        String rawBuckets = Optional.ofNullable(p.getProperty(HISTO_BUCKETS_MS))
                .map(String::trim).orElse("50,100,200,500,1000");
        b.histogramBucketsMs = parseBuckets(rawBuckets);

        return b.build();
    }

    /**
     * Returns the first non-blank value among two property keys, or default if none are set.
     */
    private static String firstNonBlank(Properties p, String k1, String k2, String def) {
        String v1 = trimToNull(p.getProperty(k1));
        if (v1 != null) return v1;
        String v2 = trimToNull(p.getProperty(k2)); return v2 != null ? v2 : def;
    }

    /** Converts empty string to null. */
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim(); return t.isEmpty() ? null : t;
    }

    /**
     * Parses boolean value from flexible strings: true/false, 1/0, yes/no, on/off.
     */
    private static boolean parseBool(String s) {
        String v = s.trim().toLowerCase(Locale.ROOT);
        switch (v) {
            case "true":
            case "1":
            case "yes":
            case "y":
            case "on":
                return true;
            case "false":
            case "0":
            case "no":
            case "n":
            case "off":
                return false;
            default:
                return Boolean.parseBoolean(v);
        }
    }

    /**
     * Parses integer safely, returning default if parse fails.
     */
    private static int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Parses histogram bucket boundaries from string like "50,100,200".
     */
    private static List<Integer> parseBuckets(String raw) {
        String r = raw.trim();
        if (r.startsWith("[") && r.endsWith("]")) {
            r = r.substring(1, r.length() - 1);
        } if (r.isEmpty()) return Arrays.asList(50, 100, 200, 500, 1000);
        String[] parts = r.split("[,;\\s]+"); List<Integer> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (!p.isBlank()) out.add(Integer.parseInt(p.trim()));
        }
        return out;
    }

    @Override
    public String toString() {
        return "PromConfig{" +
                "url='" + url + '\'' +
                ", job='" + job + '\'' +
                ", instance='" + instance + '\'' +
                ", deleteOnStop=" + deleteOnStop +
                ", pushPeriodSeconds=" + pushPeriodSeconds +
                ", histogramBucketsMs=" + histogramBucketsMs +
                '}';
    }

    /**
     * Builder for {@link PromConfig}. Typically used internally, but may be used for programmatic setup.
     */
    public static final class Builder {
        private String url;
        private String job;
        private String instance;
        private String user;
        private String password;
        private boolean deleteOnStop;
        private int pushPeriodSeconds;
        private List<Integer> histogramBucketsMs = new ArrayList<>();

        /**
         * Builds a validated immutable {@link PromConfig}.
         *
         * <p>Requires {@code url}, {@code job}, and {@code instance} to be non-null.
         * Defaults will be applied for empty histogram buckets and invalid push period.</p>
         *
         * @return a new PromConfig instance
         */
        public PromConfig build() {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(job, "job");
            Objects.requireNonNull(instance, "instance");
            if (histogramBucketsMs.isEmpty()) histogramBucketsMs = Arrays.asList(50,100,200,500,1000);
            if (pushPeriodSeconds <= 0) pushPeriodSeconds = 5;
            return new PromConfig(this);
        }
    }
}

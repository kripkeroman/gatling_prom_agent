package org.example.gatling.agent.config;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Loads Prometheus agent configuration from a file.
 *
 * <p>This class provides a unified way to read configuration parameters
 * (URL, job, instance, histogram buckets, etc.) for the
 * {@link org.example.gatling.agent.PrometheusMirror}.
 * The {@code ConfigLoader} delegates parsing of the actual file
 * (YAML or properties) to a {@link ConfigSource}, then maps the
 * resulting {@link java.util.Properties} into a {@link PromConfig} object.</p>
 *
 * <h2>Supported formats</h2>
 * <ul>
 *   <li><b>.yml / .yaml</b> – configuration in YAML format.</li>
 *   <li><b>.properties</b> – configuration as Java properties file.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ConfigLoader loader = new ConfigLoader("config.yml");
 * String pushGatewayUrl = loader.getUrl();
 * int pushPeriod = loader.getPushPeriodSeconds();
 * }</pre>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Determine the file type based on extension and delegate to the correct {@link ConfigSource}.</li>
 *   <li>Load configuration into {@link PromConfig}.</li>
 *   <li>Provide convenience accessors for frequently used fields.</li>
 *   <li>Expose derived values, such as histogram buckets in seconds.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>This class is immutable after construction and therefore thread-safe.</p>
 *
 * @see ConfigSource
 * @see PromConfig
 */
public class ConfigLoader {

    private final PromConfig config;

    /**
     * Creates a new configuration loader.
     *
     * @param filePath the path to the configuration file
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public ConfigLoader(String filePath) {
        try {
            ConfigSource source = ConfigSource.fromFile(filePath);
            Properties props = source.load();
            this.config = PromConfig.from(props);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + filePath, e);
        }
    }

    /** @return PushGateway URL. */
    public String getUrl()               { return config.getUrl(); }

    /** @return Job name for metrics. */
    public String getJobName()           { return config.getJob(); }

    /** @return Instance label. */
    public String getInstance()          { return config.getInstance(); }

    /** @return Username for basic authentication (nullable). */
    public String getUser()              { return config.getUser(); }

    /** @return Password for basic authentication (nullable). */
    public String getPassword()          { return config.getPassword(); }

    /** @return Whether to delete metrics on stop. */
    public boolean isDeleteOnStop()      { return config.isDeleteOnStop(); }

    /** @return Push period in seconds. */
    public int getPushPeriodSeconds()    { return config.getPushPeriodSeconds(); }

    /** @return Histogram buckets in milliseconds. */
    public List<Integer> getHistogramBucketsMs() { return config.getHistogramBucketsMs(); }

    /**
     * Returns histogram bucket boundaries in seconds (converted from milliseconds).
     *
     * @return array of bucket values in seconds
     */
    public double[] histogramBucketsSeconds() {
        return config.getHistogramBucketsMs().stream()
                .mapToDouble(ms -> ms / 1000.0)
                .toArray();
    }

    /**
     * Returns a debug-friendly string representation of the configuration.
     *
     * @return config as string
     */
    public String debugString() {
        return config.toString();
    }
}

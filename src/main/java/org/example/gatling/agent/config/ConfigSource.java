package org.example.gatling.agent.config;

import java.io.IOException;
import java.util.Properties;

/**
 * Abstraction for loading configuration properties from a specific source.
 *
 * <p>The {@code ConfigSource} interface defines the contract for classes that
 * provide Prometheus/Gatling agent configuration. Each implementation is responsible
 * for reading a concrete file format (e.g. YAML or Properties) and exposing the
 * configuration values as a {@link Properties} object.</p>
 *
 * <h2>Implementations</h2>
 * <ul>
 *   <li>{@link PropertiesFileSource} – loads configuration from a {@code .properties} file.</li>
 *   <li>{@link YamlFileSource} – loads configuration from a {@code .yml / .yaml} file.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ConfigSource source = ConfigSource.fromFile("config.yml");
 * Properties props = source.load();
 * }</pre>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Define a contract for loading configuration into {@link Properties}.</li>
 *   <li>Provide a factory method {@link #fromFile(String)} that selects the correct
 *       implementation based on file extension.</li>
 * </ul>
 *
 * <h2>Error handling</h2>
 * <p>When an unsupported file extension is provided, the factory method throws
 * an {@link IllegalArgumentException}. Implementations may throw {@link IOException}
 * if the file cannot be read.</p>
 */
public interface ConfigSource {

    /**
     * Loads the configuration from the underlying source.
     *
     * @return configuration as a {@link Properties} object
     * @throws IOException if the file cannot be read or parsed
     */
    Properties load() throws IOException;

    /**
     * Creates a {@link ConfigSource} implementation based on the given file path.
     *
     * <p>The following rules apply:</p>
     * <ul>
     *   <li>Paths ending with {@code .properties} → {@link PropertiesFileSource}</li>
     *   <li>Paths ending with {@code .yml} or {@code .yaml} → {@link YamlFileSource}</li>
     *   <li>Other extensions → {@link IllegalArgumentException}</li>
     * </ul>
     *
     * @param path the path to the configuration file
     * @return a concrete {@link ConfigSource} implementation
     * @throws IllegalArgumentException if the file extension is unsupported
     */
    static ConfigSource fromFile(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".properties")) return new PropertiesFileSource(path);
        if (p.endsWith(".yml") || p.endsWith(".yaml")) return new YamlFileSource(path);
        throw new IllegalArgumentException("Unsupported config format: " + path);
    }
}

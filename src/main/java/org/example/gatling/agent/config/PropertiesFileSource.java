package org.example.gatling.agent.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * {@link ConfigSource} implementation for loading configuration from a traditional
 * Java {@code .properties} file.
 *
 * <p>This class reads a given file path and loads its key-value pairs
 * into a {@link Properties} object. It is primarily used by {@link ConfigLoader}
 * to support property-based configuration files.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ConfigSource src = new PropertiesFileSource("config.properties");
 * Properties props = src.load();
 * String url = props.getProperty("prom.pushgateway.url");
 * }</pre>
 *
 * <p>File format should follow the standard Java {@code .properties} conventions,
 * e.g.:</p>
 * <pre>
 * prom.pushgateway.url=http://localhost:9091
 * prom.job=gatling
 * prom.instance=default
 * prom.push.period.seconds=5
 * prom.delete.on.stop=true
 * prom.histogram.buckets.ms=50,100,200,500,1000
 * </pre>
 */
final class PropertiesFileSource implements ConfigSource {

    /** Path to the {@code .properties} file on disk. */
    private final String filePath;

    /**
     * Creates a new {@code PropertiesFileSource}.
     *
     * @param filePath path to a valid {@code .properties} file
     */
    PropertiesFileSource(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Loads the properties file into memory.
     *
     * @return a populated {@link Properties} object
     * @throws IOException if the file cannot be read or parsed
     */
    @Override
    public Properties load() throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(filePath)) {
            props.load(in);
        }
        return props;
    }
}

package org.example.gatling.agent.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * {@link ConfigSource} implementation for loading configuration
 * from a YAML file.
 *
 * <p>This class reads a YAML configuration file and converts
 * its hierarchical structure into a flat {@link Properties} object
 * using {@link YamlFlattener}. This allows YAML configs to be
 * treated the same way as {@code .properties} files when consumed
 * by the rest of the system.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * ConfigSource src = new YamlFileSource("config.yml");
 * Properties props = src.load();
 * String url = props.getProperty("prom.pushgateway.url");
 * }</pre>
 *
 * <h2>Example YAML:</h2>
 * <pre>
 * prom:
 *   pushgateway:
 *     url: http://localhost:9091
 *     user: admin
 *     password: secret
 *   job: gatling
 *   instance: default
 *   push:
 *     period:
 *       seconds: 5
 *   delete:
 *     on:
 *       stop: true
 *   histogram:
 *     buckets:
 *       ms: [50, 100, 200, 500, 1000]
 * </pre>
 *
 * <p>The {@link YamlFlattener} will transform the above into
 * flattened keys such as:</p>
 * <pre>
 * prom.pushgateway.url=http://localhost:9091
 * prom.pushgateway.user=admin
 * prom.job=gatling
 * prom.histogram.buckets.ms=50,100,200,500,1000
 * </pre>
 */
final class YamlFileSource implements ConfigSource{

    /** Path to the YAML file on disk. */
    private final String filePath;

    /**
     * Creates a new {@code YamlFileSource}.
     *
     * @param filePath path to a valid YAML configuration file
     */
    YamlFileSource(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Loads the YAML file, parses it into an object tree,
     * and flattens it into {@link Properties}.
     *
     * @return a populated {@link Properties} object
     * @throws IOException if the file cannot be read
     */
    @Override
    public Properties load() throws IOException {
        try (InputStream in = new FileInputStream(filePath)) {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object root = yaml.load(in);
            return YamlFlattener.flatten(root);
        }
    }
}

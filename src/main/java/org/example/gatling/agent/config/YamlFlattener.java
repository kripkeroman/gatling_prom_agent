package org.example.gatling.agent.config;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class that converts hierarchical YAML/JSON-like
 * structures into flat {@link Properties}.
 *
 * <p>This is typically used after parsing a YAML configuration
 * file, so that keys like:</p>
 *
 * <pre>
 * prom:
 *   pushgateway:
 *     url: http://localhost:9091
 *     user: admin
 *   job: gatling
 *   histogram:
 *     buckets:
 *       ms: [50, 100, 200, 500, 1000]
 * </pre>
 *
 * <p>Are transformed into:</p>
 *
 * <pre>
 * prom.pushgateway.url=http://localhost:9091
 * prom.pushgateway.user=admin
 * prom.job=gatling
 * prom.histogram.buckets.ms=50,100,200,500,1000
 * </pre>
 *
 * <p>The class handles:</p>
 * <ul>
 *   <li>{@link Map} — Recursively flattens nested maps into dot-separated keys.</li>
 *   <li>{@link Collection} — Joins elements with commas.</li>
 *   <li>Arrays — Converted to comma-separated lists.</li>
 *   <li>Scalars (strings, numbers, booleans) — Stored directly.</li>
 * </ul>
 *
 * <p>It does <b>not</b> support advanced YAML constructs (anchors,
 * references, custom tags), as those are typically resolved by
 * SnakeYAML before reaching this stage.</p>
 */
final class YamlFlattener {

    /** Utility class — prevent instantiation. */
    private YamlFlattener() {}

    /**
     * Converts a parsed YAML root object into flat properties.
     *
     * @param root the root object from SnakeYAML parsing
     *             (can be a Map, List, Array, String, etc.)
     * @return a {@link Properties} object with flattened keys
     */
    static Properties flatten(Object root) {
        Properties props = new Properties();
        if (root == null) return props;
        flattenNode(root, "", props);
        return props;
    }

    /**
     * Recursively flattens a node into properties.
     *
     * @param node   the current node (map, collection, array, or scalar)
     * @param prefix current key prefix
     * @param props  target properties object
     */
    @SuppressWarnings("unchecked")
    private static void flattenNode(Object node, String prefix, Properties props) {
        if (node instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) node;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String childKey = String.valueOf(e.getKey());
                String fullKey = prefix == null || prefix.isEmpty()
                        ? childKey
                        : prefix + "." + childKey;
                flattenNode(e.getValue(), fullKey, props);
            }
            return;
        }

        if (node instanceof Collection<?>) {
            String joined = ((Collection<?>) node).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            props.setProperty(prefix, joined);
            return;
        }

        if (node != null && node.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(node);
            List<String> parts = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                parts.add(String.valueOf(java.lang.reflect.Array.get(node, i)));
            }
            props.setProperty(prefix, String.join(",", parts));
            return;
        }

        if (node != null) {
            props.setProperty(prefix, String.valueOf(node));
        }
    }
}

package org.example.gatling.agent;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Utility class for extracting contextual information from Gatling's
 * internal {@code DataWriterMessage} objects.
 *
 * <p>In Gatling, several lifecycle callbacks (onInit, onStop, onCrash, etc.)
 * provide a {@code data} object that may contain metadata about the
 * running simulation. However, its type is internal and subject to change
 * across Gatling versions. To avoid compile-time dependencies, this class
 * uses reflection to query the object for specific methods.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Safely attempt to extract the simulation class name
 *       (via {@link #extractSimulation(Object)}).</li>
 *   <li>Safely attempt to extract the unique run identifier
 *       (via {@link #extractRunId(Object)}).</li>
 *   <li>Fail gracefully (returning {@link Optional#empty()})
 *       if the data object does not expose the expected method,
 *       or if reflection fails for any reason.</li>
 * </ul>
 *
 * <h2>Example usage</h2>
 * <pre>{@code
 * Object data = ... // passed by Gatling hook
 *
 * ContextInfoExtractor.extractSimulation(data)
 *     .ifPresent(sim -> log.info("Simulation class: {}", sim));
 *
 * ContextInfoExtractor.extractRunId(data)
 *     .ifPresent(runId -> log.info("Run identifier: {}", runId));
 * }</pre>
 *
 * <h2>Thread-safety</h2>
 * This class is stateless and thread-safe. Each invocation of
 * {@code extractMethod} performs independent reflection.
 *
 * <h2>Design notes</h2>
 * This indirection isolates reflection-based lookups from core logic
 * in {@link PrometheusMirror}, keeping responsibilities clear:
 * <ul>
 *   <li>{@link PrometheusMirror} handles lifecycle + metrics.</li>
 *   <li>{@link ContextInfoExtractor} handles optional context metadata.</li>
 * </ul>
 */
public class ContextInfoExtractor {

    private ContextInfoExtractor() {}

    /**
     * Attempt to extract the simulation class name from the given
     * Gatling {@code data} object.
     *
     * @param data the object provided by Gatling hooks
     * @return the simulation class name if available, otherwise empty
     */
    public static Optional<String> extractSimulation(Object data) {
        return extractMethod(data, "simulationClassName");
    }

    /**
     * Attempt to extract the run ID from the given Gatling {@code data} object.
     *
     * @param data the object provided by Gatling hooks
     * @return the run identifier if available, otherwise empty
     */
    public static Optional<String> extractRunId(Object data) {
        return extractMethod(data, "runId");
    }

    /**
     * Core helper method that tries to invoke a named no-arg method
     * reflectively on the given object.
     *
     * @param data the object to inspect
     * @param methodName name of the method to invoke
     * @return an Optional containing the method result (as String),
     *         or empty if invocation fails or result is null
     */
    private static Optional<String> extractMethod(Object data, String methodName) {
        if (data == null) return Optional.empty();
        try {
            Method m = data.getClass().getMethod(methodName);
            Object res = m.invoke(data);
            if (res != null) return Optional.of(res.toString());
        } catch (Exception ignored) {}
        return Optional.empty();
    }
}


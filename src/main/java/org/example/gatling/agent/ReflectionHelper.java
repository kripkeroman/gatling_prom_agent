package org.example.gatling.agent;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Utility class to simplify and optimize reflective method invocation.
 * <p>
 * Gatling's internal event classes (e.g. {@code UserStart}, {@code UserEnd}, {@code Response})
 * are Scala case classes without a stable Java API. To avoid compile-time dependencies
 * and brittle code, this helper uses Java Reflection to dynamically call methods
 * (such as {@code scenario()}, {@code name()}, {@code status()}, etc.).
 * <p>
 * To improve performance, it caches discovered {@link Method} instances
 * per target class, so repeated calls on the same type do not require
 * method lookup via reflection.
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Thread-safe method cache using {@link ConcurrentHashMap}.</li>
 *   <li>Automatically sets methods accessible to bypass visibility restrictions.</li>
 *   <li>Throws {@link RuntimeException} if reflection fails, with contextual info.</li>
 * </ul>
 *
 * <h3>Example usage:</h3>
 * <pre>{@code
 * ReflectionHelper refl = new ReflectionHelper();
 * Object scenario = refl.invoke(gatlingEvent, "scenario");
 * Object status   = refl.invoke(gatlingEvent, "status");
 * }</pre>
 */
public class ReflectionHelper {

    /**
     * Cache of reflected methods keyed by declaring class.
     * Ensures that reflective lookups happen only once per method/class combination.
     */
    private final ConcurrentHashMap<Class<?>, Method> cache = new ConcurrentHashMap<>();

    /**
     * Invokes a no-argument method by name on the given target object.
     *
     * @param target the object instance to call the method on (must not be {@code null})
     * @param method the method name to invoke (must not be {@code null} or empty)
     * @return the return value of the method call (may be {@code null} if the method returns void or null)
     * @throws RuntimeException if the method cannot be found or invoked
     */
    public Object invoke(Object target, String method) {
        try {
            Method m = cache.computeIfAbsent(target.getClass(), c -> {
                try {
                    Method mm = c.getMethod(method);
                    mm.setAccessible(true);
                    return mm;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return m.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException("Reflection failed: " + method + " on " + target.getClass(), e);
        }
    }
}

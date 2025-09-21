package org.example.gatling.agent;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice;

/**
 * Collection of ByteBuddy advice classes used to intercept
 * Gatling's {@code LogFileDataWriter} lifecycle methods.
 *
 * <p>This class acts as a bridge between Gatling's internal events
 * and the {@link PrometheusMirror}, which translates them into
 * Prometheus metrics. Each nested static class corresponds to a
 * specific lifecycle callback method in Gatling and delegates
 * handling to the mirror.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Install hooks for Gatling lifecycle methods
 *       ({@code onInit}, {@code onMessage}, {@code onFlush},
 *       {@code onStop}, {@code onCrash}).</li>
 *   <li>Safely delegate captured calls to {@link PrometheusMirror},
 *       logging any exceptions without breaking Gatling execution.</li>
 *   <li>Ensure that Gatling remains stable even if metric
 *       reporting fails.</li>
 * </ul>
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>Each advice class is final and static, with a single
 *       {@code @Advice.OnMethodExit} hook that matches
 *       Gatling's method signature.</li>
 *   <li>All exceptions are caught and logged locally to prevent
 *       propagation into Gatling internals.</li>
 * </ul>
 */
@Slf4j
public class Hooks {

    private Hooks() {}

    /**
     * Advice that runs after Gatling's {@code onInit} to initialize
     * the {@link PrometheusMirror}.
     */
    public static final class InitAdvice {
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void after() {
            PrometheusMirror.onInit();
        }
    }

    /**
     * Advice that runs after Gatling's {@code onMessage}.
     * Captures both the message object and optional context data,
     * then forwards them to {@link PrometheusMirror#onMessage(Object, Object)}.
     */
    public static final class MessageAdvice {
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void after(@Advice.AllArguments Object[] args) {
            try {
                Object msg  = args != null && args.length > 0 ? args[0] : null;
                Object data = args != null && args.length > 1 ? args[1] : null;
                PrometheusMirror.onMessage(msg, data);
            } catch (Throwable t) {
                log.error("[GatlingPromAgent] onMessage mirror error: {}", String.valueOf(t));
            }
        }
    }

    /**
     * Advice that runs after Gatling's {@code onFlush}.
     * Forwards the optional context data to
     * {@link PrometheusMirror#onFlush(Object)}.
     */
    public static final class FlushAdvice {
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void after(@Advice.AllArguments Object[] args) {
            try {
                Object data = args != null && args.length > 0 ? args[0] : null;
                PrometheusMirror.onFlush(data);
            } catch (Throwable t) {
                log.error("[GatlingPromAgent] onFlush mirror error: {}", String.valueOf(t));
            }
        }
    }

    /**
     * Advice that runs after Gatling's {@code onStop}.
     * Delegates to {@link PrometheusMirror#onStop(Object)} to
     * handle cleanup and optional deletion of metrics.
     */
    public static final class StopAdvice {
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void after(@Advice.AllArguments Object[] args) {
            try {
                Object data = args != null && args.length > 0 ? args[0] : null;
                PrometheusMirror.onStop(data);
            } catch (Throwable t) {
                log.error("[GatlingPromAgent] onStop mirror error: {}", String.valueOf(t));
            }
        }
    }

    /**
     * Advice that runs after Gatling's {@code onCrash}.
     * Delegates to {@link PrometheusMirror#onCrash(String, Object)},
     * providing both the cause and the optional context data.
     */
    public static final class CrashAdvice {
        @Advice.OnMethodExit(suppress = Throwable.class)
        public static void after(@Advice.AllArguments Object[] args) {
            try {
                String cause = args != null && args.length > 0 && args[0] != null ? args[0].toString() : "unknown";
                Object data  = args != null && args.length > 1 ? args[1] : null;
                PrometheusMirror.onCrash(cause, data);
            } catch (Throwable t) {
                log.error("[GatlingPromAgent] onCrash mirror error: {}", String.valueOf(t));
            }
        }
    }
}

package org.example.gatling.agent;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Java agent entry point for the Gatling → Prometheus Pushgateway integration.
 *
 * <p>This agent uses <b>ByteBuddy</b> to dynamically instrument Gatling's
 * {@code io.gatling.core.stats.writer.LogFileDataWriter} class at runtime.
 * By injecting hooks into its lifecycle methods, the agent mirrors simulation
 * events into Prometheus metrics via {@link PrometheusMirror}.
 *
 * <h2>Lifecycle</h2>
 * The JVM automatically invokes the {@link #premain(String, Instrumentation)} method
 * when the agent JAR is attached via the {@code -javaagent} parameter:
 * <pre>{@code
 * java -javaagent:gatling-prom-agent.jar=prom.config.file=config.yml -jar gatling-bundle.jar
 * }</pre>
 *
 * <h2>Instrumentation</h2>
 * The agent:
 * <ul>
 *   <li>Ignores shaded/internal libraries and common system packages
 *       (Java, JDK, javax, slf4j, logback, scala, kotlin, etc.).</li>
 *   <li>Targets only {@code io.gatling.core.stats.writer.LogFileDataWriter}.</li>
 *   <li>Injects advice hooks into the following methods:
 *     <ul>
 *       <li>{@code onInit} → {@link Hooks.InitAdvice}</li>
 *       <li>{@code onMessage} → {@link Hooks.MessageAdvice}</li>
 *       <li>{@code onFlush} → {@link Hooks.FlushAdvice}</li>
 *       <li>{@code onStop} → {@link Hooks.StopAdvice}</li>
 *       <li>{@code onCrash} → {@link Hooks.CrashAdvice}</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Thread-Safety</h2>
 * Installation is guarded by an {@link AtomicBoolean} to ensure that the agent
 * is only installed once, even if {@link #premain} is invoked multiple times.
 *
 * <h2>Usage Example</h2>
 * Add the following to the Gatling startup command:
 * <pre>{@code
 * JAVA_OPTS="-javaagent:/path/to/gatling-prom-agent.jar"
 * ./bin/gatling.sh -s MySimulation
 * }</pre>
 *
 * The agent will automatically instrument Gatling and push metrics to the
 * configured Prometheus Pushgateway.
 *
 * @see PrometheusMirror
 * @see Hooks
 * @see AgentBuilder
 */
@Slf4j
public final class Agent {

    /** Ensures that the agent is installed only once. */
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private Agent() {}

    /**
     * Entry point for the Java agent, invoked automatically by the JVM
     * when {@code -javaagent} is specified.
     *
     * @param args agent arguments (e.g., config file path).
     * @param inst JVM instrumentation handle.
     */
    public static void premain(String args, Instrumentation inst) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }

        new AgentBuilder.Default()
                .ignore(ElementMatchers.nameStartsWith("shaded.")
                        .or(ElementMatchers.nameStartsWith("java."))
                        .or(ElementMatchers.nameStartsWith("jdk."))
                        .or(ElementMatchers.nameStartsWith("sun."))
                        .or(ElementMatchers.nameStartsWith("javax."))
                        .or(ElementMatchers.nameStartsWith("scala."))
                        .or(ElementMatchers.nameStartsWith("kotlin."))
                        .or(ElementMatchers.nameStartsWith("org.slf4j."))
                        .or(ElementMatchers.nameStartsWith("ch.qos.logback."))
                        .or(ElementMatchers.isSynthetic()))
                .type(ElementMatchers.named("io.gatling.core.stats.writer.LogFileDataWriter"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder
                                .visit(Advice.to(Hooks.InitAdvice.class)
                                        .on(ElementMatchers.named("onInit")))
                                .visit(Advice.to(Hooks.MessageAdvice.class)
                                        .on(ElementMatchers.named("onMessage")))
                                .visit(Advice.to(Hooks.FlushAdvice.class)
                                        .on(ElementMatchers.named("onFlush")))
                                .visit(Advice.to(Hooks.StopAdvice.class)
                                        .on(ElementMatchers.named("onStop")))
                                .visit(Advice.to(Hooks.CrashAdvice.class)
                                        .on(ElementMatchers.named("onCrash")))
                )
                .asTerminalTransformation()
                .installOn(inst);

        log.info("[GatlingPromAgent] installed");
    }
}

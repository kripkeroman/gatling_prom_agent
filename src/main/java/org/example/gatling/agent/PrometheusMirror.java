package org.example.gatling.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.gatling.agent.config.PrometheusConfig;
import org.example.gatling.agent.handler.MessageHandler;
import org.example.gatling.agent.metrics.Aggregator;
import org.example.gatling.agent.metrics.MetricRegistryFactory;
import org.example.gatling.agent.metrics.Metrics;
import org.example.gatling.agent.push.PushGatewayService;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central coordinator that mirrors Gatling's runtime events into Prometheus metrics
 * and pushes them to a configured PushGateway.
 *
 * <p>This class is the core integration point between Gatling and Prometheus.
 * It is invoked through ByteBuddy advices installed by the {@link Hooks} class
 * and translates Gatling lifecycle events into Prometheus metrics using
 * {@link Metrics}, {@link Aggregator}, and {@link PushGatewayService}.</p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Initialize the Prometheus integration during Gatling startup
 *       ({@link #onInit()}).</li>
 *   <li>Handle user, request, and response events coming from Gatling
 *       ({@link #onMessage(Object, Object)}).</li>
 *   <li>Flush intermediate metrics during simulation
 *       ({@link #onFlush(Object)}).</li>
 *   <li>Perform cleanup and optional deletion of metrics when the simulation ends
 *       ({@link #onStop(Object)}).</li>
 *   <li>Capture simulation crashes and ensure final metric push
 *       ({@link #onCrash(String, Object)}).</li>
 * </ul>
 *
 * <h2>Threading & Concurrency</h2>
 * <ul>
 *   <li>Metrics are stored in a Prometheus {@link io.prometheus.client.CollectorRegistry}.</li>
 *   <li>Pushes to the PushGateway are scheduled using a
 *       {@link ScheduledExecutorService} at a configurable interval.</li>
 *   <li>Aggregate values (min, max, mean, stddev) are updated via Welford’s
 *       online algorithm with explicit synchronization on a lock.</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * <ul>
 *   <li>Configuration is loaded from {@link PrometheusConfig}, which in turn
 *       reads YAML/Properties files, system properties, or environment variables.</li>
 *   <li>PushGateway details (URL, job, instance, auth, period, delete-on-stop)
 *       are fully configurable.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #onInit()} is called after Gatling's DataWriter init.</li>
 *   <li>{@link #onMessage(Object, Object)} is called for each user start/end
 *       and request/response event.</li>
 *   <li>{@link #onFlush(Object)} may be called periodically to push partial results.</li>
 *   <li>{@link #onStop(Object)} runs at the end of the simulation.</li>
 *   <li>{@link #onCrash(String, Object)} is triggered when a simulation crashes.</li>
 * </ol>
 */
@Slf4j
public class PrometheusMirror {


    /** Initialization guard to prevent double setup. */
    private static volatile boolean initialized=false;

    /** Loaded configuration for PushGateway + metrics. */
    private static PrometheusConfig config;

    /** Prometheus metric registry and collectors. */
    private static Metrics metrics;

    /** Service for pushing metrics to PushGateway. */
    private static PushGatewayService pushService;

    /** Aggregates response times for min/max/mean/stddev. */
    private static Aggregator aggregator;

    /** Handler that maps Gatling messages into metric updates. */
    private static MessageHandler handler;

    /** Scheduler for periodic heartbeat and PushGateway pushes. */
    private static ScheduledExecutorService scheduler;

    /** Labels attached to all pushed metrics (instance, runId, etc). */
    private static Map<String, String> groupingKey;

    /** Atomic guard used during stop/crash transitions. */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /** Lock object for Welford’s algorithm updates. */
    private static final Object welfordLock = new Object();

    // Welford’s online algorithm state
    private static long n = 0;
    private static double mean = 0.0;
    private static double m2 = 0.0;
    private static final AtomicLong minObserved = new AtomicLong(Long.MAX_VALUE);
    private static final AtomicLong maxObserved = new AtomicLong(0);

    /**
     * Initializes Prometheus integration.
     * <ul>
     *   <li>Loads configuration.</li>
     *   <li>Registers Prometheus metrics.</li>
     *   <li>Sets up aggregator, handler, and push service.</li>
     *   <li>Schedules periodic pushes to the PushGateway.</li>
     * </ul>
     */
    public static void onInit(){
        if(initialized) return;
        try{
            config      = PrometheusConfig.load();
            metrics     = MetricRegistryFactory.create(config.bucketsMs());
            aggregator  = new Aggregator();
            handler     = new MessageHandler(metrics,aggregator);
            pushService = new PushGatewayService(config);
            scheduler   = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> { metrics.heartbeat().set(System.currentTimeMillis()/1000.0);
                pushService.push(metrics.registry());
                },0,config.pushPeriodSec(), TimeUnit.SECONDS);
            initialized=true;
            log.info("[GatlingPromAgent] Initialized: {}",config);
        }catch(Exception e){
            log.error("Init failed",e);
        }
    }

    /**
     * Handles a Gatling message.
     * <ul>
     *   <li>Updates metrics for UserStart, UserEnd, or Response messages.</li>
     *   <li>Updates grouping labels with simulation metadata when available.</li>
     * </ul>
     *
     * @param msg  Gatling event object (user or response event).
     * @param data Context object containing simulation metadata (optional).
     */
    public static void onMessage(Object msg,Object data){
        if(!initialized) return;
        handler.handle(msg);
        if (data != null) {
            ContextInfoExtractor.extractSimulation(data)
                    .ifPresent(sim -> groupingKey.put("simulation", sim));
            ContextInfoExtractor.extractRunId(data) .ifPresent(runId -> groupingKey.put("runId", runId));
        }
    }

    /**
     * Forces a push of current metrics to the PushGateway,
     * optionally enriching them with simulation info.
     *
     * @param data simulation context (may provide simulation name).
     */
    public static void onFlush(Object data){
        if (data != null) {
            ContextInfoExtractor.extractSimulation(data) .ifPresent(sim -> groupingKey.put("simulation", sim));
        } if(initialized) pushService.push(metrics.registry());
    }

    /**
     * Stops metric reporting when a simulation finishes.
     * <ul>
     *   <li>Shuts down the scheduler.</li>
     *   <li>Updates final aggregate gauges.</li>
     *   <li>Pushes metrics one last time.</li>
     *   <li>Deletes metrics if {@code deleteOnStop=true}.</li>
     * </ul>
     *
     * @param data simulation context (may provide simulation name).
     */
    public static void onStop(Object data){
        try {
            if (scheduler != null) scheduler.shutdownNow();
            if (data != null) {
                ContextInfoExtractor.extractSimulation(data)
                        .ifPresent(sim -> log.info("[GatlingPromAgent] Stopping simulation {}", sim));
            }
            refreshAggregateGauges();
            safePush();
            if (config.deleteOnStop() && pushService != null) {
                pushService.delete(metrics.registry());
            }
        } catch (Throwable t) {
            log.error("[GatlingPromAgent] onStop error", t);
        } finally {
            INITIALIZED.set(false);
        }
    }

    /**
     * Handles simulation crashes by logging cause and ensuring
     * a final metrics push to the PushGateway.
     *
     * @param cause textual description of crash reason.
     * @param data  simulation context (optional).
     */
    public static void onCrash(String cause, Object data) {
        log.error("[GatlingPromAgent] crash: {}", cause);
        if (data != null) {
            ContextInfoExtractor.extractSimulation(data)
                    .ifPresent(sim -> log.error("[GatlingPromAgent] crash in simulation {}", sim));
        } if (INITIALIZED.get() && pushService != null) {
            pushService.pushSafe(metrics.registry());
        }
    }

    /**
     * Helper that attempts to push current metrics safely,
     * catching and logging all exceptions.
     */
    private static void safePush() {
        if (!initialized || pushService == null) return;
        try {
            pushService.push(metrics.registry());
        } catch (Exception e) {
            log.warn("[GatlingPromAgent] push failed: {}", e.getMessage());
        }
    }

    /**
     * Recomputes and updates aggregate gauges:
     * <ul>
     *   <li>Minimum response time.</li>
     *   <li>Maximum response time.</li>
     *   <li>Mean response time.</li>
     *   <li>Standard deviation of response time.</li>
     * </ul>
     *
     * <p>Uses Welford’s online algorithm for numerical stability.</p>
     */
    private static void refreshAggregateGauges() {
        long cnt = n;
        if (cnt == 0) return;
        double meanLocal;
        double varianceLocal;
        long minLocal = minObserved.get();
        long maxLocal = maxObserved.get();
        synchronized (welfordLock) {
            meanLocal = mean; varianceLocal = (n > 1) ? (m2 / (n - 1)) : 0.0;
        }
        double stddevLocal = Math.sqrt(varianceLocal);
        metrics.minResponseMs().set(minLocal);
        metrics.maxResponseMs().set(maxLocal);
        metrics.meanResponseMs().set(meanLocal);
        metrics.stdDevResponseMs().set(stddevLocal);
    }
}

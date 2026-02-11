package be.steby.CoreProject.bll.domains.monitoring.services;

import be.steby.CoreProject.bll.domains.monitoring.models.ExecutorMetrics;

import java.util.List;
import java.util.Optional;

/**
 * Service for monitoring ThreadPoolTaskExecutor instances.
 *
 * <p>Provides real-time metrics for all configured async executors,
 * enabling detection of saturation, bottlenecks, and capacity issues.</p>
 *
 * <h4>Monitored executors:</h4>
 * <ul>
 *   <li>email - Email sending operations</li>
 *   <li>sms - SMS sending operations (Twilio)</li>
 *   <li>activityLog - Activity log persistence</li>
 *   <li>eventListener - Event listener processing</li>
 *   <li>geocoding - External geocoding API calls</li>
 *   <li>general - General purpose async tasks</li>
 *   <li>securityMonitoring - Security monitoring tasks</li>
 * </ul>
 *
 * <h4>Why monitor executors?</h4>
 * <p>Circuit breakers protect against external service failures, but executor
 * saturation can cause task rejection BEFORE the external call is even made.
 * This creates silent failures that circuit breakers cannot detect.</p>
 *
 * @see be.steby.CoreProject.il.configs.AsyncConfig
 * @see ExecutorMetrics
 */
public interface ExecutorMonitoringService {

    /**
     * Retrieves metrics for all registered executors.
     *
     * <p>Returns a snapshot of each executor's current state including
     * active threads, queue size, and completed task count.</p>
     *
     * @return List of ExecutorMetrics for all executors, never null
     */
    List<ExecutorMetrics> getAllMetrics();

    /**
     * Retrieves metrics for a specific executor by name.
     *
     * @param name Executor name (e.g., "email", "sms", "geocoding")
     * @return Optional containing metrics if executor exists, empty otherwise
     */
    Optional<ExecutorMetrics> getMetrics(String name);

    /**
     * Returns the names of all registered executors.
     *
     * @return List of executor names
     */
    List<String> getExecutorNames();

    /**
     * Checks if any executor is showing signs of saturation.
     *
     * <p>Useful for quick health checks and alerting.</p>
     *
     * @return true if at least one executor is saturated
     * @see ExecutorMetrics#isSaturated()
     */
    boolean hasAnySaturated();
}
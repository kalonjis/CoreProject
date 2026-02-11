package be.steby.CoreProject.pl.domains.monitoring.models.responses;

import java.util.List;

/**
 * Aggregated metrics response for the monitoring dashboard.
 *
 * <p>Used by: GET /api/monitoring/metrics/dashboard</p>
 *
 * <p>Contains all system metrics for the admin health dashboard including
 * JVM, CPU, disk, database pool, and async executor metrics.</p>
 *
 * @param jvm       JVM heap memory metrics
 * @param cpu       CPU usage metrics
 * @param disk      Disk space metrics
 * @param dbPool    Database connection pool metrics
 * @param executors Async executor thread pool metrics
 * @param uptime    Application uptime in seconds
 */
public record DashboardMetricsResponse(
        JvmMetricsResponse jvm,
        CpuMetricsResponse cpu,
        DiskMetricsResponse disk,
        DbPoolMetricsResponse dbPool,
        List<ExecutorMetricsResponse> executors,
        double uptime
) {

    // =========================================================================
    // NESTED RESPONSE RECORDS
    // =========================================================================

    public record JvmMetricsResponse(
            long heapUsed,
            long heapMax,
            int heapUsedPercent
    ) {}

    public record CpuMetricsResponse(
            int systemUsage,
            int processUsage
    ) {}

    public record DiskMetricsResponse(
            long free,
            long total,
            int freePercent
    ) {}

    public record DbPoolMetricsResponse(
            int active,
            int idle,
            int max
    ) {}

    /**
     * Async executor thread pool metrics.
     *
     * <p>Provides visibility into async task processing capacity and load.</p>
     *
     * <h4>Key indicators:</h4>
     * <ul>
     *   <li>{@code queueUsagePercent > 70%} = Warning (approaching saturation)</li>
     *   <li>{@code queueUsagePercent > 90%} = Critical (risk of task rejection)</li>
     *   <li>{@code saturated = true} = Executor overloaded</li>
     * </ul>
     *
     * @param name               Executor identifier (e.g., "email", "sms")
     * @param activeCount        Threads currently executing tasks
     * @param poolSize           Current threads in the pool
     * @param corePoolSize       Configured minimum threads
     * @param maxPoolSize        Configured maximum threads
     * @param queueSize          Tasks waiting in queue
     * @param queueCapacity      Maximum queue capacity (-1 if unbounded)
     * @param queueUsagePercent  Queue usage percentage (0-100)
     * @param completedTaskCount Total tasks completed since startup
     * @param saturated          True if executor shows saturation signs
     */
    public record ExecutorMetricsResponse(
            String name,
            int activeCount,
            int poolSize,
            int corePoolSize,
            int maxPoolSize,
            int queueSize,
            int queueCapacity,
            int queueUsagePercent,
            long completedTaskCount,
            boolean saturated
    ) {}
}
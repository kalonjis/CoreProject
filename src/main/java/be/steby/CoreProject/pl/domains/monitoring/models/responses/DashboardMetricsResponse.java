package be.steby.CoreProject.pl.domains.monitoring.models.responses;

/**
 * Aggregated metrics response for the monitoring dashboard.
 *
 * Used by: GET /api/monitoring/metrics/dashboard
 *
 * @param jvm    JVM heap memory metrics
 * @param cpu    CPU usage metrics
 * @param disk   Disk space metrics
 * @param dbPool Database connection pool metrics
 * @param uptime Application uptime in seconds
 */
public record DashboardMetricsResponse(
    JvmMetricsResponse jvm,
    CpuMetricsResponse cpu,
    DiskMetricsResponse disk,
    DbPoolMetricsResponse dbPool,
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
}
package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * Aggregated metrics for the monitoring dashboard.
 *
 * @param jvm    JVM heap memory metrics
 * @param cpu    CPU usage metrics
 * @param disk   Disk space metrics
 * @param dbPool Database connection pool metrics
 * @param uptime Application uptime in seconds
 */
public record DashboardMetrics(
    JvmMetrics jvm,
    CpuMetrics cpu,
    DiskMetrics disk,
    DbPoolMetrics dbPool,
    double uptime
) {}
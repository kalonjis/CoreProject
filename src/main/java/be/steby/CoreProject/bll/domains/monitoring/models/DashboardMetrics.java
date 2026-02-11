package be.steby.CoreProject.bll.domains.monitoring.models;

import java.util.List;

/**
 * Aggregated metrics for the monitoring dashboard.
 *
 * <p>Contains all system metrics displayed on the admin health dashboard:</p>
 * <ul>
 *   <li>JVM memory usage</li>
 *   <li>CPU usage (system and process)</li>
 *   <li>Disk space</li>
 *   <li>Database connection pool (HikariCP)</li>
 *   <li>Async executor thread pools</li>
 *   <li>Application uptime</li>
 * </ul>
 *
 * @param jvm       JVM heap memory metrics
 * @param cpu       CPU usage metrics
 * @param disk      Disk space metrics
 * @param dbPool    Database connection pool metrics
 * @param executors Async executor thread pool metrics
 * @param uptime    Application uptime in seconds
 */
public record DashboardMetrics(
        JvmMetrics jvm,
        CpuMetrics cpu,
        DiskMetrics disk,
        DbPoolMetrics dbPool,
        List<ExecutorMetrics> executors,
        double uptime
) {}
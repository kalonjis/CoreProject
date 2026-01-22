package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * CPU usage metrics.
 *
 * @param systemUsage  System-wide CPU usage percentage (0-100)
 * @param processUsage JVM process CPU usage percentage (0-100)
 */
public record CpuMetrics(
    int systemUsage,
    int processUsage
) {}
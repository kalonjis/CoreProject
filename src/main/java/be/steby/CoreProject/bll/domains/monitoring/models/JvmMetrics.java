package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * JVM Heap memory metrics.
 *
 * @param heapUsed       Heap memory currently used (bytes)
 * @param heapMax        Maximum heap memory available (bytes)
 * @param heapUsedPercent Heap usage percentage (0-100)
 */
public record JvmMetrics(
    long heapUsed,
    long heapMax,
    int heapUsedPercent
) {}
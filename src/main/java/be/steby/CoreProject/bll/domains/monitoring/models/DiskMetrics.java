package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * Disk space metrics.
 *
 * @param free        Free disk space (bytes)
 * @param total       Total disk space (bytes)
 * @param freePercent Free space percentage (0-100)
 */
public record DiskMetrics(
    long free,
    long total,
    int freePercent
) {}
package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * Database connection pool metrics (HikariCP).
 *
 * @param active Currently active connections
 * @param idle   Idle connections in pool
 * @param max    Maximum pool size
 */
public record DbPoolMetrics(
    int active,
    int idle,
    int max
) {}
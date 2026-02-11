package be.steby.CoreProject.bll.domains.monitoring.models;

/**
 * Metrics for a ThreadPoolTaskExecutor.
 *
 * <p>Provides real-time snapshot of an executor's state including
 * active threads, queue size, and cumulative statistics.</p>
 *
 * <h4>Usage in monitoring:</h4>
 * <ul>
 *   <li>High {@code queueSize} relative to {@code queueCapacity} = potential saturation</li>
 *   <li>{@code activeCount} constantly at {@code maxPoolSize} = executor overloaded</li>
 *   <li>{@code completedTaskCount} useful for throughput analysis</li>
 * </ul>
 *
 * <h4>Warning thresholds (suggested):</h4>
 * <ul>
 *   <li>Queue usage > 70% = Warning</li>
 *   <li>Queue usage > 90% = Critical (risk of RejectedExecutionException)</li>
 * </ul>
 *
 * @param name             Executor identifier (e.g., "email", "sms", "geocoding")
 * @param activeCount      Threads currently executing tasks
 * @param poolSize         Current number of threads in the pool
 * @param corePoolSize     Configured minimum threads (always kept alive)
 * @param maxPoolSize      Configured maximum threads
 * @param queueSize        Tasks currently waiting in the queue
 * @param queueCapacity    Configured maximum queue capacity
 * @param completedTaskCount Total tasks completed since startup
 */
public record ExecutorMetrics(
    String name,
    int activeCount,
    int poolSize,
    int corePoolSize,
    int maxPoolSize,
    int queueSize,
    int queueCapacity,
    long completedTaskCount
) {

    /**
     * Calculates queue usage as a percentage.
     *
     * @return Queue usage percentage (0-100), or 0 if capacity is 0
     */
    public int queueUsagePercent() {
        if (queueCapacity <= 0) {
            return 0;
        }
        return (int) Math.round((double) queueSize / queueCapacity * 100);
    }

    /**
     * Determines if the executor is potentially saturated.
     *
     * <p>An executor is considered saturated when:</p>
     * <ul>
     *   <li>All threads are active (activeCount == maxPoolSize), AND</li>
     *   <li>Queue is more than 50% full</li>
     * </ul>
     *
     * @return true if executor shows signs of saturation
     */
    public boolean isSaturated() {
        return activeCount >= maxPoolSize && queueUsagePercent() > 50;
    }
}
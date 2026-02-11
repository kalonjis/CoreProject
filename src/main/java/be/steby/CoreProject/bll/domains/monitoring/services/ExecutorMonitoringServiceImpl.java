package be.steby.CoreProject.bll.domains.monitoring.services;

import be.steby.CoreProject.bll.domains.monitoring.models.ExecutorMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link ExecutorMonitoringService}.
 *
 * <p>Retrieves real-time metrics directly from ThreadPoolTaskExecutor beans.
 * No data is stored - all values are live snapshots at the time of the call.</p>
 *
 * <h4>How it works:</h4>
 * <p>The executors Map is injected from {@code AsyncConfig.executors()} bean,
 * which aggregates all configured ThreadPoolTaskExecutor instances.</p>
 *
 * <h4>Thread safety:</h4>
 * <p>ThreadPoolTaskExecutor methods like {@code getActiveCount()} and
 * {@code getQueue().size()} are thread-safe and can be called concurrently.</p>
 *
 * @see be.steby.CoreProject.il.configs.AsyncConfig#executors
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutorMonitoringServiceImpl implements ExecutorMonitoringService {

    /**
     * Map of executor names to their ThreadPoolTaskExecutor instances.
     * Injected from AsyncConfig.executors() bean.
     */
    private final Map<String, ThreadPoolTaskExecutor> executors;

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    @Override
    public List<ExecutorMetrics> getAllMetrics() {
        return executors.entrySet().stream()
            .map(entry -> buildMetrics(entry.getKey(), entry.getValue()))
            .toList();
    }

    @Override
    public Optional<ExecutorMetrics> getMetrics(String name) {
        ThreadPoolTaskExecutor executor = executors.get(name);
        if (executor == null) {
            log.warn("Executor not found: {}", name);
            return Optional.empty();
        }
        return Optional.of(buildMetrics(name, executor));
    }

    @Override
    public List<String> getExecutorNames() {
        return List.copyOf(executors.keySet());
    }

    @Override
    public boolean hasAnySaturated() {
        return executors.entrySet().stream()
            .map(entry -> buildMetrics(entry.getKey(), entry.getValue()))
            .anyMatch(ExecutorMetrics::isSaturated);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Builds ExecutorMetrics from a ThreadPoolTaskExecutor.
     *
     * <p>Extracts queue capacity from the executor's configuration.
     * Note: getRemainingCapacity() returns Integer.MAX_VALUE for unbounded queues,
     * so we calculate capacity from the configured value if available.</p>
     *
     * @param name     Executor name
     * @param executor ThreadPoolTaskExecutor instance
     * @return ExecutorMetrics snapshot
     */
    private ExecutorMetrics buildMetrics(String name, ThreadPoolTaskExecutor executor) {
        int queueSize = executor.getQueueSize();
        int queueCapacity = executor.getQueueCapacity();

        // Fallback: if queueCapacity returns 0, try to calculate from queue
        if (queueCapacity <= 0) {
            int remainingCapacity = executor.getThreadPoolExecutor()
                .getQueue()
                .remainingCapacity();
            // If unbounded (Integer.MAX_VALUE), use a sensible default for display
            queueCapacity = (remainingCapacity == Integer.MAX_VALUE) 
                ? -1  // Indicates unbounded
                : queueSize + remainingCapacity;
        }

        return new ExecutorMetrics(
            name,
            executor.getActiveCount(),
            executor.getPoolSize(),
            executor.getCorePoolSize(),
            executor.getMaxPoolSize(),
            queueSize,
            queueCapacity,
            executor.getThreadPoolExecutor().getCompletedTaskCount()
        );
    }
}
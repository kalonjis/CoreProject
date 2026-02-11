package be.steby.CoreProject.pl.domains.monitoring.models.mappers;

import be.steby.CoreProject.bll.domains.monitoring.models.*;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.CircuitBreakerStatusResponse;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.DashboardMetricsResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for Monitoring domain.
 *
 * <p>Converts BLL models (records) to PL responses (records).
 * Controls explicitly what data is exposed via the API.</p>
 *
 * <h4>Mapping strategy:</h4>
 * <ul>
 *   <li>BLL records contain domain logic (e.g., calculated percentages)</li>
 *   <li>Response records are flat DTOs optimized for JSON serialization</li>
 *   <li>Calculated fields (like queueUsagePercent) are pre-computed here</li>
 * </ul>
 */
@Component
public class MonitoringMapper {

    // =========================================================================
    // DASHBOARD METRICS
    // =========================================================================

    /**
     * Maps DashboardMetrics (BLL) to DashboardMetricsResponse (PL).
     *
     * @param model BLL dashboard metrics
     * @return Response DTO for API
     */
    public DashboardMetricsResponse toResponse(DashboardMetrics model) {
        return new DashboardMetricsResponse(
                toJvmResponse(model.jvm()),
                toCpuResponse(model.cpu()),
                toDiskResponse(model.disk()),
                toDbPoolResponse(model.dbPool()),
                toExecutorResponseList(model.executors()),
                model.uptime()
        );
    }

    private DashboardMetricsResponse.JvmMetricsResponse toJvmResponse(JvmMetrics model) {
        return new DashboardMetricsResponse.JvmMetricsResponse(
                model.heapUsed(),
                model.heapMax(),
                model.heapUsedPercent()
        );
    }

    private DashboardMetricsResponse.CpuMetricsResponse toCpuResponse(CpuMetrics model) {
        return new DashboardMetricsResponse.CpuMetricsResponse(
                model.systemUsage(),
                model.processUsage()
        );
    }

    private DashboardMetricsResponse.DiskMetricsResponse toDiskResponse(DiskMetrics model) {
        return new DashboardMetricsResponse.DiskMetricsResponse(
                model.free(),
                model.total(),
                model.freePercent()
        );
    }

    private DashboardMetricsResponse.DbPoolMetricsResponse toDbPoolResponse(DbPoolMetrics model) {
        return new DashboardMetricsResponse.DbPoolMetricsResponse(
                model.active(),
                model.idle(),
                model.max()
        );
    }

    // =========================================================================
    // EXECUTOR METRICS
    // =========================================================================

    /**
     * Maps list of ExecutorMetrics (BLL) to list of ExecutorMetricsResponse (PL).
     *
     * @param models List of BLL executor metrics
     * @return List of response DTOs
     */
    private List<DashboardMetricsResponse.ExecutorMetricsResponse> toExecutorResponseList(
            List<ExecutorMetrics> models) {
        if (models == null) {
            return List.of();
        }
        return models.stream()
                .map(this::toExecutorResponse)
                .toList();
    }

    /**
     * Maps single ExecutorMetrics (BLL) to ExecutorMetricsResponse (PL).
     *
     * <p>Pre-computes derived fields (queueUsagePercent, saturated) from
     * the BLL model's methods to avoid client-side calculation.</p>
     *
     * @param model BLL executor metrics
     * @return Response DTO
     */
    private DashboardMetricsResponse.ExecutorMetricsResponse toExecutorResponse(
            ExecutorMetrics model) {
        return new DashboardMetricsResponse.ExecutorMetricsResponse(
                model.name(),
                model.activeCount(),
                model.poolSize(),
                model.corePoolSize(),
                model.maxPoolSize(),
                model.queueSize(),
                model.queueCapacity(),
                model.queueUsagePercent(),
                model.completedTaskCount(),
                model.isSaturated()
        );
    }

    // =========================================================================
    // CIRCUIT BREAKERS
    // =========================================================================

    /**
     * Maps CircuitBreakerStatus (BLL) to CircuitBreakerStatusResponse (PL).
     *
     * @param model BLL circuit breaker status
     * @return Response DTO
     */
    public CircuitBreakerStatusResponse toResponse(CircuitBreakerStatus model) {
        return new CircuitBreakerStatusResponse(
                model.name(),
                model.state(),
                model.failureRate(),
                model.slowCallRate(),
                model.bufferedCalls(),
                model.failedCalls(),
                model.successfulCalls(),
                model.notPermittedCalls()
        );
    }

    /**
     * Maps list of CircuitBreakerStatus (BLL) to list of responses (PL).
     *
     * @param models List of BLL circuit breaker statuses
     * @return List of response DTOs
     */
    public List<CircuitBreakerStatusResponse> toResponseList(List<CircuitBreakerStatus> models) {
        return models.stream()
                .map(this::toResponse)
                .toList();
    }
}
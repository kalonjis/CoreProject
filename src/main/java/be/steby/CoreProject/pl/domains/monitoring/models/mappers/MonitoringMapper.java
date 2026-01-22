package be.steby.CoreProject.pl.domains.monitoring.models.mappers;

import be.steby.CoreProject.bll.domains.monitoring.models.*;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.CircuitBreakerStatusResponse;
import be.steby.CoreProject.pl.domains.monitoring.models.responses.DashboardMetricsResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for Monitoring domain.
 *
 * Converts BLL models (records) to PL responses (records).
 * Controls explicitly what data is exposed via the API.
 */
@Component
public class MonitoringMapper {

    // =========================================================================
    // DASHBOARD METRICS
    // =========================================================================

    /**
     * Maps DashboardMetrics (BLL) to DashboardMetricsResponse (PL).
     */
    public DashboardMetricsResponse toResponse(DashboardMetrics model) {
        return new DashboardMetricsResponse(
                toJvmResponse(model.jvm()),
                toCpuResponse(model.cpu()),
                toDiskResponse(model.disk()),
                toDbPoolResponse(model.dbPool()),
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
    // CIRCUIT BREAKERS
    // =========================================================================

    /**
     * Maps CircuitBreakerStatus (BLL) to CircuitBreakerStatusResponse (PL).
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
     * Maps list of CircuitBreakerStatus to list of responses.
     */
    public List<CircuitBreakerStatusResponse> toResponseList(List<CircuitBreakerStatus> models) {
        return models.stream()
                .map(this::toResponse)
                .toList();
    }
}
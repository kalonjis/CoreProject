package be.steby.CoreProject.bll.domains.monitoring.services;

import be.steby.CoreProject.bll.domains.monitoring.models.*;
import be.steby.CoreProject.bll.domains.monitoring.services.MonitoringService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of MonitoringService.
 *
 * Bridge between Spring Boot Actuator and the monitoring domain.
 * Returns BLL models (records) - mapping to Response is done in PL.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringServiceImpl implements MonitoringService {

    private final HealthEndpoint healthEndpoint;
    private final MetricsEndpoint metricsEndpoint;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // =========================================================================
    // HEALTH
    // =========================================================================

    @Override
    public HealthComponent getHealth() {
        log.debug("Retrieving full application health");
        return healthEndpoint.health();
    }

    @Override
    public HealthComponent getComponentHealth(String component) {
        log.debug("Retrieving health for component: {}", component);
        return healthEndpoint.healthForPath(component);
    }

    // =========================================================================
    // METRICS
    // =========================================================================

    @Override
    public DashboardMetrics getDashboardMetrics() {
        log.debug("Building dashboard metrics");

        return new DashboardMetrics(
            buildJvmMetrics(),
            buildCpuMetrics(),
            buildDiskMetrics(),
            buildDbPoolMetrics(),
            getMetricValue("process.uptime", null)
        );
    }

    // =========================================================================
    // CIRCUIT BREAKERS
    // =========================================================================

    @Override
    public List<CircuitBreakerStatus> getAllCircuitBreakers() {
        log.debug("Retrieving all circuit breakers status");

        return circuitBreakerRegistry.getAllCircuitBreakers()
            .stream()
            .map(this::mapCircuitBreaker)
            .collect(Collectors.toList());
    }

    @Override
    public CircuitBreakerStatus getCircuitBreaker(String name) {
        log.debug("Retrieving circuit breaker: {}", name);

        CircuitBreaker cb = circuitBreakerRegistry.find(name)
            .orElseThrow(() -> new IllegalArgumentException(
                "Circuit breaker not found: " + name));

        return mapCircuitBreaker(cb);
    }

    // =========================================================================
    // PRIVATE HELPERS - Metrics Building
    // =========================================================================

    private JvmMetrics buildJvmMetrics() {
        double heapUsed = getMetricValue("jvm.memory.used", List.of("area:heap"));
        double heapMax = getMetricValue("jvm.memory.max", List.of("area:heap"));

        int heapPercent = heapMax > 0
            ? (int) Math.round((heapUsed / heapMax) * 100)
            : 0;

        return new JvmMetrics(
            (long) heapUsed,
            (long) heapMax,
            heapPercent
        );
    }

    private CpuMetrics buildCpuMetrics() {
        double systemCpu = getMetricValue("system.cpu.usage", null);
        double processCpu = getMetricValue("process.cpu.usage", null);

        return new CpuMetrics(
            (int) Math.round(systemCpu * 100),
            (int) Math.round(processCpu * 100)
        );
    }

    private DiskMetrics buildDiskMetrics() {
        double diskFree = getMetricValue("disk.free", null);
        double diskTotal = getMetricValue("disk.total", null);

        int freePercent = diskTotal > 0
            ? (int) Math.round((diskFree / diskTotal) * 100)
            : 0;

        return new DiskMetrics(
            (long) diskFree,
            (long) diskTotal,
            freePercent
        );
    }

    private DbPoolMetrics buildDbPoolMetrics() {
        return new DbPoolMetrics(
            (int) getMetricValue("hikaricp.connections.active", null),
            (int) getMetricValue("hikaricp.connections.idle", null),
            (int) getMetricValue("hikaricp.connections.max", null)
        );
    }

    private double getMetricValue(String metricName, List<String> tags) {
        try {
            MetricsEndpoint.MetricDescriptor metric = metricsEndpoint.metric(metricName, tags);
            if (metric != null && !metric.getMeasurements().isEmpty()) {
                return metric.getMeasurements().get(0).getValue();
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve metric '{}': {}", metricName, e.getMessage());
        }
        return 0.0;
    }

    // =========================================================================
    // PRIVATE HELPERS - Circuit Breaker Mapping
    // =========================================================================

    private CircuitBreakerStatus mapCircuitBreaker(CircuitBreaker cb) {
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        return new CircuitBreakerStatus(
            cb.getName(),
            cb.getState().name(),
            metrics.getFailureRate(),
            metrics.getSlowCallRate(),
            metrics.getNumberOfBufferedCalls(),
            metrics.getNumberOfFailedCalls(),
            metrics.getNumberOfSuccessfulCalls(),
            metrics.getNumberOfNotPermittedCalls()
        );
    }
}
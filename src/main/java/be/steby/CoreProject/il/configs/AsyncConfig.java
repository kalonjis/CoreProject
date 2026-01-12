package be.steby.CoreProject.il.configs;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous execution with dedicated thread pools.
 *
 * <p>This configuration defines multiple thread pools optimized for different
 * types of async operations. Each executor is configured with appropriate
 * pool sizes based on the nature of its tasks.</p>
 *
 * <h4>MDC Propagation:</h4>
 * <p>All executors use {@link MdcTaskDecorator} to propagate the MDC context
 * (including Correlation ID) from the calling thread to async worker threads.
 * This ensures consistent logging and tracing across async boundaries.</p>
 *
 * <h4>Available Executors:</h4>
 * <ul>
 *   <li>{@code generalPurposeExecutor} - Default executor for miscellaneous tasks</li>
 *   <li>{@code emailExecutor} - Dedicated to email sending operations</li>
 *   <li>{@code smsExecutor} - Dedicated to SMS sending operations</li>
 *   <li>{@code activityLogExecutor} - Dedicated to activity log persistence</li>
 *   <li>{@code securityMonitoringExecutor} - Dedicated to security monitoring tasks</li>
 *   <li>{@code eventListenerExecutor} - Dedicated to event listener processing</li>
 *   <li>{@code geocodingExecutor} - Dedicated to external geocoding API calls</li>
 * </ul>
 *
 * @see MdcTaskDecorator
 * @see CustomAsyncExceptionHandler
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Shared MDC task decorator instance.
     * Stateless and thread-safe, can be reused across all executors.
     */
    private final MdcTaskDecorator mdcTaskDecorator = new MdcTaskDecorator();

    /**
     * Default executor used when no specific executor is specified in @Async.
     *
     * @return the general purpose executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return generalPurposeExecutor();
    }

    /**
     * Thread pool for general purpose async tasks.
     *
     * <p>Used as the default executor when @Async is used without
     * specifying a particular executor name.</p>
     *
     * @return configured ThreadPoolTaskExecutor for general tasks
     */
    @Bean(name = "generalPurposeExecutor")
    public Executor generalPurposeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("General-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(mdcTaskDecorator); // MDC propagation
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool dedicated to email sending operations.
     *
     * <p>Configuration rationale:</p>
     * <ul>
     *   <li>Core pool: 2 threads (emails are I/O bound, not CPU intensive)</li>
     *   <li>Max pool: 5 threads (handles email bursts)</li>
     *   <li>Queue: 100 (buffer for email spikes)</li>
     * </ul>
     *
     * @return configured ThreadPoolTaskExecutor for email tasks
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(mdcTaskDecorator); // MDC propagation
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool dedicated to SMS sending operations.
     *
     * <p>Configuration rationale:</p>
     * <ul>
     *   <li>Core pool: 2 threads (SMS APIs are rate-limited)</li>
     *   <li>Max pool: 4 threads (limited by Twilio rate limits)</li>
     *   <li>Queue: 50 (smaller queue as SMS should be sent quickly)</li>
     * </ul>
     *
     * @return configured ThreadPoolTaskExecutor for SMS tasks
     */
    @Bean(name = "smsExecutor")
    public Executor smsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("SMS-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(mdcTaskDecorator); // MDC propagation
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool dedicated to activity log persistence.
     *
     * <p>Configuration rationale:</p>
     * <ul>
     *   <li>Core pool: 3 threads (DB writes can be batched)</li>
     *   <li>Max pool: 6 threads (handles logging spikes)</li>
     *   <li>Queue: 500 (large buffer - logs can wait)</li>
     *   <li>Termination: 60s (ensure all logs are persisted on shutdown)</li>
     * </ul>
     *
     * @return configured ThreadPoolTaskExecutor for activity logging
     */
    @Bean(name = "activityLogExecutor")
    public Executor activityLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ActivityLog-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setTaskDecorator(mdcTaskDecorator); // MDC propagation
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool dedicated to security monitoring tasks.
     *
     * <p>Configuration rationale:</p>
     * <ul>
     *   <li>Core pool: 1 thread (low volume, high priority)</li>
     *   <li>Max pool: 3 threads (handles security event bursts)</li>
     *   <li>Queue: 50 (security events should not queue up)</li>
     * </ul>
     *
     * @return configured ThreadPoolTaskExecutor for security monitoring
     */
    @Bean("securityMonitoringExecutor")
    public Executor securityMonitoringExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("SecurityMonitoring-");
        executor.setTaskDecorator(mdcTaskDecorator); // MDC propagation
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for event listener processing.
     *
     * <p>Configuration rationale:</p>
     * <ul>
     *   <li>Core pool: 2 threads (balanced for event processing)</li>
     *   <li>Max pool: 4 threads (handles event bursts)</li>
     *   <li>Queue: 100 (buffer for event spikes)</li>
     * </ul>
     *
     * @return configured ThreadPoolTaskExecutor for event listeners
     */
    @Bean(name = "eventListenerExecutor")
    public Executor eventListenerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("EventListener-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setTaskDecorator(mdcTaskDecorator); // MDC propagation
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for geocoding operations.
     *
     * <p>Configuration rationale:</p>
     * <ul>
     *   <li>Core pool: 2 threads (Nominatim API is rate-limited to 1 req/sec)</li>
     *   <li>Max pool: 5 threads (handles burst if multiple addresses created)</li>
     *   <li>Queue: 100 (addresses waiting for geocoding)</li>
     *   <li>Termination: 60s (allow pending geocoding to complete)</li>
     * </ul>
     *
     * @return configured ThreadPoolTaskExecutor for geocoding tasks
     */
    @Bean(name = "geocodingExecutor")
    public Executor geocodingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Geocoding-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setTaskDecorator(mdcTaskDecorator); // MDC propagation
        executor.initialize();
        return executor;
    }

    /**
     * Exposes all executors as a Map for monitoring purposes.
     *
     * <p>This bean allows monitoring services to iterate over all executors
     * and collect metrics like queue size, active threads, etc.</p>
     *
     * @return Map of executor names to their ThreadPoolTaskExecutor instances
     */
    @Bean
    public Map<String, ThreadPoolTaskExecutor> executors(
            @Qualifier("emailExecutor") ThreadPoolTaskExecutor emailExecutor,
            @Qualifier("smsExecutor") ThreadPoolTaskExecutor smsExecutor,
            @Qualifier("activityLogExecutor") ThreadPoolTaskExecutor activityLogExecutor,
            @Qualifier("eventListenerExecutor") ThreadPoolTaskExecutor eventListenerExecutor,
            @Qualifier("geocodingExecutor") ThreadPoolTaskExecutor geocodingExecutor,
            @Qualifier("generalPurposeExecutor") ThreadPoolTaskExecutor generalPurposeExecutor,
            @Qualifier("securityMonitoringExecutor") ThreadPoolTaskExecutor securityMonitoringExecutor) {

        Map<String, ThreadPoolTaskExecutor> executors = new HashMap<>();
        executors.put("email", emailExecutor);
        executors.put("sms", smsExecutor);
        executors.put("activityLog", activityLogExecutor);
        executors.put("eventListener", eventListenerExecutor);
        executors.put("geocoding", geocodingExecutor);
        executors.put("general", generalPurposeExecutor);
        executors.put("securityMonitoring", securityMonitoringExecutor);
        return executors;
    }

    /**
     * Custom exception handler for uncaught exceptions in async tasks.
     *
     * @return the custom async exception handler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }
}
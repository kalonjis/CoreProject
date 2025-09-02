package be.steby.CoreProject.il.configs;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous execution with dedicated thread pools.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Creates and configures a ThreadPoolTaskExecutor with common settings.
     *
     * @param corePoolSize       the core number of threads.
     * @param maxPoolSize        the maximum number of threads.
     * @param queueCapacity      the queue capacity.
     * @param threadNamePrefix   the thread name prefix.
     * @param awaitTerminationSeconds the seconds to wait for shutdown.
     * @return a configured ThreadPoolTaskExecutor.
     */
    private ThreadPoolTaskExecutor createExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix, int awaitTerminationSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }

    /**
     * Default executor, used when no specific executor is named.
     */
    @Override
    public Executor getAsyncExecutor() {
        return generalPurposeExecutor();
    }

    /**
     * Thread pool for general-purpose tasks.
     */
    @Bean(name = "generalPurposeExecutor")
    public Executor generalPurposeExecutor() {
        return createExecutor(4, 8, 200, "General-", 30);
    }

    /**
     * Dedicated thread pool for email sending tasks.
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        return createExecutor(2, 5, 100, "Email-", 30);
    }

    /**
     * Dedicated thread pool for activity logging tasks.
     */
    @Bean(name = "activityLogExecutor")
    public Executor activityLogExecutor() {
        return createExecutor(3, 6, 500, "ActivityLog-", 60);
    }

    /**
     * Dedicated thread pool for event listener tasks.
     */
    @Bean(name = "eventListenerExecutor")
    public Executor eventListenerExecutor() {
        return createExecutor(2, 4, 100, "EventListener-", 30);
    }

    /**
     * Bean that exposes all configured executors for monitoring.
     * We use TaskExecutor here because it's the specific Spring interface that
     * gives us more control over task execution than the generic Executor interface.
     */
    @Bean
    public Map<String, TaskExecutor> executors(
            @Qualifier("emailExecutor") TaskExecutor emailExecutor,
            @Qualifier("activityLogExecutor") TaskExecutor activityLogExecutor,
            @Qualifier("eventListenerExecutor") TaskExecutor eventListenerExecutor,
            @Qualifier("generalPurposeExecutor") TaskExecutor generalPurposeExecutor) {

        Map<String, TaskExecutor> executors = new HashMap<>();
        executors.put("email", emailExecutor);
        executors.put("activityLog", activityLogExecutor);
        executors.put("eventListener", eventListenerExecutor);
        executors.put("general", generalPurposeExecutor);
        return executors;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // You can use this to define a custom handler for exceptions in async tasks.
        // For example, logging the exception and the task that failed.
        return null; // Or return a custom handler like new CustomAsyncExceptionHandler();
    }
}
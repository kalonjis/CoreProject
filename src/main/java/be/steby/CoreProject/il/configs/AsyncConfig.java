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
 * Configuration pour l'exécution asynchrone avec pools de threads séparés
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Executor par défaut - utilisé si aucun autre n'est spécifié
     */
    @Override
    public Executor getAsyncExecutor() {
        return generalPurposeExecutor();
    }

    /**
     * Pool de threads pour les tâches générales
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
        executor.initialize();
        return executor;
    }

    /**
     * Pool de threads dédié aux envois d'emails
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
        executor.initialize();
        return executor;
    }


    /**
     * Pool de threads dédié aux envois de SMS
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
        executor.initialize();
        return executor;
    }


    /**
     * Pool de threads dédié aux logs d'activité (écriture en DB)
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
        executor.initialize();
        return executor;
    }

    @Bean("securityMonitoringExecutor")
    public Executor securityMonitoringExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("SecurityMonitoring-");
        executor.initialize();
        return executor;
    }

    /**
     * Pool de threads pour les listeners d'événements
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
        executor.initialize();
        return executor;
    }


    /**
     * Executor for geocoding operations.
     *
     * Configuration:
     * - Core pool size: 2 threads (rate-limited API, no need for many threads)
     * - Max pool size: 5 threads (handles burst if multiple addresses created)
     * - Queue capacity: 100 (addresses waiting for geocoding)
     *
     * This executor is dedicated to external geocoding API calls (Nominatim)
     * to avoid blocking other async operations.
     *
     * @return configured ThreadPoolTaskExecutor for geocoding tasks
     */
    @Bean(name = "geocodingExecutor")
    public Executor geocodingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Small pool because Nominatim is rate-limited to 1 req/sec
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);

        // Thread naming for easier debugging in logs
        executor.setThreadNamePrefix("geocoding-");

        // Graceful shutdown: wait for tasks to complete
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * Bean qui expose tous les executors pour le monitoring
     */
    @Bean
    public Map<String, ThreadPoolTaskExecutor> executors(
            @Qualifier("emailExecutor") ThreadPoolTaskExecutor emailExecutor,
            @Qualifier("smsExecutor") ThreadPoolTaskExecutor smsExecutor,
            @Qualifier("activityLogExecutor") ThreadPoolTaskExecutor activityLogExecutor,
            @Qualifier("eventListenerExecutor") ThreadPoolTaskExecutor eventListenerExecutor,
            @Qualifier("geocodingExecutor") ThreadPoolTaskExecutor geocodingExecutor,
            @Qualifier("generalPurposeExecutor") ThreadPoolTaskExecutor generalPurposeExecutor) {

        Map<String, ThreadPoolTaskExecutor> executors = new HashMap<>();
        executors.put("email", emailExecutor);
        executors.put("sms", smsExecutor);
        executors.put("activityLog", activityLogExecutor);
        executors.put("eventListener", eventListenerExecutor);
        executors.put("geocodingExecutor", geocodingExecutor);
        executors.put("general", generalPurposeExecutor);
        return executors;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }

}
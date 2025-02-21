package be.steby.CoreProject.il.configs;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for enabling asynchronous execution support in Spring
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Configuration minimale et adaptée
        executor.setCorePoolSize(2);     // 2 threads de base
        executor.setMaxPoolSize(5);      // Maximum 5 threads
        executor.setQueueCapacity(100);  // Capacité de file d'attente réduite

        executor.setThreadNamePrefix("AsyncMailTask-");

        // Configuration par défaut
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    // Optionnel : Gestion des exceptions
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            System.err.println("Async method " + method.getName()
                    + " threw exception: " + throwable.getMessage());
        };
    }
}

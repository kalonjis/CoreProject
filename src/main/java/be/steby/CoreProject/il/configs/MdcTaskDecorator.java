package be.steby.CoreProject.il.configs;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Task decorator that propagates MDC (Mapped Diagnostic Context) to async threads.
 *
 * <p>When using Spring's {@code @Async} annotation, tasks are executed in a different
 * thread pool. By default, the MDC context (including the Correlation ID) is not
 * propagated to these worker threads. This decorator solves that problem by copying
 * the MDC context from the calling thread to the async execution thread.</p>
 *
 * <h4>The Problem:</h4>
 * <pre>{@code
 * // HTTP Thread (has correlationId in MDC)
 * @PostMapping("/login")
 * public Response login() {
 *     log.info("Login started");  // ✅ Has correlationId
 *     eventPublisher.publishEvent(new LoginEvent(user));
 *     return response;
 * }
 *
 * // Async Thread (loses MDC by default!)
 * @Async("emailExecutor")
 * @EventListener
 * public void handleLogin(LoginEvent event) {
 *     log.info("Sending email");  // ❌ No correlationId without this decorator
 * }
 * }</pre>
 *
 * <h4>The Solution:</h4>
 * <p>This decorator captures the MDC map before the task is submitted, then restores
 * it in the worker thread before execution, and cleans up afterward.</p>
 *
 * <h4>Usage:</h4>
 * <p>Apply this decorator to your thread pool executors in AsyncConfig:</p>
 * <pre>{@code
 * @Bean("emailExecutor")
 * public Executor emailExecutor() {
 *     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *     executor.setTaskDecorator(new MdcTaskDecorator());
 *     // ... other configuration
 *     return executor;
 * }
 * }</pre>
 *
 * <h4>Thread Safety:</h4>
 * <p>This decorator is stateless and thread-safe. Each task decoration captures
 * its own snapshot of the MDC context at decoration time.</p>
 *
 * <h4>Memory Safety:</h4>
 * <p>The decorator ensures MDC is always cleared in a finally block, preventing
 * memory leaks when threads are reused by the pool.</p>
 *
 * @see org.slf4j.MDC
 * @see org.springframework.core.task.TaskDecorator
 * @see be.steby.CoreProject.il.filters.CorrelationIdFilter
 */
public class MdcTaskDecorator implements TaskDecorator {

    /**
     * Decorates a runnable task to propagate MDC context.
     *
     * <p>The decoration process:</p>
     * <ol>
     *   <li><strong>Capture:</strong> Copy the current MDC map (runs in calling thread)</li>
     *   <li><strong>Restore:</strong> Set the captured MDC in worker thread before task runs</li>
     *   <li><strong>Execute:</strong> Run the original task with MDC available</li>
     *   <li><strong>Cleanup:</strong> Clear MDC to prevent leaks when thread is reused</li>
     * </ol>
     *
     * @param runnable the original task to decorate
     * @return a decorated task that preserves MDC context
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        // Step 1: Capture MDC context from the CALLING thread (e.g., HTTP request thread)
        // This happens immediately when the async method is called, NOT when the task executes
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        // Step 2: Return a wrapper that will execute in the WORKER thread
        return () -> {
            try {
                // Step 3: Restore MDC context in the worker thread BEFORE task execution
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }

                // Step 4: Execute the original task (now with MDC available)
                runnable.run();

            } finally {
                // Step 5: CRITICAL - Clear MDC to prevent memory leaks
                // Worker threads are reused, so leftover MDC data could contaminate future tasks
                MDC.clear();
            }
        };
    }
}
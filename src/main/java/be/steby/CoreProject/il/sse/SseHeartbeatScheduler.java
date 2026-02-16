package be.steby.CoreProject.il.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;


/**
 * Scheduler for sending SSE heartbeats to keep connections alive.
 *
 * <p>Periodically sends heartbeat events to all connected clients to:</p>
 * <ul>
 *   <li>Keep connections alive through proxies and load balancers</li>
 *   <li>Detect and clean up dead connections</li>
 *   <li>Provide connection health monitoring</li>
 * </ul>
 *
 * <h4>Why Heartbeats?</h4>
 * <p>Many network intermediaries (proxies, load balancers, firewalls) close
 * idle connections after a timeout (often 60 seconds). Regular heartbeats
 * prevent this by keeping the connection active.</p>
 *
 * <h4>Configuration:</h4>
 * <pre>{@code
 * app:
 *   sse:
 *     heartbeat-enabled: true
 *     heartbeat-interval: 30000  # 30 seconds
 * }</pre>
 *
 * <h4>Disabling:</h4>
 * <p>Set {@code app.sse.heartbeat-enabled=false} to disable heartbeats.
 * This component won't be created if disabled.</p>
 *
 * @see SseEmitterManager
 * @see SseConfig
 */
@Component
@ConditionalOnProperty(name = "app.sse.heartbeat-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SseHeartbeatScheduler {

    private final SseEmitterManager emitterManager;

    /**
     * Sends heartbeat to all connected clients.
     *
     * <p>Runs every 30 seconds by default. Interval can be configured
     * via {@code app.sse.heartbeat-interval}.</p>
     *
     * <p>Failed heartbeats automatically trigger connection cleanup
     * via the emitter's error callback.</p>
     */
    @Scheduled(fixedRateString = "${app.sse.heartbeat-interval:30000}")
    public void sendHeartbeats() {
        int connectionCount = emitterManager.getConnectionCount();

        if (connectionCount == 0) {
            log.debug("[Heartbeat] No SSE connections, skipping");
            return; // No connections, nothing to do
        }

        log.info("Sending heartbeats to {} SSE connections", connectionCount);

        final int[] success = {0};
        final int[] failed = {0};

        emitterManager.forEachEmitter((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
                success[0]++;
                log.trace("[Heartbeat] Sent to user {}", userId);
            } catch (IOException e) {
                failed[0]++;
                log.warn("[Heartbeat] Failed for user {}: {} - connection will be removed",
                        userId, e.getMessage());
                // Connection cleanup happens automatically via emitter callbacks
            }
        });

        if (failed[0] > 0) {
            log.warn("[Heartbeat] Result: {}/{} success, {} failed",
                    success[0], connectionCount, failed[0]);
        }
    }
}
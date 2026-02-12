package be.steby.CoreProject.il.sse;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Server-Sent Events (SSE).
 *
 * <p>These properties can be customized in {@code application.yml}:</p>
 * <pre>{@code
 * app:
 *   sse:
 *     timeout: 1800000       # 30 minutes
 *     heartbeat-interval: 30000  # 30 seconds
 * }</pre>
 *
 * <h4>Timeout Considerations:</h4>
 * <ul>
 *   <li><b>Too short:</b> Frequent reconnections, poor UX</li>
 *   <li><b>Too long:</b> Dead connections not detected</li>
 *   <li><b>Recommended:</b> 30 minutes with heartbeats every 30 seconds</li>
 * </ul>
 *
 * <h4>Proxy/Load Balancer:</h4>
 * <p>Many proxies (Nginx, AWS ALB) have default timeouts of 60 seconds.
 * The heartbeat interval should be shorter than these timeouts to keep
 * the connection alive.</p>
 *
 * @see SseEmitterManager
 * @see SseNotificationPusher
 */
@Configuration
@ConfigurationProperties(prefix = "app.sse")
public class SseConfig {

    /**
     * SSE connection timeout in milliseconds.
     *
     * <p>After this duration, the connection is automatically closed.
     * The client should reconnect if still interested.</p>
     *
     * <p>Default: 30 minutes (1800000 ms)</p>
     */
    private long timeout = 30 * 60 * 1000L; // 30 minutes

    /**
     * Heartbeat interval in milliseconds.
     *
     * <p>Frequency at which to send heartbeat events to keep
     * connections alive and detect dead connections.</p>
     *
     * <p>Default: 30 seconds (30000 ms)</p>
     */
    private long heartbeatInterval = 30 * 1000L; // 30 seconds

    /**
     * Whether to enable heartbeat sending.
     *
     * <p>Default: true</p>
     */
    private boolean heartbeatEnabled = true;

    /**
     * Maximum number of concurrent SSE connections.
     *
     * <p>Prevents resource exhaustion from too many connections.
     * Set to 0 for unlimited.</p>
     *
     * <p>Default: 10000</p>
     */
    private int maxConnections = 10000;

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public boolean isHeartbeatEnabled() {
        return heartbeatEnabled;
    }

    public void setHeartbeatEnabled(boolean heartbeatEnabled) {
        this.heartbeatEnabled = heartbeatEnabled;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
}
package be.steby.CoreProject.bll.domains.auth.listeners;

import be.steby.CoreProject.bll.domains.auth.events.IpBlockedLoginAttemptEvent;
import be.steby.CoreProject.bll.domains.auth.services.activity_log.SecurityMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for security monitoring events
 * Handles blocked IPs, suspicious patterns, etc.
 */
@Component
@Order(200) // Lower priority than normal activity listeners
@RequiredArgsConstructor
@Slf4j
public class SecurityMonitoringListener {

    private final SecurityMonitoringService securityMonitoringService;

    /**
     * Handle blocked IP login attempts
     * Important for security monitoring and potential alerting
     */
    @EventListener
    @Async("securityMonitoringExecutor") // Separate executor for security
    public void handleBlockedIpAttempt(IpBlockedLoginAttemptEvent event) {
        try {
            log.debug("Processing blocked IP event - IP: {}, Attempts: {}",
                    event.blockedIpAddress(), event.attemptCount());

            // Log the security event
            securityMonitoringService.logBlockedIpAttempt(event);

            // Analyze suspicious patterns (optional)
            if (event.attemptCount() >= 15) { // Threshold for critical alerts
                securityMonitoringService.triggerHighRiskAlert(event);
            }

            log.debug("Blocked IP attempt logged - IP: {}", event.blockedIpAddress());
        } catch (Exception e) {
            log.error("Failed to log blocked IP attempt for IP: {}",
                    event.blockedIpAddress(), e);
        }
    }
}
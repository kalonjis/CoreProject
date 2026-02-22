//package be.steby.CoreProject.bll.domains.auth.services.activity_log;
//
//import be.steby.CoreProject.bll.common.services.activitylog.ActivityLogService;
//import be.steby.CoreProject.bll.domains.auth.events.IpBlockedLoginAttemptEvent;
//import be.steby.CoreProject.dal.repositories.ActivityLogRepository;
//import be.steby.CoreProject.dl.enums.action_log_type.SecurityAction;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
///**
// * Service dedicated to security monitoring and logging
// * Separated from AuthActivityLogService for better separation of concerns
// */
//@Service
//@Slf4j
//public class SecurityMonitoringService extends ActivityLogService {
//
//    public SecurityMonitoringService(ActivityLogRepository activityLogRepository) {
//        super(activityLogRepository);
//    }
//
//    @Override
//    protected String getDomainName() {
//        return "SECURITY";
//    }
//
//    /**
//     * Log blocked IP attempts for security monitoring
//     */
//    public void logBlockedIpAttempt(IpBlockedLoginAttemptEvent event) {
//        // Create security log with blocked IP details
//        String details = String.format(
//                "IP %s blocked after %d attempts. Attempted username: %s. Reason: %s",
//                event.blockedIpAddress(),
//                event.attemptCount(),
//                event.attemptedUsername() != null ? event.attemptedUsername() : "N/A",
//                event.blockReason()
//        );
//
//        // ✅ Call logSecurityEvent directly instead of logUserActivity
//        logSecurityEvent(null, SecurityAction.IP_BLOCKED, true, details);
//
//        log.info("Security event logged - {}", details);
//    }
//
//    /**
//     * Trigger high-risk security alerts for repeated blocked attempts
//     */
//    public void triggerHighRiskAlert(IpBlockedLoginAttemptEvent event) {
//        log.warn("HIGH RISK SECURITY ALERT - IP {} has been blocked with {} attempts. " +
//                        "This may indicate a persistent attack. Consider IP-level blocking at firewall level.",
//                event.blockedIpAddress(), event.attemptCount());
//
//        // Here you could:
//        // - Send email notifications to admins
//        // - Send Slack alerts
//        // - Trigger automatic measures (firewall blocking, etc.)
//        // - Create security tickets
//
//        String alertDetails = String.format(
//                "HIGH RISK: IP %s reached %d failed attempts - potential persistent attack",
//                event.blockedIpAddress(), event.attemptCount()
//        );
//
//        // ✅ Call logSecurityEvent directly instead of logUserActivity
//        logSecurityEvent(null, SecurityAction.HIGH_RISK_ALERT, true, alertDetails);
//    }
//}
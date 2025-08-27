package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

/**
 * Security domain actions with specialized business logic.
 * Contains all security-related actions and their specific behaviors.
 */
public enum SecurityAction implements ActionLogType {

    // ================== THREAT DETECTION ==================
    SUSPICIOUS_ACTIVITY_DETECTED("Suspicious activity detected"),
    BRUTE_FORCE_ATTEMPT_DETECTED("Brute force attempt detected"),
    UNUSUAL_LOGIN_LOCATION("Unusual login location detected"),
    MULTIPLE_FAILED_ATTEMPTS("Multiple failed login attempts"),
    ACCOUNT_ENUMERATION_ATTEMPT("Account enumeration attempt detected"),
    SQL_INJECTION_ATTEMPT("SQL injection attempt detected"),
    XSS_ATTEMPT_DETECTED("Cross-site scripting attempt detected"),
    CSRF_ATTEMPT_DETECTED("Cross-site request forgery attempt detected"),

    // ================== ACCOUNT SECURITY ==================
    ACCOUNT_TEMPORARILY_LOCKED("Account temporarily locked for security"),
    ACCOUNT_PERMANENTLY_LOCKED("Account permanently locked for security"),
    ACCOUNT_SECURITY_REVIEW("Account flagged for security review"),
    SECURITY_QUESTION_FAILED("Security question answer failed"),
    BACKUP_CODE_INVALID("Invalid backup code used"),
    RECOVERY_ATTEMPT_FAILED("Account recovery attempt failed"),
    UNAUTHORIZED_ACCESS_ATTEMPT("Unauthorized access attempt"),

    // ================== SESSION SECURITY ==================
    CONCURRENT_SESSION_DETECTED("Concurrent session from different location"),
    SESSION_HIJACK_ATTEMPT("Potential session hijacking detected"),
    SESSION_FIXATION_ATTEMPT("Session fixation attempt detected"),
    INVALID_SESSION_TOKEN("Invalid session token used"),
    EXPIRED_SESSION_ACCESS("Access attempted with expired session"),
    SESSION_ANOMALY_DETECTED("Session behavior anomaly detected"),

    // ================== API SECURITY ==================
    API_RATE_LIMIT_EXCEEDED("API rate limit exceeded"),
    INVALID_API_KEY_USED("Invalid API key used"),
    API_KEY_COMPROMISED("API key potentially compromised"),
    UNAUTHORIZED_API_ACCESS("Unauthorized API access attempt"),
    API_ENDPOINT_ABUSE("API endpoint abuse detected"),
    MALFORMED_API_REQUEST("Malformed API request detected"),

    // ================== DATA SECURITY ==================
    UNAUTHORIZED_DATA_ACCESS("Unauthorized data access attempt"),
    DATA_BREACH_DETECTED("Potential data breach detected"),
    SENSITIVE_DATA_EXPOSURE("Sensitive data exposure detected"),
    DATA_INTEGRITY_VIOLATION("Data integrity violation detected"),
    UNAUTHORIZED_DATA_EXPORT("Unauthorized data export attempt"),
    DATA_TAMPERING_ATTEMPT("Data tampering attempt detected"),

    // ================== NETWORK SECURITY ==================
    SUSPICIOUS_IP_ADDRESS("Access from suspicious IP address"),
    BLACKLISTED_IP_BLOCKED("Access blocked from blacklisted IP"),
    GEO_BLOCKING_TRIGGERED("Geographic blocking triggered"),
    VPN_TOR_USAGE_DETECTED("VPN/Tor usage detected"),
    PROXY_USAGE_DETECTED("Proxy usage detected"),
    DDoS_ATTEMPT_DETECTED("DDoS attempt detected"),
    PORT_SCAN_DETECTED("Port scanning detected"),

    // ================== MALWARE & EXPLOITS ==================
    MALWARE_UPLOAD_ATTEMPT("Malware upload attempt detected"),
    VIRUS_SCAN_FAILED("File failed virus scan"),
    EXPLOIT_ATTEMPT_DETECTED("Exploitation attempt detected"),
    BUFFER_OVERFLOW_ATTEMPT("Buffer overflow attempt detected"),
    CODE_INJECTION_ATTEMPT("Code injection attempt detected"),
    FILE_INCLUSION_ATTEMPT("File inclusion attempt detected"),

    // ================== AUTHENTICATION SECURITY ==================
    WEAK_PASSWORD_POLICY_VIOLATION("Password policy violation"),
    CREDENTIAL_STUFFING_DETECTED("Credential stuffing attack detected"),
    PASSWORD_SPRAY_DETECTED("Password spraying attack detected"),
    DICTIONARY_ATTACK_DETECTED("Dictionary attack detected"),
    RAINBOW_TABLE_ATTACK("Rainbow table attack detected"),

    // ================== PRIVILEGE ESCALATION ==================
    PRIVILEGE_ESCALATION_ATTEMPT("Privilege escalation attempt"),
    UNAUTHORIZED_ADMIN_ACCESS("Unauthorized admin access attempt"),
    ROLE_MANIPULATION_ATTEMPT("Role manipulation attempt detected"),
    PERMISSION_BYPASS_ATTEMPT("Permission bypass attempt"),

    // ================== SYSTEM SECURITY ==================
    SYSTEM_FILE_MODIFICATION("Unauthorized system file modification"),
    CONFIGURATION_TAMPERING("System configuration tampering detected"),
    LOG_TAMPERING_ATTEMPT("Log tampering attempt detected"),
    BACKUP_SYSTEM_COMPROMISE("Backup system compromise detected"),
    SECURITY_TOOL_DISABLED("Security tool disabled or bypassed"),

    // ================== COMPLIANCE & AUDIT ==================
    COMPLIANCE_VIOLATION("Security compliance violation"),
    AUDIT_LOG_ANOMALY("Audit log anomaly detected"),
    UNAUTHORIZED_ADMIN_ACTION("Unauthorized administrative action"),
    SECURITY_POLICY_VIOLATION("Security policy violation"),
    DATA_RETENTION_VIOLATION("Data retention policy violation"),

    // ================== INCIDENT RESPONSE ==================
    SECURITY_INCIDENT_CREATED("Security incident created"),
    SECURITY_INCIDENT_ESCALATED("Security incident escalated"),
    SECURITY_INCIDENT_RESOLVED("Security incident resolved"),
    FORENSIC_ANALYSIS_INITIATED("Forensic analysis initiated"),
    THREAT_INTELLIGENCE_ALERT("Threat intelligence alert triggered"),

    // ================== SECURITY MEASURES ==================
    IP_TEMPORARILY_BLOCKED("IP address temporarily blocked"),
    IP_PERMANENTLY_BLOCKED("IP address permanently blocked"),
    USER_AGENT_BLOCKED("User agent blocked"),
    CAPTCHA_CHALLENGE_ISSUED("CAPTCHA challenge issued"),
    SECURITY_SCAN_INITIATED("Security scan initiated"),
    VULNERABILITY_ASSESSMENT("Vulnerability assessment performed");

    private final String description;

    SecurityAction(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getCategory() {
        return "SECURITY";
    }

    @Override
    public int getDefaultRiskLevel() {
        return switch (this) {
            // Critical security threats
            case DATA_BREACH_DETECTED, SYSTEM_FILE_MODIFICATION, ACCOUNT_PERMANENTLY_LOCKED,
                 MALWARE_UPLOAD_ATTEMPT, PRIVILEGE_ESCALATION_ATTEMPT, LOG_TAMPERING_ATTEMPT -> 5;

            // High security threats
            case BRUTE_FORCE_ATTEMPT_DETECTED, SQL_INJECTION_ATTEMPT, XSS_ATTEMPT_DETECTED,
                 UNAUTHORIZED_ADMIN_ACCESS, DATA_TAMPERING_ATTEMPT, API_KEY_COMPROMISED -> 4;

            // Medium security threats
            case SUSPICIOUS_ACTIVITY_DETECTED, UNUSUAL_LOGIN_LOCATION, SESSION_HIJACK_ATTEMPT,
                 UNAUTHORIZED_DATA_ACCESS, CREDENTIAL_STUFFING_DETECTED -> 3;

            // Low-medium security events
            case API_RATE_LIMIT_EXCEEDED, SUSPICIOUS_IP_ADDRESS, MULTIPLE_FAILED_ATTEMPTS,
                 VPN_TOR_USAGE_DETECTED -> 2;

            // Low security events (monitoring, routine checks)
            default -> 1;
        };
    }

    // ================== DOMAIN-SPECIFIC PROCESSING ==================

    /**
     * Security-specific action processing with detailed logic
     */
    public String processSecurityAction(ActivityLog log) {
        String userName = log.getUser() != null ? log.getUser().getUsername() : "system";
        String ipAddress = log.getIpAddress() != null ? log.getIpAddress() : "unknown";
        String details = log.getActionDetails() != null ? log.getActionDetails() : "";

        return switch (this) {
            // Threat Detection
            case SUSPICIOUS_ACTIVITY_DETECTED -> "⚠️ Suspicious activity detected for user: " + userName +
                    " from IP: " + ipAddress + " - " + details;

            case BRUTE_FORCE_ATTEMPT_DETECTED -> "🚨 Brute force attack detected against user: " + userName +
                    " from IP: " + ipAddress;

            case UNUSUAL_LOGIN_LOCATION -> "🌍 Unusual login location detected for user: " + userName +
                    " from IP: " + ipAddress + " - " + details;

            case SQL_INJECTION_ATTEMPT -> "💉 SQL injection attempt detected from IP: " + ipAddress +
                    " - " + details;

            case XSS_ATTEMPT_DETECTED -> "🕷️ XSS attempt detected from IP: " + ipAddress +
                    " - " + details;

            // Account Security
            case ACCOUNT_TEMPORARILY_LOCKED -> log.isSuccessful() ?
                    "🔒 Account temporarily locked for security - user: " + userName + " - " + details :
                    "❌ Failed to lock account - user: " + userName;

            case ACCOUNT_PERMANENTLY_LOCKED -> log.isSuccessful() ?
                    "🔐 Account permanently locked for security - user: " + userName + " - " + details :
                    "❌ Failed to permanently lock account - user: " + userName;

            case UNAUTHORIZED_ACCESS_ATTEMPT -> "🚫 Unauthorized access attempt by user: " + userName +
                    " from IP: " + ipAddress + " - " + details;

            // Session Security
            case CONCURRENT_SESSION_DETECTED -> "👥 Concurrent session detected for user: " + userName +
                    " from IP: " + ipAddress;

            case SESSION_HIJACK_ATTEMPT -> "🎭 Potential session hijacking for user: " + userName +
                    " from IP: " + ipAddress + " - " + details;

            case INVALID_SESSION_TOKEN -> "🔑 Invalid session token used by user: " + userName +
                    " from IP: " + ipAddress;

            // API Security
            case API_RATE_LIMIT_EXCEEDED -> "⏰ API rate limit exceeded from IP: " + ipAddress +
                    " - " + details;

            case INVALID_API_KEY_USED -> "🗝️ Invalid API key used from IP: " + ipAddress +
                    " - " + details;

            case API_KEY_COMPROMISED -> "🚨 API key potentially compromised - " + details;

            // Data Security
            case DATA_BREACH_DETECTED -> "💥 Potential data breach detected - " + details;

            case UNAUTHORIZED_DATA_ACCESS -> "📊 Unauthorized data access attempt by user: " + userName +
                    " from IP: " + ipAddress + " - " + details;

            case SENSITIVE_DATA_EXPOSURE -> "🔍 Sensitive data exposure detected - " + details;

            // Network Security
            case BLACKLISTED_IP_BLOCKED -> "🚫 Blacklisted IP blocked: " + ipAddress + " - " + details;

            case VPN_TOR_USAGE_DETECTED -> "🕵️ VPN/Tor usage detected from IP: " + ipAddress +
                    " by user: " + userName;

            case DDoS_ATTEMPT_DETECTED -> "💥 DDoS attempt detected from IP: " + ipAddress +
                    " - " + details;

            // Malware & Exploits
            case MALWARE_UPLOAD_ATTEMPT -> "🦠 Malware upload attempt by user: " + userName +
                    " from IP: " + ipAddress + " - " + details;

            case EXPLOIT_ATTEMPT_DETECTED -> "⚡ Exploitation attempt detected from IP: " + ipAddress +
                    " - " + details;

            // Privilege Escalation
            case PRIVILEGE_ESCALATION_ATTEMPT -> "⬆️ Privilege escalation attempt by user: " + userName +
                    " from IP: " + ipAddress + " - " + details;

            case UNAUTHORIZED_ADMIN_ACCESS -> "👑 Unauthorized admin access attempt by user: " + userName +
                    " from IP: " + ipAddress;

            // System Security
            case SYSTEM_FILE_MODIFICATION -> "📁 Unauthorized system file modification detected - " + details;

            case LOG_TAMPERING_ATTEMPT -> "📝 Log tampering attempt detected - " + details;

            // Security Measures
            case IP_TEMPORARILY_BLOCKED -> log.isSuccessful() ?
                    "🚫 IP temporarily blocked: " + ipAddress + " - " + details :
                    "❌ Failed to block IP: " + ipAddress;

            case CAPTCHA_CHALLENGE_ISSUED -> "🤖 CAPTCHA challenge issued to IP: " + ipAddress +
                    " - " + details;

            case SECURITY_SCAN_INITIATED -> log.isSuccessful() ?
                    "🔍 Security scan initiated - " + details :
                    "❌ Failed to initiate security scan";

            // Default for other actions
            default -> "🛡️ Security event: " + this.getDescription() +
                    " - User: " + userName + ", IP: " + ipAddress + " - " + details;
        };
    }

    /**
     * Security-specific log enrichment
     */
    public void enrichSecurityLog(ActivityLog log) {
        // Set risk level
        log.setRiskLevel(this.getDefaultRiskLevel());

        // All security events trigger alerts
        log.setTriggeredAlert(true);

        // Set compliance category for security events
        log.setComplianceCategory("SECURITY");

        // Mark critical threats with error details for immediate attention
        if (this.getDefaultRiskLevel() == 5) {
            log.setErrorDetails("CRITICAL SECURITY THREAT - Immediate response required");
        }

        // Add security-specific metadata if not already present
        if (log.getMetadata() == null || log.getMetadata().isEmpty()) {
            log.setMetadata("{\"securityEvent\":true}");
        }
    }
}
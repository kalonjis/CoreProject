package be.steby.CoreProject.dl.enums.action_log_type;

/**
 * Security-related actions for activity logging
 */
public enum SecurityAction implements ActionLogType {
    IP_BLOCKED("IP address blocked due to too many attempts"),
    USERNAME_BLOCKED("Username blocked due to too many attempts"),
    COMBINED_BLOCKED("Username+IP combination blocked"),
    HIGH_RISK_ALERT("High-risk security pattern detected"),
    SUSPICIOUS_PATTERN("Suspicious login patterns detected"),
    BRUTE_FORCE_DETECTED("Brute force attack detected"),
    ACCOUNT_LOCKOUT("Account locked due to security policy"),
    SECURITY_BREACH_ATTEMPT("Potential security breach attempt");

    private final String description;

    SecurityAction(String description) { this.description = description; }


    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getCategory() {
        return "SECURITY";
    }
}
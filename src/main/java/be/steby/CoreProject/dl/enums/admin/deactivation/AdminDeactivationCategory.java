package be.steby.CoreProject.dl.enums.admin.deactivation;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.ReactivationPolicy;
import be.steby.CoreProject.dl.enums.UserRole;

/**
 * Complete administrative deactivation categories with comprehensive rules.
 * Each category defines deactivation permissions, reactivation policies, and business rules.
 *
 * ✅ COMPLETE VERSION with all existing methods + ReactivationPolicy integration
 */
public enum AdminDeactivationCategory {

    // ===== BANNED CATEGORIES =====
    BANNED_HATE_SPEECH(
            "Banned - Hate Speech",
            "Permanent ban for hate speech or discriminatory content",
            DeactivationMainCategory.BANNED,
            ReactivationPolicy.SUPER_ADMIN_ONLY
    ),
    BANNED_HARASSMENT(
            "Banned - Harassment",
            "Permanent ban for harassment of other users",
            DeactivationMainCategory.BANNED,
            ReactivationPolicy.SUPER_ADMIN_ONLY
    ),
    BANNED_INAPPROPRIATE_BEHAVIOR(
            "Banned - Inappropriate Behavior",
            "Permanent ban for severe inappropriate behavior",
            DeactivationMainCategory.BANNED,
            ReactivationPolicy.SUPER_ADMIN_ONLY
    ),

    // ===== LEGAL CATEGORIES =====
    LEGAL_REQUEST(
            "Legal Request",
            "Deactivation following legal authority request",
            DeactivationMainCategory.LEGAL,
            ReactivationPolicy.NEVER
    ),
    LEGAL_COPYRIGHT(
            "Copyright Violation",
            "Deactivation for copyright infringement",
            DeactivationMainCategory.LEGAL,
            ReactivationPolicy.NEVER
    ),
    LEGAL_MINOR_SAFETY(
            "Minor Safety",
            "Deactivation for minor protection concerns",
            DeactivationMainCategory.LEGAL,
            ReactivationPolicy.NEVER
    ),

    // ===== TOS VIOLATION CATEGORIES =====
    TOS_VIOLATION_SPAM(
            "TOS Violation - Spam",
            "Repeated sending of unwanted messages",
            DeactivationMainCategory.TOS_VIOLATION,
            ReactivationPolicy.ADMIN_ONLY
    ),
    TOS_VIOLATION_MULTIPLE_ACCOUNTS(
            "TOS Violation - Multiple Accounts",
            "Creating multiple fake or duplicate accounts",
            DeactivationMainCategory.TOS_VIOLATION,
            ReactivationPolicy.ADMIN_ONLY
    ),
    TOS_VIOLATION_COMMERCIAL_ABUSE(
            "TOS Violation - Commercial Abuse",
            "Unauthorized commercial or promotional activity",
            DeactivationMainCategory.TOS_VIOLATION,
            ReactivationPolicy.ADMIN_ONLY
    ),
    TOS_VIOLATION_UNDERAGE(
            "TOS Violation - Underage User",
            "User confirmed to be under minimum age requirement",
            DeactivationMainCategory.TOS_VIOLATION,
            ReactivationPolicy.ADMIN_ONLY
    ),
    TOS_VIOLATION_FRAUD(
            "TOS Violation - Fraud",
            "Fraudulent activity or financial misconduct",
            DeactivationMainCategory.TOS_VIOLATION,
            ReactivationPolicy.SUPER_ADMIN_ONLY
    ),
    TOS_VIOLATION_IMPERSONATION(
            "TOS Violation - Impersonation",
            "Impersonating other users or entities",
            DeactivationMainCategory.TOS_VIOLATION,
            ReactivationPolicy.SUPER_ADMIN_ONLY
    ),

    // ===== SECURITY RISK CATEGORIES =====
    SECURITY_RISK_COMPROMISED_ACCOUNT(
            "Security Risk - Compromised Account",
            "Account suspected of being compromised",
            DeactivationMainCategory.SECURITY_RISK,
            ReactivationPolicy.ADMIN_ONLY
    ),
    SECURITY_RISK_SUSPICIOUS_ACTIVITY(
            "Security Risk - Suspicious Activity",
            "Suspicious login patterns or activity",
            DeactivationMainCategory.SECURITY_RISK,
            ReactivationPolicy.ADMIN_ONLY
    ),
    SECURITY_RISK_MALWARE(
            "Security Risk - Malware",
            "Account spreading or infected with malware",
            DeactivationMainCategory.SECURITY_RISK,
            ReactivationPolicy.SUPER_ADMIN_ONLY
    ),

    // ===== MAINTENANCE CATEGORIES =====
    MAINTENANCE_SYSTEM_UPGRADE(
            "Maintenance - System Upgrade",
            "System upgrade requiring temporary deactivation",
            DeactivationMainCategory.MAINTENANCE,
            ReactivationPolicy.SELF_SERVICE
    ),
    MAINTENANCE_DATA_CLEANUP(
            "Maintenance - Data Cleanup",
            "Database cleanup requiring temporary deactivation",
            DeactivationMainCategory.MAINTENANCE,
            ReactivationPolicy.SELF_SERVICE
    ),
    MAINTENANCE_MIGRATION(
            "Maintenance - Data Migration",
            "Data migration requiring temporary deactivation",
            DeactivationMainCategory.MAINTENANCE,
            ReactivationPolicy.SELF_SERVICE
    ),

    // ===== ADMINISTRATIVE CATEGORIES =====
    ADMIN_ERROR(
            "Administrative Error",
            "Accidental deactivation due to administrative error",
            DeactivationMainCategory.ADMINISTRATIVE,
            ReactivationPolicy.SELF_SERVICE
    ),
    ADMIN_INVESTIGATION(
            "Administrative Investigation",
            "Temporary deactivation pending investigation",
            DeactivationMainCategory.ADMINISTRATIVE,
            ReactivationPolicy.ADMIN_ONLY
    ),
    OTHER_ADMIN_REASON(
            "Other Administrative Reason",
            "Other administrative reason not listed above",
            DeactivationMainCategory.ADMINISTRATIVE,
            ReactivationPolicy.ADMIN_ONLY
    );

    // ===== FIELDS =====
    private final String displayName;
    private final String description;
    private final DeactivationMainCategory mainCategory;
    private final ReactivationPolicy reactivationPolicy;

    // ===== CONSTRUCTOR =====
    AdminDeactivationCategory(String displayName, String description,
                              DeactivationMainCategory mainCategory,
                              ReactivationPolicy reactivationPolicy) {
        this.displayName = displayName;
        this.description = description;
        this.mainCategory = mainCategory;
        this.reactivationPolicy = reactivationPolicy;
    }

    // ===== BASIC GETTERS =====
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public DeactivationMainCategory getMainCategory() { return mainCategory; }
    public ReactivationPolicy getReactivationPolicy() { return reactivationPolicy; }

    // ===== REACTIVATION METHODS (for backward compatibility) =====

    /**
     * Determines if this category allows self-service reactivation.
     */
    public boolean allowsReactivation() {
        return reactivationPolicy == ReactivationPolicy.SELF_SERVICE;
    }

    /**
     * Determines if this category allows admin reactivation.
     */
    public boolean allowsAdminReactivation() {
        return reactivationPolicy == ReactivationPolicy.SELF_SERVICE
                || reactivationPolicy == ReactivationPolicy.ADMIN_ONLY
                || reactivationPolicy == ReactivationPolicy.SUPER_ADMIN_ONLY;
    }

    /**
     * Determines if this category requires SUPER_ADMIN for reactivation.
     */
    public boolean requiresSuperAdminReactivation() {
        return reactivationPolicy == ReactivationPolicy.SUPER_ADMIN_ONLY;
    }

    // ===== DEACTIVATION PERMISSION METHODS =====

    /**
     * ✅ USED BY AdminDeactivationRequest
     * Returns categories that require Super Admin approval for DEACTIVATION.
     * Note: This is different from reactivation rules!
     */
    public boolean requiresSuperAdminApproval() {
        return switch (this) {
            case LEGAL_REQUEST, LEGAL_MINOR_SAFETY, BANNED_HATE_SPEECH,
                 TOS_VIOLATION_FRAUD, SECURITY_RISK_MALWARE -> true;
            default -> mainCategory == DeactivationMainCategory.LEGAL;
        };
    }

    // ===== BUSINESS RULE METHODS =====

    /**
     * ✅ USED BY AdminDeactivationRequest
     * Returns categories that require session invalidation.
     */
    public boolean requiresSessionInvalidation() {
        return switch (mainCategory) {
            case SECURITY_RISK, BANNED -> true;
            case TOS_VIOLATION -> getSeverityLevel() >= 4;
            default -> false;
        };
    }

    /**
     * ✅ USED BY AdminDeactivationRequest
     * Returns the severity level of this category.
     */
    public int getSeverityLevel() {
        return switch (mainCategory) {
            case MAINTENANCE -> 1;
            case ADMINISTRATIVE -> 2;
            case TOS_VIOLATION -> switch (this) {
                case TOS_VIOLATION_SPAM, TOS_VIOLATION_MULTIPLE_ACCOUNTS -> 2;
                case TOS_VIOLATION_COMMERCIAL_ABUSE, TOS_VIOLATION_UNDERAGE -> 3;
                case TOS_VIOLATION_FRAUD, TOS_VIOLATION_IMPERSONATION -> 4;
                default -> 3;
            };
            case SECURITY_RISK -> 4;
            case BANNED, LEGAL -> 5;
        };
    }

    /**
     * ✅ USED BY AdminDeactivationRequest
     * Determines if this category requires detailed explanation.
     */
    public boolean requiresDetailedExplanation() {
        return switch (this) {
            case BANNED_INAPPROPRIATE_BEHAVIOR, BANNED_HARASSMENT, BANNED_HATE_SPEECH,
                 TOS_VIOLATION_FRAUD, TOS_VIOLATION_IMPERSONATION,
                 SECURITY_RISK_SUSPICIOUS_ACTIVITY, SECURITY_RISK_MALWARE,
                 LEGAL_REQUEST, ADMIN_ERROR, ADMIN_INVESTIGATION, OTHER_ADMIN_REASON -> true;
            default -> false;
        };
    }

    /**
     * ✅ USED BY AdminDeactivationRequest
     * Returns the log retention period for this category.
     */
    public int getLogRetentionDays() {
        return switch (this) {
            case LEGAL_REQUEST, LEGAL_COPYRIGHT, LEGAL_MINOR_SAFETY -> 2555; // 7 years
            case BANNED_HARASSMENT, BANNED_HATE_SPEECH -> 1095; // 3 years
            case SECURITY_RISK_MALWARE, TOS_VIOLATION_FRAUD -> 730; // 2 years
            default -> 365; // 1 year
        };
    }

    // ===== UTILITY METHODS =====

    /**
     * Checks if an actor can reactivate a user with this category.
     */
    public boolean canReactivate(User actor, User target) {
        return switch (reactivationPolicy) {
            case NEVER -> false;
            case SELF_SERVICE -> actor.equals(target) || hasAdminPermission(actor);
            case ADMIN_ONLY -> hasAdminPermission(actor);
            case SUPER_ADMIN_ONLY -> hasSuperAdminPermission(actor);
        };
    }

    private boolean hasAdminPermission(User user) {
        return user.getUserRoles().contains(UserRole.ADMIN)
                || user.getUserRoles().contains(UserRole.SUPER_ADMIN);
    }

    private boolean hasSuperAdminPermission(User user) {
        return user.getUserRoles().contains(UserRole.SUPER_ADMIN);
    }

    // ===== STATIC FILTER METHODS =====

    public static AdminDeactivationCategory[] getByPolicy(ReactivationPolicy policy) {
        return java.util.Arrays.stream(values())
                .filter(category -> category.reactivationPolicy == policy)
                .toArray(AdminDeactivationCategory[]::new);
    }

    public static AdminDeactivationCategory[] getReactivableCategories() {
        return java.util.Arrays.stream(values())
                .filter(category -> category.reactivationPolicy != ReactivationPolicy.NEVER)
                .toArray(AdminDeactivationCategory[]::new);
    }

    public static AdminDeactivationCategory[] getByMainCategory(DeactivationMainCategory mainCategory) {
        return java.util.Arrays.stream(values())
                .filter(category -> category.getMainCategory() == mainCategory)
                .toArray(AdminDeactivationCategory[]::new);
    }

    public static AdminDeactivationCategory[] getBySeverityLevel(int minLevel) {
        return java.util.Arrays.stream(values())
                .filter(category -> category.getSeverityLevel() >= minLevel)
                .toArray(AdminDeactivationCategory[]::new);
    }
}
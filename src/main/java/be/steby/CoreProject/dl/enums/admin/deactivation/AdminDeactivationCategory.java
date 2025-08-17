package be.steby.CoreProject.dl.enums.admin.deactivation;

/**
 * Administrative deactivation categories with comprehensive reactivation rules.
 * Each category defines its own self-reactivation, admin reactivation, and super admin requirements.
 *
 * ✅ OPTION 3: Systematic approach with all rules centralized in enum fields
 */
public enum AdminDeactivationCategory {

    // ❌ BANNED - Strict rules for permanent bans
    BANNED_HATE_SPEECH(
            "Banned for hate speech",
            "Permanent ban for hate speech or discriminatory content",
            DeactivationMainCategory.BANNED,
            false,  // allowsReactivation (self-service) → NO
            true,   // allowsAdminReactivation → YES (but restricted)
            true    // requiresSuperAdminReactivation → SUPER_ADMIN required
    ),
    BANNED_HARASSMENT(
            "Banned for harassment",
            "Permanent ban for harassment of other users",
            DeactivationMainCategory.BANNED,
            false,  // allowsReactivation → NO
            true,   // allowsAdminReactivation → YES (but restricted)
            true    // requiresSuperAdminReactivation → SUPER_ADMIN required
    ),
    BANNED_INAPPROPRIATE_BEHAVIOR(
            "Banned for inappropriate behavior",
            "Permanent ban for severe inappropriate behavior",
            DeactivationMainCategory.BANNED,
            false,  // allowsReactivation → NO
            true,   // allowsAdminReactivation → YES (but restricted)
            true    // requiresSuperAdminReactivation → SUPER_ADMIN required
    ),

    // ❌ LEGAL - NEVER allow reactivation (legal/compliance restrictions)
    LEGAL_REQUEST(
            "Legal request",
            "Deactivation following legal authority request",
            DeactivationMainCategory.LEGAL,
            false,  // allowsReactivation → NO
            false,  // allowsAdminReactivation → NEVER!
            false   // requiresSuperAdminReactivation → N/A
    ),
    LEGAL_COPYRIGHT(
            "Copyright violation",
            "Deactivation for copyright infringement",
            DeactivationMainCategory.LEGAL,
            false,  // allowsReactivation → NO
            false,  // allowsAdminReactivation → NEVER!
            false   // requiresSuperAdminReactivation → N/A
    ),
    LEGAL_MINOR_SAFETY(
            "Minor safety",
            "Deactivation for minor protection concerns",
            DeactivationMainCategory.LEGAL,
            false,  // allowsReactivation → NO
            false,  // allowsAdminReactivation → NEVER!
            false   // requiresSuperAdminReactivation → N/A
    ),

    // ✅ MAINTENANCE - ALLOW reactivation (temporary/operational)
    MAINTENANCE_SYSTEM_UPGRADE(
            "System maintenance",
            "System upgrade requiring temporary deactivation",
            DeactivationMainCategory.MAINTENANCE,
            true,   // allowsReactivation → YES
            true,   // allowsAdminReactivation → YES
            false   // requiresSuperAdminReactivation → Regular admin OK
    ),
    MAINTENANCE_DATA_CLEANUP(
            "Data cleanup",
            "Database cleanup requiring temporary deactivation",
            DeactivationMainCategory.MAINTENANCE,
            true,   // allowsReactivation → YES
            true,   // allowsAdminReactivation → YES
            false   // requiresSuperAdminReactivation → Regular admin OK
    ),

    // ✅ ADMINISTRATIVE - ALLOW reactivation (admin errors/special cases)
    ADMIN_ERROR(
            "Administrative error",
            "Accidental deactivation due to administrative error",
            DeactivationMainCategory.ADMINISTRATIVE,
            true,   // allowsReactivation → YES
            true,   // allowsAdminReactivation → YES
            false   // requiresSuperAdminReactivation → Regular admin OK
    ),
    OTHER_ADMIN_REASON(
            "Other administrative reason",
            "Other administrative reason not listed above",
            DeactivationMainCategory.ADMINISTRATIVE,
            true,   // allowsReactivation → YES
            true,   // allowsAdminReactivation → YES
            false   // requiresSuperAdminReactivation → Regular admin OK
    ),

    // 🔄 TOS_VIOLATION - Mixed rules based on severity
    TOS_VIOLATION_SPAM(
            "TOS Violation - Spam",
            "Repeated sending of unwanted messages",
            DeactivationMainCategory.TOS_VIOLATION,
            true,   // allowsReactivation → YES (low severity)
            true,   // allowsAdminReactivation → YES
            false   // requiresSuperAdminReactivation → Regular admin OK
    ),
    TOS_VIOLATION_MULTIPLE_ACCOUNTS(
            "TOS Violation - Multiple accounts",
            "Creation of unauthorized multiple accounts",
            DeactivationMainCategory.TOS_VIOLATION,
            true,   // allowsReactivation → YES (low severity)
            true,   // allowsAdminReactivation → YES
            false   // requiresSuperAdminReactivation → Regular admin OK
    ),
    TOS_VIOLATION_COMMERCIAL_ABUSE(
            "TOS Violation - Commercial abuse",
            "Unauthorized commercial use of the platform",
            DeactivationMainCategory.TOS_VIOLATION,
            false,  // allowsReactivation → NO (medium severity)
            true,   // allowsAdminReactivation → YES
            false   // requiresSuperAdminReactivation → Regular admin OK
    ),
    TOS_VIOLATION_FRAUD(
            "TOS Violation - Fraud",
            "Attempt at fraud or financial scam",
            DeactivationMainCategory.TOS_VIOLATION,
            false,  // allowsReactivation → NO (high severity)
            true,   // allowsAdminReactivation → YES (but restricted)
            true    // requiresSuperAdminReactivation → SUPER_ADMIN required
    ),
    TOS_VIOLATION_IMPERSONATION(
            "TOS Violation - Impersonation",
            "Impersonation of other users or entities",
            DeactivationMainCategory.TOS_VIOLATION,
            false,  // allowsReactivation → NO (high severity)
            true,   // allowsAdminReactivation → YES (but restricted)
            true    // requiresSuperAdminReactivation → SUPER_ADMIN required
    ),
    TOS_VIOLATION_UNDERAGE(
            "TOS Violation - Underage user",
            "User below the required legal age",
            DeactivationMainCategory.TOS_VIOLATION,
            false,  // allowsReactivation → NO (legal compliance)
            false,  // allowsAdminReactivation → NO (legal compliance)
            false   // requiresSuperAdminReactivation → N/A
    ),

    // 🔄 SECURITY_RISK - Mixed rules based on risk type
    SECURITY_RISK_COMPROMISED(
            "Security risk - Compromised account",
            "Account potentially compromised by third party",
            DeactivationMainCategory.SECURITY_RISK,
            true,   // allowsReactivation → YES (after securing)
            true,   // allowsAdminReactivation → YES
            false   // requiresSuperAdminReactivation → Regular admin OK
    ),
    SECURITY_RISK_SUSPICIOUS_ACTIVITY(
            "Security risk - Suspicious activity",
            "Detection of suspicious activities",
            DeactivationMainCategory.SECURITY_RISK,
            false,  // allowsReactivation → NO (investigation needed)
            true,   // allowsAdminReactivation → YES (but restricted)
            true    // requiresSuperAdminReactivation → SUPER_ADMIN required
    ),
    SECURITY_RISK_MALWARE(
            "Security risk - Malware",
            "Detection of malicious software",
            DeactivationMainCategory.SECURITY_RISK,
            false,  // allowsReactivation → NO (risk too high)
            true,   // allowsAdminReactivation → YES (but restricted)
            true    // requiresSuperAdminReactivation → SUPER_ADMIN required
    );

    // ===== FIELDS =====
    private final String displayName;
    private final String description;
    private final DeactivationMainCategory mainCategory;
    private final boolean allowsReactivation;           // ✅ Self-service reactivation
    private final boolean allowsAdminReactivation;      // ✅ NEW - Admin can reactivate
    private final boolean requiresSuperAdminReactivation; // ✅ NEW - Requires SUPER_ADMIN

    /**
     * ✅ ENHANCED CONSTRUCTOR with all reactivation rules
     *
     * @param displayName Human-readable name for this category
     * @param description Detailed description of this category
     * @param mainCategory The main category this belongs to
     * @param allowsReactivation Whether self-service reactivation is allowed
     * @param allowsAdminReactivation Whether admin reactivation is allowed
     * @param requiresSuperAdminReactivation Whether SUPER_ADMIN role is required for reactivation
     */
    AdminDeactivationCategory(String displayName, String description,
                              DeactivationMainCategory mainCategory,
                              boolean allowsReactivation,
                              boolean allowsAdminReactivation,
                              boolean requiresSuperAdminReactivation) {
        this.displayName = displayName;
        this.description = description;
        this.mainCategory = mainCategory;
        this.allowsReactivation = allowsReactivation;
        this.allowsAdminReactivation = allowsAdminReactivation;
        this.requiresSuperAdminReactivation = requiresSuperAdminReactivation;
    }

    // ===== BASIC GETTERS =====
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public DeactivationMainCategory getMainCategory() { return mainCategory; }

    // ===== ✅ REACTIVATION RULE GETTERS =====

    /**
     * Determines if this category allows self-service reactivation.
     *
     * @return true if user can reactivate their own account
     */
    public boolean allowsReactivation() {
        return allowsReactivation;
    }

    /**
     * ✅ NEW - Determines if this category allows admin reactivation.
     *
     * @return true if admin can reactivate this account
     */
    public boolean allowsAdminReactivation() {
        return allowsAdminReactivation;
    }

    /**
     * ✅ NEW - Determines if this category requires SUPER_ADMIN for reactivation.
     *
     * @return true if only SUPER_ADMIN can reactivate this account
     */
    public boolean requiresSuperAdminReactivation() {
        return requiresSuperAdminReactivation;
    }

    // ===== DERIVED RULES (preserved from original) =====

    /**
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
     * Returns categories that require Super Admin approval for DEACTIVATION.
     * Note: This is different from reactivation rules!
     */
    public boolean requiresSuperAdminApproval() {
        return switch (this) {
            case LEGAL_REQUEST, LEGAL_MINOR_SAFETY, BANNED_HATE_SPEECH -> true;
            default -> mainCategory == DeactivationMainCategory.LEGAL;
        };
    }

    /**
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
     * Determines if this category requires detailed explanation.
     */
    public boolean requiresDetailedExplanation() {
        return switch (this) {
            case BANNED_INAPPROPRIATE_BEHAVIOR, BANNED_HARASSMENT, BANNED_HATE_SPEECH,
                 TOS_VIOLATION_FRAUD, TOS_VIOLATION_IMPERSONATION,
                 SECURITY_RISK_SUSPICIOUS_ACTIVITY, LEGAL_REQUEST,
                 ADMIN_ERROR, OTHER_ADMIN_REASON -> true;
            default -> false;
        };
    }

    /**
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

    // ===== ✅ UTILITY METHODS =====

    /**
     * Returns all categories that allow self-reactivation.
     */
    public static AdminDeactivationCategory[] getReactivableCategories() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::allowsReactivation)
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * ✅ NEW - Returns all categories that allow admin reactivation.
     */
    public static AdminDeactivationCategory[] getAdminReactivableCategories() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::allowsAdminReactivation)
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * ✅ NEW - Returns all categories that require SUPER_ADMIN for reactivation.
     */
    public static AdminDeactivationCategory[] getSuperAdminOnlyCategories() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::requiresSuperAdminReactivation)
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * ✅ NEW - Returns all categories that NEVER allow reactivation.
     */
    public static AdminDeactivationCategory[] getNeverReactivableCategories() {
        return java.util.Arrays.stream(values())
                .filter(category -> !category.allowsAdminReactivation())
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * Returns all subcategories of a main category.
     */
    public static AdminDeactivationCategory[] getByMainCategory(DeactivationMainCategory mainCategory) {
        return java.util.Arrays.stream(values())
                .filter(category -> category.getMainCategory() == mainCategory)
                .toArray(AdminDeactivationCategory[]::new);
    }

    // ===== COUNTS =====

    public static long getReactivableCount() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::allowsReactivation)
                .count();
    }

    public static long getAdminReactivableCount() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::allowsAdminReactivation)
                .count();
    }

    public static long getSuperAdminOnlyCount() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::requiresSuperAdminReactivation)
                .count();
    }

    public static long getNeverReactivableCount() {
        return java.util.Arrays.stream(values())
                .filter(category -> !category.allowsAdminReactivation())
                .count();
    }
}
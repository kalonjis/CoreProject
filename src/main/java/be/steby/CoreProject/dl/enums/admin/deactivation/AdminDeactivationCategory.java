package be.steby.CoreProject.dl.enums.admin.deactivation;

/**
 * Administrative deactivation categories with built-in reactivation rules.
 * Each category specifies whether it allows automatic reactivation.
 */
public enum AdminDeactivationCategory {

    // ❌ BANNED - DO NOT allow reactivation (permanent bans)
    BANNED_HATE_SPEECH(
            "Banned for hate speech",
            "Permanent ban for hate speech or discriminatory content",
            DeactivationMainCategory.BANNED,
            false  // ❌ allowsReactivation
    ),
    BANNED_HARASSMENT(
            "Banned for harassment",
            "Permanent ban for harassment of other users",
            DeactivationMainCategory.BANNED,
            false  // ❌ allowsReactivation
    ),
    BANNED_INAPPROPRIATE_BEHAVIOR(
            "Banned for inappropriate behavior",
            "Permanent ban for severe inappropriate behavior",
            DeactivationMainCategory.BANNED,
            false  // ❌ allowsReactivation
    ),

    // ❌ LEGAL - DO NOT allow reactivation (legal/compliance issues)
    LEGAL_REQUEST(
            "Legal request",
            "Deactivation following legal authority request",
            DeactivationMainCategory.LEGAL,
            false  // ❌ allowsReactivation
    ),
    LEGAL_COPYRIGHT(
            "Copyright violation",
            "Deactivation for copyright infringement",
            DeactivationMainCategory.LEGAL,
            false  // ❌ allowsReactivation
    ),
    LEGAL_MINOR_SAFETY(
            "Minor safety",
            "Deactivation for minor protection concerns",
            DeactivationMainCategory.LEGAL,
            false  // ❌ allowsReactivation
    ),

    // ✅ MAINTENANCE - ALLOW reactivation (temporary/operational)
    MAINTENANCE_SYSTEM_UPGRADE(
            "System maintenance",
            "System upgrade requiring temporary deactivation",
            DeactivationMainCategory.MAINTENANCE,
            true   // ✅ allowsReactivation
    ),
    MAINTENANCE_DATA_CLEANUP(
            "Data cleanup",
            "Database cleanup requiring temporary deactivation",
            DeactivationMainCategory.MAINTENANCE,
            true   // ✅ allowsReactivation
    ),

    // ✅ ADMINISTRATIVE - ALLOW reactivation (admin errors/special cases)
    ADMIN_ERROR(
            "Administrative error",
            "Accidental deactivation due to administrative error",
            DeactivationMainCategory.ADMINISTRATIVE,
            true   // ✅ allowsReactivation
    ),
    OTHER_ADMIN_REASON(
            "Other administrative reason",
            "Other administrative reason not listed above",
            DeactivationMainCategory.ADMINISTRATIVE,
            true   // ✅ allowsReactivation
    ),

    // 🔄 TOS_VIOLATION - Mixed (depends on severity)
    TOS_VIOLATION_SPAM(
            "TOS Violation - Spam",
            "Repeated sending of unwanted messages",
            DeactivationMainCategory.TOS_VIOLATION,
            true   // ✅ Low severity -> reactivation possible
    ),
    TOS_VIOLATION_MULTIPLE_ACCOUNTS(
            "TOS Violation - Multiple accounts",
            "Creation of unauthorized multiple accounts",
            DeactivationMainCategory.TOS_VIOLATION,
            true   // ✅ Low severity -> reactivation possible
    ),
    TOS_VIOLATION_COMMERCIAL_ABUSE(
            "TOS Violation - Commercial abuse",
            "Unauthorized commercial use of the platform",
            DeactivationMainCategory.TOS_VIOLATION,
            false  // ❌ Medium severity -> no reactivation
    ),
    TOS_VIOLATION_FRAUD(
            "TOS Violation - Fraud",
            "Attempt at fraud or financial scam",
            DeactivationMainCategory.TOS_VIOLATION,
            false  // ❌ High severity -> no reactivation
    ),
    TOS_VIOLATION_IMPERSONATION(
            "TOS Violation - Impersonation",
            "Impersonation of other users or entities",
            DeactivationMainCategory.TOS_VIOLATION,
            false  // ❌ High severity -> no reactivation
    ),
    TOS_VIOLATION_UNDERAGE(
            "TOS Violation - Underage user",
            "User below the required legal age",
            DeactivationMainCategory.TOS_VIOLATION,
            false  // ❌ Legal compliance -> no reactivation
    ),

    // 🔄 SECURITY_RISK - Mixed (depends on risk type)
    SECURITY_RISK_COMPROMISED(
            "Security risk - Compromised account",
            "Account potentially compromised by third party",
            DeactivationMainCategory.SECURITY_RISK,
            true   // ✅ Can be reactivated after securing
    ),
    SECURITY_RISK_SUSPICIOUS_ACTIVITY(
            "Security risk - Suspicious activity",
            "Detection of suspicious activities",
            DeactivationMainCategory.SECURITY_RISK,
            false  // ❌ Investigation needed
    ),
    SECURITY_RISK_MALWARE(
            "Security risk - Malware",
            "Detection of malicious software",
            DeactivationMainCategory.SECURITY_RISK,
            false  // ❌ Risk too high
    );

    private final String displayName;
    private final String description;
    private final DeactivationMainCategory mainCategory;
    private final boolean allowsReactivation;  // ✅ NEW FIELD

    /**
     * ✅ MODIFIED CONSTRUCTOR with allowsReactivation parameter.
     *
     * @param displayName Human-readable name for this category
     * @param description Detailed description of this category
     * @param mainCategory The main category this belongs to
     * @param allowsReactivation Whether this category allows automatic reactivation
     */
    AdminDeactivationCategory(String displayName, String description,
                              DeactivationMainCategory mainCategory,
                              boolean allowsReactivation) {
        this.displayName = displayName;
        this.description = description;
        this.mainCategory = mainCategory;
        this.allowsReactivation = allowsReactivation;
    }

    // ===== EXISTING GETTERS =====
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public DeactivationMainCategory getMainCategory() { return mainCategory; }

    // ===== NEW SIMPLE METHOD - replaces complex switch statement! =====
    /**
     * ✅ SIMPLIFIED METHOD - Determines if this category allows reactivation.
     * Replaces the complex switch statement with direct field access.
     *
     * @return true if reactivation is allowed, false otherwise
     */
    public boolean allowsReactivation() {
        return allowsReactivation;
    }

    // ===== UTILITY METHODS =====

    /**
     * Returns all categories that allow reactivation.
     *
     * @return Array of categories that allow reactivation
     */
    public static AdminDeactivationCategory[] getReactivableCategories() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::allowsReactivation)
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * Returns all categories that do NOT allow reactivation.
     *
     * @return Array of categories that do not allow reactivation
     */
    public static AdminDeactivationCategory[] getNonReactivableCategories() {
        return java.util.Arrays.stream(values())
                .filter(category -> !category.allowsReactivation())
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * Returns all subcategories of a main category.
     *
     * @param mainCategory The main category to filter by
     * @return Array of subcategories
     */
    public static AdminDeactivationCategory[] getByMainCategory(DeactivationMainCategory mainCategory) {
        return java.util.Arrays.stream(values())
                .filter(category -> category.getMainCategory() == mainCategory)
                .toArray(AdminDeactivationCategory[]::new);
    }

    /**
     * Returns categories that require session invalidation.
     * This logic is preserved from the original implementation.
     *
     * @return true if this category requires session invalidation
     */
    public boolean requiresSessionInvalidation() {
        return switch (mainCategory) {
            case SECURITY_RISK, BANNED -> true;
            case TOS_VIOLATION -> getSeverityLevel() >= 4;
            default -> false;
        };
    }

    /**
     * Returns categories that require Super Admin approval.
     * This logic is preserved from the original implementation.
     *
     * @return true if this category requires Super Admin approval
     */
    public boolean requiresSuperAdminApproval() {
        return switch (this) {
            case LEGAL_REQUEST, LEGAL_MINOR_SAFETY, BANNED_HATE_SPEECH -> true;
            default -> mainCategory == DeactivationMainCategory.LEGAL;
        };
    }

    /**
     * Returns the severity level of this category.
     * This logic is preserved from the original implementation.
     *
     * @return Severity level (1-5, higher = more severe)
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
     * This logic is preserved from the original implementation.
     *
     * @return true if detailed explanation is required
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
     * This logic is preserved from the original implementation.
     *
     * @return Log retention period in days
     */
    public int getLogRetentionDays() {
        return switch (this) {
            case LEGAL_REQUEST, LEGAL_COPYRIGHT, LEGAL_MINOR_SAFETY -> 2555; // 7 years
            case BANNED_HARASSMENT, BANNED_HATE_SPEECH -> 1095; // 3 years
            case SECURITY_RISK_MALWARE, TOS_VIOLATION_FRAUD -> 730; // 2 years
            default -> 365; // 1 year
        };
    }

    /**
     * Returns the count of categories that allow reactivation.
     *
     * @return Count of reactivable categories
     */
    public static long getReactivableCount() {
        return java.util.Arrays.stream(values())
                .filter(AdminDeactivationCategory::allowsReactivation)
                .count();
    }

    /**
     * Returns the count of categories that do NOT allow reactivation.
     *
     * @return Count of non-reactivable categories
     */
    public static long getNonReactivableCount() {
        return java.util.Arrays.stream(values())
                .filter(category -> !category.allowsReactivation())
                .count();
    }
}
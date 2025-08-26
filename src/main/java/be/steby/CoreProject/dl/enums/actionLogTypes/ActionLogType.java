package be.steby.CoreProject.dl.enums.actionLogTypes;

import be.steby.CoreProject.dl.entities.ActivityLog;
import be.steby.CoreProject.dl.entities.User;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sealed interface for all activity log action types.
 * Replaces the monolithic enum with domain-specific enums.
 *
 * Currently permits:
 * - AuthAction (authentication domain)
 *
 * Future domains to be added:
 * - AdminAction, AccountAction, SecurityAction, etc.
 */
public sealed interface ActionLogType
        permits AuthAction {
    // TODO: Add other domains: AdminAction, AccountAction, SecurityAction,
    //       PasswordAction, EmailAction, DeviceAction, ApiAction, etc.

    // ================== ABSTRACT METHODS (must be implemented by all enums) ==================

    /**
     * Human-readable description of the action
     */
    String getDescription();

    /**
     * Category/domain of the action (AUTH, ADMIN, ACCOUNT, etc.)
     */
    String getCategory();

    // Note: name() is automatically provided by enum implementations

    // ================== COMMON DEFAULT METHODS ==================

    /**
     * Central pattern matching dispatcher - calls the appropriate domain-specific processor
     */
    default String processAction(ActivityLog log) {
        return switch (this) {
            case AuthAction auth -> auth.processAuthAction(log);
            // TODO: Add other domains when implemented
            // case AdminAction admin -> admin.processAdminAction(log);
            // case AccountAction account -> account.processAccountAction(log);
            // case SecurityAction security -> security.processSecurityAction(log);
        };
    }

    /**
     * Central enrichment dispatcher - calls the appropriate domain-specific enrichment
     */
    default void enrichActivityLog(ActivityLog log) {
        switch (this) {
            case AuthAction auth -> auth.enrichAuthLog(log);
            // TODO: Add other domains when implemented
            // case AdminAction admin -> admin.enrichAdminLog(log);
            // case AccountAction account -> account.enrichAccountLog(log);
            // case SecurityAction security -> security.enrichSecurityLog(log);
        }
    }

    /**
     * Basic validation that applies to all action types
     */
    default boolean validateBasicRequirements(ActivityLog log) {
        return log != null
                && log.getActionType() != null
                && log.getActionCategory() != null
                && log.getTimestamp() != null;
    }

    /**
     * Domain-specific validation (to be overridden by specific domains)
     */
    default boolean validateForDomain(ActivityLog log, User user) {
        return validateBasicRequirements(log) && user != null;
    }

    /**
     * Factory method to create a basic ActivityLog for this action type
     */
    default ActivityLog createActivityLog(User user, boolean successful) {
        return ActivityLog.builderWithTimestamp()
                .user(user)
                .actionType(this.name())
                .actionCategory(this.getCategory())
                .successful(successful)
                .build();
    }

    /**
     * Factory method to create ActivityLog with context
     */
    default ActivityLog createActivityLogWithContext(User user, boolean successful, RequestContext context) {
        ActivityLog log = createActivityLog(user, successful);

        if (context != null) {
            log.setIpAddress(context.getIpAddress());
            log.setLocation(context.getLocation());
            log.setSessionId(context.getSessionId());
            log.setUserAgent(context.getUserAgent());
        }

        // Auto-enrich based on action type
        this.enrichActivityLog(log);

        return log;
    }

    /**
     * Get expected duration for this action (can be overridden by domains)
     */
    default Duration getExpectedDuration() {
        return Duration.ofSeconds(5); // Default 5 seconds
    }

    /**
     * Get default risk level for this action type
     */
    default int getDefaultRiskLevel() {
        return 1; // Low risk by default
    }

    /**
     * Check if this action requires compliance logging
     */
    default boolean requiresComplianceLogging() {
        return false; // Most actions don't require compliance logging
    }

    /**
     * Get UI color for this action (for frontend display)
     */
    default String getUIColor() {
        return "#6c757d"; // Default gray
    }

    /**
     * Get icon class for this action (for frontend display)
     */
    default String getIconClass() {
        return "fas fa-circle"; // Default circle icon
    }

    // ================== STATIC UTILITY METHODS ==================

    /**
     * Convert string representation back to sealed type
     * This is the key method for reconstructing types from database
     */
    static ActionLogType fromString(String actionName, String category) {
        return switch (category.toUpperCase()) {
            case "AUTH" -> AuthAction.valueOf(actionName);
            // TODO: Add other domains when implemented
            // case "ADMIN" -> AdminAction.valueOf(actionName);
            // case "ACCOUNT" -> AccountAction.valueOf(actionName);
            // case "SECURITY" -> SecurityAction.valueOf(actionName);
            // case "PASSWORD" -> PasswordAction.valueOf(actionName);
            // case "EMAIL" -> EmailAction.valueOf(actionName);
            // case "DEVICE" -> DeviceAction.valueOf(actionName);
            // case "API" -> ApiAction.valueOf(actionName);
            // case "PROFILE" -> ProfileAction.valueOf(actionName);
            // case "ROLE" -> RoleAction.valueOf(actionName);
            // case "SYSTEM" -> SystemAction.valueOf(actionName);
            default -> throw new IllegalArgumentException("Unknown action category: " + category + " with action: " + actionName);
        };
    }

    /**
     * Get all action types for a specific category
     */
    static List<ActionLogType> getByCategory(String category) {
        return switch (category.toUpperCase()) {
            case "AUTH" -> List.of(AuthAction.values());
            // TODO: Add other domains when implemented
            // case "ADMIN" -> List.of(AdminAction.values());
            // case "ACCOUNT" -> List.of(AccountAction.values());
            default -> List.of();
        };
    }

    /**
     * Get statistics by category from a list of activity logs
     */
    static Map<String, Long> getCategoryStats(List<ActivityLog> logs) {
        return logs.stream()
                .collect(Collectors.groupingBy(
                        ActivityLog::getActionCategory,
                        Collectors.counting()
                ));
    }

    /**
     * Get all available categories
     */
    static List<String> getAllCategories() {
        return List.of("AUTH"); // TODO: Add other categories as they're implemented
    }

    /**
     * Convert ActivityLog back to ActionLogType (helper for repositories)
     */
    static ActionLogType fromActivityLog(ActivityLog log) {
        if (log == null || log.getActionType() == null || log.getActionCategory() == null) {
            throw new IllegalArgumentException("ActivityLog must have actionType and actionCategory set");
        }
        return fromString(log.getActionType(), log.getActionCategory());
    }

    /**
     * Bulk process multiple activity logs
     */
    static Map<ActionLogType, List<String>> bulkProcess(List<ActivityLog> logs) {
        return logs.stream()
                .collect(Collectors.groupingBy(
                        ActionLogType::fromActivityLog,
                        Collectors.mapping(
                                log -> fromActivityLog(log).processAction(log),
                                Collectors.toList()
                        )
                ));
    }

    /**
     * Get actions by risk level
     */
    static List<ActivityLog> filterByRiskLevel(List<ActivityLog> logs, int minRiskLevel) {
        return logs.stream()
                .filter(log -> log.getRiskLevel() != null && log.getRiskLevel() >= minRiskLevel)
                .collect(Collectors.toList());
    }

    /**
     * Get failed actions
     */
    static List<ActivityLog> getFailedActions(List<ActivityLog> logs) {
        return logs.stream()
                .filter(log -> !log.isSuccessful())
                .collect(Collectors.toList());
    }

    /**
     * Get actions within time range
     */
    static List<ActivityLog> getActionsInTimeRange(List<ActivityLog> logs, Instant start, Instant end) {
        return logs.stream()
                .filter(log -> log.getTimestamp() != null
                        && log.getTimestamp().isAfter(start)
                        && log.getTimestamp().isBefore(end))
                .collect(Collectors.toList());
    }

    // ================== VALIDATION HELPERS ==================

    /**
     * Validate that an action name exists for a category
     */
    static boolean isValidAction(String actionName, String category) {
        try {
            fromString(actionName, category);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get all valid action names for a category
     */
    static List<String> getValidActionsForCategory(String category) {
        return getByCategory(category).stream()
                .map(ActionLogType::name)
                .collect(Collectors.toList());
    }
}
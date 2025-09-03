package be.steby.CoreProject.bll.common.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.action_log_type.ActionLogType;

/**
 * Simplified event triggered when a user performs an action that needs to be logged.
 * Used for asynchronous activity logging with the new device caching system.
 *
 * RequestContext has been removed - device detection is now handled at the service layer
 * before event publishing, making this event much simpler and more focused.
 */
public record UserActionEvent(
        User user,
        Device device,                // May be null if device detection failed
        ActionLogType actionType,
        Boolean successful,           // null = unknown, true = success, false = failure
        String details,
        String failureReason         // Only used when successful = false
) {

    // ================== CONVENIENCE CONSTRUCTORS ==================

    /**
     * Constructor for successful action without failure reason
     */
    public UserActionEvent(User user, Device device, ActionLogType actionType, boolean successful, String details) {
        this(user, device, actionType, successful, details, null);
    }

    /**
     * Constructor for action with unknown success status (generic logging)
     */
    public UserActionEvent(User user, Device device, ActionLogType actionType, String details) {
        this(user, device, actionType, null, details, null);
    }

    // ================== STATIC FACTORY METHODS ==================

    /**
     * Create a successful action event
     */
    public static UserActionEvent success(User user, Device device, ActionLogType actionType, String details) {
        return new UserActionEvent(user, device, actionType, true, details, null);
    }

    /**
     * Create a failed action event
     */
    public static UserActionEvent failure(User user, Device device, ActionLogType actionType,
                                          String details, String failureReason) {
        return new UserActionEvent(user, device, actionType, false, details, failureReason);
    }

    /**
     * Create a generic action event (success status unknown)
     */
    public static UserActionEvent generic(User user, Device device, ActionLogType actionType, String details) {
        return new UserActionEvent(user, device, actionType, null, details, null);
    }

    // ================== VALIDATION AND UTILITY METHODS ==================

    /**
     * Check if this event represents a successful action
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(successful);
    }

    /**
     * Check if this event represents a failed action
     */
    public boolean isFailed() {
        return Boolean.FALSE.equals(successful);
    }

    /**
     * Check if this event has device information
     */
    public boolean hasDevice() {
        return device != null;
    }

    /**
     * Check if this event has failure information
     */
    public boolean hasFailureReason() {
        return failureReason != null && !failureReason.trim().isEmpty();
    }

    /**
     * Get device ID for logging purposes
     */
    public String getDeviceIdForLogging() {
        return device != null ? device.getId().toString() : "unknown";
    }

    /**
     * Get a formatted string for logging
     */
    public String toLogString() {
        return String.format("UserActionEvent[user=%s, device=%s, action=%s, success=%s]",
                user != null ? user.getUsername() : "unknown",
                getDeviceIdForLogging(),
                actionType,
                successful
        );
    }

    // ================== VALIDATION ==================

    /**
     * Validate the event has required fields
     */
    public void validate() {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null in UserActionEvent");
        }
        if (actionType == null) {
            throw new IllegalArgumentException("ActionLogType cannot be null in UserActionEvent");
        }
        if (Boolean.FALSE.equals(successful) && (failureReason == null || failureReason.trim().isEmpty())) {
            // This is just a warning - we don't want to break the flow
            // but it's good to know if failure events lack failure reasons
        }
    }
}
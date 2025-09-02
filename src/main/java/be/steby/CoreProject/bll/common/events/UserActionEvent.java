package be.steby.CoreProject.bll.common.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.action_log_type.ActionLogType;

/**
 * Event triggered when a user performs an action that needs to be logged
 * Used for asynchronous activity logging
 */
public record UserActionEvent(
        User user,
        Device device,
        ActionLogType actionType,
        boolean successful,
        String details,
        String failureReason,
        RequestContext requestContext
) {

    // ================== CONVENIENCE CONSTRUCTORS ==================

    /**
     * Constructor without context (most common case)
     */
    public UserActionEvent(User user, Device device, ActionLogType actionType, boolean successful, String details) {
        this(user, device, actionType, successful, details, null, null);
    }

    /**
     * Constructor for successful action with context
     */
    public UserActionEvent(User user, Device device, ActionLogType actionType, boolean successful,
                           String details, RequestContext requestContext) {
        this(user, device, actionType, successful, details, null, requestContext);
    }

    /**
     * Constructor for failed action
     */
    public UserActionEvent(User user, Device device, ActionLogType actionType, boolean successful,
                           String details, String failureReason) {
        this(user, device, actionType, successful, details, failureReason, null);
    }

    /**
     * Simple success constructor
     */
    public static UserActionEvent success(User user, Device device, ActionLogType actionType, String details) {
        return new UserActionEvent(user, device, actionType, true, details);
    }

    /**
     * Simple failure constructor
     */
    public static UserActionEvent failure(User user, Device device, ActionLogType actionType,
                                          String details, String failureReason) {
        return new UserActionEvent(user, device, actionType, false, details, failureReason);
    }

    // ================== UTILITY METHODS ==================

    /**
     * Check if this is a successful action
     */
    public boolean isSuccess() {
        return successful;
    }

    /**
     * Check if this is a failed action
     */
    public boolean isFailure() {
        return !successful;
    }

    /**
     * Get action category from the action type
     */
    public String getActionCategory() {
        return actionType.getCategory();
    }

    /**
     * Get action description
     */
    public String getActionDescription() {
        return actionType.getDescription();
    }

    /**
     * Check if request context is available
     */
    public boolean hasRequestContext() {
        return requestContext != null;
    }

    /**
     * Get IP address from context if available
     */
    public String getClientIpAddress() {
        return hasRequestContext() ? requestContext.getClientIp() : null;
    }

    /**
     * Get user agent from context if available
     */
    public String getUserAgent() {
        return hasRequestContext() ? requestContext.getUserAgent() : null;
    }

    /**
     * Get session ID from context if available
     */
    public String getSessionId() {
        return hasRequestContext() ? requestContext.getSessionId() : null;
    }
}
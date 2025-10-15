package be.steby.CoreProject.bll.domains.auth.exceptions;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;

/**
 * Exception thrown when a user with mustChangePassword=true attempts
 * to access a restricted resource.
 * 
 * <p>This exception is automatically handled by the GlobalExceptionHandler
 * (ControllerAdvisor) which will return a 403 Forbidden response.
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public class PasswordChangeRequiredException extends CoreProjectException {
    
    /**
     * Creates a new PasswordChangeRequiredException with default 403 status.
     * 
     * @param message Error message explaining why password change is required
     */
    public PasswordChangeRequiredException(String message) {
        super(message, 403);
    }
}
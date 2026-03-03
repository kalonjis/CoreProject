package be.steby.CoreProject.bll.domains.password.models;

/**
 * Business layer DTO for password reset initiation.
 *
 * <p>The reset type is no longer carried here — each dedicated service
 * (EmailLinkPasswordResetService, EmailCodePasswordResetService, SmsPasswordResetService)
 * knows its own type by definition.
 *
 * @param email the user's normalized email address (lowercase, trimmed)
 */
public record ForgotPasswordBLLRequest(String email) {}
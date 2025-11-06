package be.steby.CoreProject.pl.domains.password.controllers;

import be.steby.CoreProject.bll.domains.password.models.SmsPasswordResetResult;
import be.steby.CoreProject.bll.domains.password.models.SmsVerificationResult;
import be.steby.CoreProject.bll.domains.password.services.PasswordService;
import be.steby.CoreProject.bll.domains.password.services.cookies.PasswordCookieService;
import be.steby.CoreProject.dl.enums.NotificationType;
import be.steby.CoreProject.pl.domains.password.models.requests.*;
import be.steby.CoreProject.pl.domains.password.models.responses.PasswordOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for password management operations.
 *
 * <p>Handles three main flows:
 * <ul>
 *   <li>Password change for authenticated users</li>
 *   <li>Password reset request (forgot password) via email or SMS</li>
 *   <li>Password reset completion with token</li>
 * </ul>
 *
 * <p>Security considerations:
 * <ul>
 *   <li>All responses use generic messages to prevent user enumeration</li>
 *   <li>Rate limiting should be configured at infrastructure level</li>
 *   <li>All operations are logged for security monitoring</li>
 *   <li>SMS delivery requires verified phone number</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final PasswordService passwordService;
    private final PasswordCookieService passwordCookieService;

    /**
     * Initiates the password reset process by sending a reset code via email or SMS.
     *
     * <p>This endpoint supports two notification types:
     * <ul>
     *   <li>EMAIL: Sends a password reset link via email (traditional method)</li>
     *   <li>SMS: Sends a short verification code via SMS (requires verified phone number)</li>
     * </ul>
     *
     * <p><strong>Security:</strong> Always returns the same generic success message,
     * regardless of whether the email exists in the database or whether SMS delivery
     * requirements are met. This prevents attackers from enumerating valid email addresses
     * or discovering user phone number status.
     *
     * <p><strong>SMS Requirements:</strong> If SMS is selected but the user doesn't have
     * a verified phone number, the system silently fails (for security).
     *
     * <p><strong>Endpoint:</strong> POST /api/password/forgot
     *
     * @param request Contains the user's email address and notification type
     * @param httpRequest HTTP request for logging/auditing purposes
     * @param httpResponse HTTP response for setting cookies (SMS flow)
     * @return Generic success message adapted to the notification type
     */
    @PostMapping("/forgot")
    public ResponseEntity<PasswordOperationResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Password reset requested for email: {} via {}",
                request.email(), request.notificationType());

        // Service handles all business logic and returns SMS result
        SmsPasswordResetResult smsResult = passwordService.requestPasswordReset(request.toBllModel(), httpRequest);

        // Handle SMS-specific cookie management
        if (request.notificationType() == NotificationType.SMS && smsResult.success()) {
            passwordCookieService.setVerificationCookie(httpResponse, smsResult.jwtToken());
            log.debug("SMS verification cookie set for password reset");
        }

        // Return appropriate generic response based on notification type
        return ResponseEntity.ok(
                request.notificationType() == NotificationType.SMS
                        ? PasswordOperationResponse.resetSmsSent()
                        : PasswordOperationResponse.resetEmailSent()
        );
    }

    /**
     * Completes the password reset process using a token from the email.
     *
     * <p>The token is validated by the service layer. If invalid or expired,
     * an exception is thrown and caught by the global exception handler.
     *
     * <p><strong>Endpoint:</strong> PUT /api/password/reset?token=xxx
     *
     * @param token Password reset token from the email link
     * @param request Contains the new password
     * @param httpRequest HTTP request for logging/auditing purposes
     * @return Success message if password was reset
     */
    @PutMapping("/reset")
    public ResponseEntity<PasswordOperationResponse> resetPassword(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("Password reset attempted with token");

        passwordService.resetPassword(request.toBllModel(), token, httpRequest);

        return ResponseEntity.ok(PasswordOperationResponse.passwordReset());
    }

    /**
     * Requests a new password reset token if the previous one expired.
     *
     * <p>This endpoint allows users to generate a new reset link without
     * going through the entire forgot password flow again.
     *
     * <p><strong>Security:</strong> Returns generic message to prevent token validation attacks.
     *
     * <p><strong>Endpoint:</strong> GET /api/password/reset/resend?token=xxx
     *
     * @param token The expired token
     * @param httpRequest HTTP request for logging/auditing purposes
     * @return Generic success message
     */
    @GetMapping("/reset/resend")
    public ResponseEntity<PasswordOperationResponse> resendResetToken(
            @RequestParam String token,
            HttpServletRequest httpRequest) {

        log.info("New password reset token requested");

        passwordService.requestPasswordToken(token, httpRequest);

        return ResponseEntity.ok(PasswordOperationResponse.newTokenSent());
    }

    /**
     * Changes the password for an authenticated user.
     *
     * <p>This endpoint requires authentication. The user must provide their
     * current password for verification before the new password is set.
     *
     * <p><strong>Endpoint:</strong> PUT /api/password/change
     *
     * @param request Contains current password and new password
     * @param httpRequest HTTP request for logging/auditing purposes
     * @return Success message if password was changed
     */
    @PutMapping("/change")
    public ResponseEntity<PasswordOperationResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("Password change requested by authenticated user");

        // Service validates current password and applies business rules
        passwordService.changePassword(request.toBllModel(), httpRequest);

        return ResponseEntity.ok(PasswordOperationResponse.passwordChanged());
    }


    /**
     * Verifies SMS password reset code and grants permission to reset password.
     *
     * <p>Validates the user-provided 6-digit code against the hashed code stored
     * in the JWT token from the verification cookie. If validation succeeds,
     * generates a permission token that allows access to the password reset page.
     *
     * <p><strong>Security:</strong> Code must match exactly. Only anonymous users allowed.
     *
     * <p><strong>Endpoint:</strong> POST /api/password/verify-sms-code
     *
     * @param request Contains the 6-digit verification code
     * @param cookieValue JWT token from the password_reset_sms_token cookie
     * @param httpResponse HTTP response for setting permission cookie
     * @return Success message if code verification succeeds
     */
    @PostMapping("/verify-sms-code")
    public ResponseEntity<PasswordOperationResponse> verifySmsCode(
            @Valid @RequestBody VerifySmsPasswordResetRequest request,
            @CookieValue(name = "password_reset_sms_token") String cookieValue,
            HttpServletResponse httpResponse) {

        log.info("SMS password reset code verification attempted");

        // Extract JWT token from cookie
        String jwtToken = passwordCookieService.getVerificationToken(cookieValue);

        // Verify the SMS code via service
        SmsVerificationResult result = passwordService.verifySmsPasswordResetCode(
                request.toBllModel(jwtToken)
        );

        if (result.success()) {
            // Clear verification cookie and set permission cookie
            passwordCookieService.clearVerificationCookie(httpResponse);
            passwordCookieService.setPasswordResetPermissionCookie(httpResponse, result.resetPermissionToken());

            log.debug("SMS code verified successfully, permission cookie set");
            return ResponseEntity.ok(PasswordOperationResponse.smsCodeVerified());
        } else {
            // Clear verification cookie on failure
            passwordCookieService.clearVerificationCookie(httpResponse);

            log.warn("SMS code verification failed");
            return ResponseEntity.badRequest()
                    .body(PasswordOperationResponse.smsCodeVerificationFailed());
        }
    }


    /**
     * Completes password reset using permission token from SMS verification.
     *
     * <p>This endpoint uses the permission token set after successful SMS code
     * verification. The token grants temporary access to reset the password
     * without needing the original email token.
     *
     * <p><strong>Security:</strong> Permission token expires after 15 minutes.
     * Only anonymous users allowed. All user sessions are invalidated after reset.
     *
     * <p><strong>Endpoint:</strong> PUT /api/password/reset-with-permission
     *
     * @param request Contains permission token, new password and confirmation
     * @return Success message if password was reset
     */
    @PutMapping("/reset-with-permission")
    public ResponseEntity<PasswordOperationResponse> resetPasswordWithPermission(
            @CookieValue(name = "password_reset_permission") String permissionToken,
            @Valid @RequestBody ResetPasswordWithPermissionRequest request) {

        log.info("Password reset with permission token attempted");

        // Service handles all validation and business logic
        // Any exceptions are caught by ControllerAdvisor
        passwordService.resetPasswordWithPermission(request.toBllModel(permissionToken));

        log.info("Password reset with permission completed successfully");
        return ResponseEntity.ok(PasswordOperationResponse.passwordReset());
    }


}
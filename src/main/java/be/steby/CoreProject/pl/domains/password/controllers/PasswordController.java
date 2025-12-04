package be.steby.CoreProject.pl.domains.password.controllers;

import be.steby.CoreProject.bll.domains.password.models.CodePasswordResetResult;
import be.steby.CoreProject.bll.domains.password.models.CodeVerificationResult;
import be.steby.CoreProject.bll.domains.password.services.PasswordService;
import be.steby.CoreProject.bll.domains.password.services.cookies.PasswordCookieService;
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
 * <p>Handles the following flows:
 * <ul>
 *   <li>Password reset request (forgot password) via EMAIL_LINK, EMAIL_CODE, or SMS_CODE</li>
 *   <li>Verification code validation (for EMAIL_CODE and SMS_CODE flows)</li>
 *   <li>Password reset completion with token or permission cookie</li>
 *   <li>Password change for authenticated users</li>
 * </ul>
 *
 * <p><strong>Security considerations:</strong>
 * <ul>
 *   <li>All responses use generic messages to prevent user enumeration</li>
 *   <li>Rate limiting should be configured at infrastructure level</li>
 *   <li>All operations are logged for security monitoring</li>
 *   <li>SMS delivery requires verified phone number</li>
 *   <li>Code-based flows use HTTP-only cookies for permission management</li>
 * </ul>
 *
 * @see be.steby.CoreProject.dl.enums.PasswordResetType
 */
@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final PasswordService passwordService;
    private final PasswordCookieService passwordCookieService;

    // =========================================================================
    // FORGOT PASSWORD - INITIATE RESET
    // =========================================================================

    /**
     * Initiates the password reset process.
     *
     * <p>This endpoint supports three reset types:
     * <ul>
     *   <li>{@code EMAIL_LINK}: Sends email with clickable reset link</li>
     *   <li>{@code EMAIL_CODE}: Sends email with 6-digit verification code</li>
     *   <li>{@code SMS_CODE}: Sends SMS with 6-digit verification code</li>
     * </ul>
     *
     * <p><strong>Security:</strong> Always returns the same generic success message,
     * regardless of whether the email exists in the database or whether delivery
     * requirements are met. This prevents attackers from enumerating valid email
     * addresses or discovering user phone number status.
     *
     * <p><strong>Code-based flows:</strong> For {@code EMAIL_CODE} and {@code SMS_CODE},
     * a verification cookie is set to enable the subsequent code verification step.
     *
     * <p><strong>Endpoint:</strong> POST /api/password/forgot
     *
     * @param request Contains the user's email address and reset type
     * @param httpRequest HTTP request for logging/auditing purposes
     * @param httpResponse HTTP response for setting cookies (code-based flows)
     * @return Generic success message adapted to the reset type
     */
    @PostMapping("/forgot")
    public ResponseEntity<PasswordOperationResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.info("Password reset requested for email: {} via {}",
                request.email(), request.resetType());

        // Service handles all business logic and returns result
        CodePasswordResetResult result = passwordService.requestPasswordReset(
                request.toBllModel(),
                httpRequest
        );

        // Set verification cookie for code-based flows
        if (request.resetType().requiresCodeVerification() && result.success()) {
            passwordCookieService.setVerificationCookie(httpResponse, result.jwtToken());
            log.debug("Verification cookie set for {} password reset", request.resetType());
        }

        // Return appropriate generic response based on reset type
        return ResponseEntity.ok(PasswordOperationResponse.forResetType(request.resetType()));
    }

    // =========================================================================
    // CODE VERIFICATION (EMAIL_CODE and SMS_CODE flows)
    // =========================================================================

    /**
     * Verifies the password reset code and grants permission to reset password.
     *
     * <p>This endpoint handles verification for both {@code EMAIL_CODE} and {@code SMS_CODE}
     * reset flows. It validates the user-provided 6-digit code against the hashed code
     * stored in the JWT token from the verification cookie.
     *
     * <p>If validation succeeds, the verification cookie is cleared and a permission
     * cookie is set, granting temporary access to the password reset page.
     *
     * <p><strong>Security:</strong>
     * <ul>
     *   <li>Code must match exactly</li>
     *   <li>Only anonymous users allowed</li>
     *   <li>Verification cookie is always cleared (success or failure)</li>
     *   <li>Permission cookie expires after 15 minutes</li>
     * </ul>
     *
     * <p><strong>Endpoint:</strong> POST /api/password/verify-code
     *
     * @param request Contains the 6-digit verification code
     * @param cookieValue JWT token from the verification cookie
     * @param httpResponse HTTP response for managing cookies
     * @return Success message if code verification succeeds, error otherwise
     */
    @PostMapping("/verify-code")
    public ResponseEntity<PasswordOperationResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request,
            @CookieValue(name = "password_reset_sms_token") String cookieValue,
            HttpServletResponse httpResponse) {

        log.info("Password reset code verification attempted");

        // Extract JWT token from cookie
        String jwtToken = passwordCookieService.getVerificationToken(cookieValue);

        // Verify the code via service
        CodeVerificationResult result = passwordService.verifyPasswordResetCode(
                request.toBllModel(jwtToken)
        );

        if (result.success()) {
            // Clear verification cookie and set permission cookie
            passwordCookieService.clearVerificationCookie(httpResponse);
            passwordCookieService.setPasswordResetPermissionCookie(
                    httpResponse,
                    result.resetPermissionToken()
            );

            log.debug("Code verified successfully, permission cookie set");
            return ResponseEntity.ok(PasswordOperationResponse.codeVerified());
        } else {
            // Clear verification cookie on failure
            passwordCookieService.clearVerificationCookie(httpResponse);

            log.warn("Code verification failed");
            return ResponseEntity.badRequest()
                    .body(PasswordOperationResponse.codeVerificationFailed());
        }
    }

    // =========================================================================
    // RESET PASSWORD - COMPLETE RESET
    // =========================================================================

    /**
     * Completes password reset using a token from the email link.
     *
     * <p>This endpoint is used for {@code EMAIL_LINK} reset flow. The token
     * is embedded in the URL that the user clicked in their email.
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
     * Completes password reset using permission cookie from code verification.
     *
     * <p>This endpoint is used for {@code EMAIL_CODE} and {@code SMS_CODE} reset flows,
     * after the user has successfully verified their code via {@link #verifyCode}.
     *
     * <p>The permission cookie grants temporary access to reset the password
     * without needing the original email token.
     *
     * <p><strong>Security:</strong>
     * <ul>
     *   <li>Permission token expires after 15 minutes</li>
     *   <li>Only anonymous users allowed</li>
     *   <li>All user sessions are invalidated after reset</li>
     * </ul>
     *
     * <p><strong>Endpoint:</strong> PUT /api/password/reset-with-permission
     *
     * @param permissionToken JWT token from the permission cookie
     * @param request Contains the new password
     * @return Success message if password was reset
     */
    @PutMapping("/reset-with-permission")
    public ResponseEntity<PasswordOperationResponse> resetPasswordWithPermission(
            @CookieValue(name = "password_reset_permission") String permissionToken,
            @Valid @RequestBody ResetPasswordWithPermissionRequest request) {

        log.info("Password reset with permission token attempted");

        passwordService.resetPasswordWithPermission(request.toBllModel(permissionToken));

        log.info("Password reset with permission completed successfully");
        return ResponseEntity.ok(PasswordOperationResponse.passwordReset());
    }

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

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

    // =========================================================================
    // CHANGE PASSWORD (AUTHENTICATED)
    // =========================================================================

    /**
     * Changes the password for an authenticated user.
     *
     * <p>This endpoint requires authentication. The user must provide their
     * current password for verification before the new password is set.
     *
     * <p>After successful password change, all active sessions except the
     * current one are invalidated for security.
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

        passwordService.changePassword(request.toBllModel(), httpRequest);

        return ResponseEntity.ok(PasswordOperationResponse.passwordChanged());
    }
}
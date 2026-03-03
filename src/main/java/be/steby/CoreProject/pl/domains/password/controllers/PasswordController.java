package be.steby.CoreProject.pl.domains.password.controllers;

import be.steby.CoreProject.bll.domains.password.models.CodePasswordResetResult;
import be.steby.CoreProject.bll.domains.password.models.CodeVerificationResult;
import be.steby.CoreProject.bll.domains.password.services.EmailCodePasswordResetService;
import be.steby.CoreProject.bll.domains.password.services.EmailLinkPasswordResetService;
import be.steby.CoreProject.bll.domains.password.services.PasswordService;
import be.steby.CoreProject.bll.domains.password.services.SmsPasswordResetService;
import be.steby.CoreProject.bll.domains.password.services.cookies.PasswordCookieService;
import be.steby.CoreProject.pl.domains.password.models.requests.*;
import be.steby.CoreProject.pl.domains.password.models.responses.PasswordOperationResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for password management operations.
 *
 * <p>Endpoints:
 * <pre>
 *   POST /api/password/forgot/email-link     → Reset via email link
 *   POST /api/password/forgot/email-code     → Reset via email code
 *   POST /api/password/forgot/sms-code       → Reset via SMS code
 *   POST /api/password/verify-code           → Verify code (EMAIL_CODE + SMS_CODE)
 *   PUT  /api/password/reset                 → Complete reset via token (EMAIL_LINK)
 *   PUT  /api/password/reset-with-permission → Complete reset via permission cookie
 *   GET  /api/password/reset/resend          → Resend expired EMAIL_LINK token
 *   PUT  /api/password/change                → Change password (authenticated)
 *   PUT  /api/password/define                → Define password (OAuth users)
 * </pre>
 */
@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final EmailLinkPasswordResetService emailLinkPasswordResetService;
    private final EmailCodePasswordResetService emailCodePasswordResetService;
    private final SmsPasswordResetService smsPasswordResetService;
    private final PasswordService passwordService;
    private final PasswordCookieService passwordCookieService;

    // =========================================================================
    // FORGOT PASSWORD - INITIATE RESET
    // =========================================================================

    /**
     * Initiates password reset via email link.
     * Sends an email containing a clickable reset link.
     */
    @PostMapping("/forgot/email-link")
    public ResponseEntity<PasswordOperationResponse> forgotPasswordEmailLink(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("EMAIL_LINK password reset requested");

        emailLinkPasswordResetService.requestReset(request.toBllModel());

        return ResponseEntity.ok(PasswordOperationResponse.resetEmailLinkSent());
    }

    /**
     * Initiates password reset via email code.
     * Sends a 6-digit code by email and sets a verification cookie.
     */
    @PostMapping("/forgot/email-code")
    public ResponseEntity<PasswordOperationResponse> forgotPasswordEmailCode(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletResponse httpResponse) {

        log.info("EMAIL_CODE password reset requested");

        CodePasswordResetResult result = emailCodePasswordResetService.requestReset(request.toBllModel());

        if (result.success()) {
            passwordCookieService.setVerificationCookie(httpResponse, result.jwtToken());
        }

        return ResponseEntity.ok(PasswordOperationResponse.resetEmailCodeSent());
    }

    /**
     * Initiates password reset via SMS code.
     * Sends a 6-digit code by SMS and sets a verification cookie.
     * Requires a verified phone number on the account.
     */
    @PostMapping("/forgot/sms-code")
    public ResponseEntity<PasswordOperationResponse> forgotPasswordSmsCode(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletResponse httpResponse) {

        log.info("SMS_CODE password reset requested");

        CodePasswordResetResult result = smsPasswordResetService.requestReset(request.toBllModel());

        if (result.success()) {
            passwordCookieService.setVerificationCookie(httpResponse, result.jwtToken());
        }

        return ResponseEntity.ok(PasswordOperationResponse.resetSmsCodeSent(result.phoneHint()));
    }

    // =========================================================================
    // CODE VERIFICATION (EMAIL_CODE and SMS_CODE)
    // =========================================================================

    /**
     * Verifies the 6-digit code for EMAIL_CODE and SMS_CODE flows.
     * On success: clears verification cookie, sets permission cookie.
     * On failure: clears verification cookie.
     */
    @PostMapping("/verify-code")
    public ResponseEntity<PasswordOperationResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request,
            @CookieValue(name = "password_reset_sms_token") String cookieValue,
            HttpServletResponse httpResponse) {

        log.info("Password reset code verification attempted");

        String jwtToken = passwordCookieService.getVerificationToken(cookieValue);

        CodeVerificationResult result = passwordService.verifyPasswordResetCode(
                request.toBllModel(jwtToken)
        );

        if (result.success()) {
            passwordCookieService.clearVerificationCookie(httpResponse);
            passwordCookieService.setPasswordResetPermissionCookie(
                    httpResponse,
                    result.resetPermissionToken()
            );
            return ResponseEntity.ok(PasswordOperationResponse.codeVerified());
        } else {
            passwordCookieService.clearVerificationCookie(httpResponse);
            return ResponseEntity.badRequest()
                    .body(PasswordOperationResponse.codeVerificationFailed());
        }
    }

    // =========================================================================
    // RESET PASSWORD - COMPLETE RESET
    // =========================================================================

    /**
     * Completes the password reset using a token from the email link (EMAIL_LINK flow).
     */
    @PutMapping("/reset")
    public ResponseEntity<PasswordOperationResponse> resetPassword(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("Password reset attempted with token");

        passwordService.resetPassword(request.toBllModel(), token);

        return ResponseEntity.ok(PasswordOperationResponse.passwordReset());
    }

    /**
     * Completes the password reset using the permission cookie (EMAIL_CODE and SMS_CODE flows).
     */
    @PutMapping("/reset-with-permission")
    public ResponseEntity<PasswordOperationResponse> resetPasswordWithPermission(
            @CookieValue(name = "password_reset_permission") String permissionToken,
            @Valid @RequestBody ResetPasswordWithPermissionRequest request) {

        log.info("Password reset with permission token attempted");

        passwordService.resetPasswordWithPermission(request.toBllModel(permissionToken));

        return ResponseEntity.ok(PasswordOperationResponse.passwordReset());
    }

    // =========================================================================
    // TOKEN MANAGEMENT
    // =========================================================================

    /**
     * Requests a new EMAIL_LINK reset token when the previous one has expired.
     */
    @GetMapping("/reset/resend")
    public ResponseEntity<PasswordOperationResponse> resendResetToken(
            @RequestParam String token) {

        log.info("New password reset token requested");

        passwordService.requestPasswordToken(token);

        return ResponseEntity.ok(PasswordOperationResponse.newTokenSent());
    }

    // =========================================================================
    // CHANGE PASSWORD (AUTHENTICATED)
    // =========================================================================

    /**
     * Changes the password for an authenticated user.
     * Requires the current password for verification.
     * Invalidates all other active sessions on success.
     */
    @PutMapping("/change")
    public ResponseEntity<PasswordOperationResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        log.info("Password change requested by authenticated user");

        passwordService.changePassword(request.toBllModel());

        return ResponseEntity.ok(PasswordOperationResponse.passwordChanged());
    }

    // =========================================================================
    // DEFINE PASSWORD (OAUTH USERS)
    // =========================================================================

    /**
     * Defines a first password for OAuth users (Google, GitHub, Microsoft...).
     * Does not require a current password since OAuth users don't have one yet.
     */
    @PutMapping("/define")
    public ResponseEntity<PasswordOperationResponse> definePassword(
            @Valid @RequestBody DefinePasswordRequest request) {

        log.info("Password definition requested by OAuth user");

        passwordService.definePassword(request.password());

        return ResponseEntity.ok(PasswordOperationResponse.passwordDefined());
    }
}
package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.exceptions.MaxAttemptsReachedException;
import be.steby.CoreProject.bll.common.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.bll.domains.password.events.sms.PasswordResetSmsRequestedEvent;
import be.steby.CoreProject.bll.domains.password.exceptions.PasswordRequestValidationException;
import be.steby.CoreProject.bll.domains.password.models.CodePasswordResetResult;
import be.steby.CoreProject.bll.domains.password.models.ForgotPasswordBLLRequest;
import be.steby.CoreProject.bll.domains.password.services.tokens.verification_code.VerificationCodeTokenService;
import be.steby.CoreProject.bll.domains.password.services.jwt.PasswordResetJwtService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Handles password reset via SMS verification code (SMS_CODE flow).
 *
 * <p>Flow:
 * <ol>
 *   <li>User submits their email → {@link #requestReset(ForgotPasswordBLLRequest)}</li>
 *   <li>A 6-digit code is generated and sent via SMS</li>
 *   <li>A JWT reference token is returned for cookie storage</li>
 *   <li>User enters the code → handled by {@link VerificationCodeTokenService}</li>
 *   <li>On success, a permission token is issued for the reset form</li>
 * </ol>
 *
 * <p>Requirements: user must have a verified phone number.
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsPasswordResetService {

    private final UserService userService;
    private final VerificationCodeTokenService verificationCodeTokenService;
    private final PasswordResetJwtService passwordResetJwtService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${url.front_server}")
    private String frontUrl;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Initiates a password reset via SMS code.
     *
     * <p>Always returns a result without leaking whether the email exists
     * or whether the user has a verified phone number (security).
     *
     * @param request the forgot password request
     * @return success with JWT reference token, or silent failure
     */
    public CodePasswordResetResult requestReset(ForgotPasswordBLLRequest request) {
        log.debug("Processing SMS_CODE password reset request for email: {}", request.email());

        validateRequest(request);
        checkIsAnonymous();

        try {
            User user = userService.getUserByEmail(request.email());

            if (!canReceiveSms(user)) {
                log.debug("User {} cannot receive SMS - requirements not met", user.getUsername());
                return CodePasswordResetResult.failure();
            }

            VerificationCodeTokenService.CodeGenerationResult result =
                    verificationCodeTokenService.createVerificationCodeToken(user);

            String jwtReferenceToken = passwordResetJwtService.generateCodeReferenceToken(
                    result.token().getToken()
            );

            eventPublisher.publishEvent(
                    new PasswordResetSmsRequestedEvent(user, result.plainVerificationCode())
            );

            String phoneHint = maskPhoneNumber(user.getPhoneNumber());

            log.info("SMS_CODE password reset initiated successfully for user: {}", user.getUsername());
            return CodePasswordResetResult.success(jwtReferenceToken, phoneHint);

        } catch (MaxAttemptsReachedException e) {
            log.warn("SMS_CODE password reset failed - rate limit exceeded: {}", e.getMessage());
            return CodePasswordResetResult.failure();
        } catch (Exception e) {
            log.debug("SMS_CODE password reset silently handled - reason: {}", e.getMessage());
            return CodePasswordResetResult.failure();
        }
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    private void validateRequest(ForgotPasswordBLLRequest request) {
        if (request == null) {
            throw new PasswordRequestValidationException("Password reset request cannot be null");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new PasswordRequestValidationException("Email address cannot be null or blank");
        }
        String normalizedEmail = request.email().toLowerCase().trim();
        if (!request.email().equals(normalizedEmail)) {
            throw new PasswordRequestValidationException("Email must be normalized (lowercase and trimmed)");
        }
    }

    private void checkIsAnonymous() {
        if (!userService.isAnonymous()) {
            throw new UserAuthenticationStateException(
                    "You are logged in. Please use the change password feature instead: "
                    + frontUrl + "/password/change", 403
            );
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 6) {
            return "***";
        }
        return phoneNumber.substring(0, 3) + " *** *** " + phoneNumber.substring(phoneNumber.length() - 2);
    }

    private boolean canReceiveSms(User user) {
        return user.getPhoneNumber() != null
                && !user.getPhoneNumber().isBlank()
                && user.isPhoneNumberVerified();
    }
}
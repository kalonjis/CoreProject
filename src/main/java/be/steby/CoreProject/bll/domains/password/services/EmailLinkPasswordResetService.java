package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.common.exceptions.UserAuthenticationStateException;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.domains.password.exceptions.PasswordRequestValidationException;
import be.steby.CoreProject.bll.domains.password.models.ForgotPasswordBLLRequest;
import be.steby.CoreProject.bll.domains.password.services.tokens.email.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Handles password reset via email link (EMAIL_LINK flow).
 *
 * <p>Flow:
 * <ol>
 *   <li>User submits their email → {@link #requestReset(ForgotPasswordBLLRequest)}</li>
 *   <li>A long-lived token is generated and stored in DB</li>
 *   <li>An email is sent with a clickable reset link containing the token</li>
 *   <li>User clicks the link → handled by {@code PasswordService.resetPassword()}</li>
 * </ol>
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailLinkPasswordResetService {

    private final UserService userService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${url.front_server}")
    private String frontUrl;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Initiates a password reset via email link.
     *
     * <p>Always returns silently without leaking whether the email exists (security).
     *
     * @param request the forgot password request
     */
    public void requestReset(ForgotPasswordBLLRequest request) {
        log.debug("Processing EMAIL_LINK password reset request for email: {}", request.email());

        validateRequest(request);
        checkIsAnonymous();

        try {
            User user = userService.getUserByEmail(request.email());

            PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(user);
            Device device = deviceService.detectAndRegisterDevice(user);

            eventPublisher.publishEvent(new RequestPasswordResetEvent(user, token.getPublicId(), device));

            log.info("EMAIL_LINK password reset initiated successfully for user: {}", user.getUsername());

        } catch (Exception e) {
            log.debug("EMAIL_LINK password reset silently handled - reason: {}", e.getMessage());
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
}
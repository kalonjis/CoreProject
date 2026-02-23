package be.steby.CoreProject.bll.domains.emailaddress.services;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.domains.emailaddress.events.*;
import be.steby.CoreProject.bll.domains.emailaddress.exceptions.InvalidEmailException;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailChangeRequest;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.common.exceptions.TokenConfirmationStatusException;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.emailaddress.services.tokens.EmailConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.EmailConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailAddressServiceImpl implements EmailAddressService {

    private final UserService userService;
    private final EmailConfirmationTokenServiceImpl emailConfirmationTokenService;
    private final EmailPolicyService emailPolicyService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void changeEmailRequest(EmailChangeRequest request, HttpServletRequest httpRequest) {
        User user = userService.getAuthenticatedUser();

        // ===== DEFENSIVE VALIDATION (Defense in Depth) =====
        validateEmailChangeRequestSecurely(request, user);

        // Business logic: Create token for old email verification
        EmailConfirmationToken token = emailConfirmationTokenService.createEmailConfirmationToken(user);
        token.setNewEmailAddress(request.newEmail());
        emailConfirmationTokenService.saveToken(token);

        // Publish event to send verification email to OLD address
        eventPublisher.publishEvent(
                new EmailChangeRequestEvent(user, request.newEmail(), token.getPublicId())
        );

        log.info("Email change request initiated for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void cancelEmailChange(String token, HttpServletRequest httpRequest) {
        EmailConfirmationToken emailToken = emailConfirmationTokenService
                .getSecureTokenByType(token, TokenType.EMAIL_CONFIRMATION);

        if (!emailToken.isRevoked()) {
            emailConfirmationTokenService.revokeToken(emailToken);
        }

        User user = emailToken.getUser();

        // Publish event to notify user
        eventPublisher.publishEvent(
                new EmailChangeCancellationEvent(user, emailToken.getNewEmailAddress())
        );

        log.info("Email change cancelled for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void changeEmailVerification(String token, HttpServletRequest httpRequest) {
        EmailConfirmationToken emailToken = emailConfirmationTokenService
                .getSecureValidToken(token, TokenType.EMAIL_CONFIRMATION);

        if (emailToken.isConfirmed()) {
            throw new TokenConfirmationStatusException(
                    "The request is already confirmed, please check " +
                            emailToken.getNewEmailAddress() + " mail box for the next step"
            );
        }

        User user = emailToken.getUser();
        String newEmail = emailToken.getNewEmailAddress();

        // Mark token as confirmed (user verified from OLD email)
        emailToken.setConfirmed(true);
        emailConfirmationTokenService.saveToken(emailToken);

        // Publish event to send confirmation email to NEW address
        eventPublisher.publishEvent(
                new EmailChangeVerificationEvent(user, emailToken.getPublicId(), newEmail)
        );

        log.info("Email change verification completed for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void confirmEmail(String token, HttpServletRequest httpRequest) {
        EmailConfirmationToken emailToken = emailConfirmationTokenService
                .getSecureValidToken(token, TokenType.EMAIL_CONFIRMATION);

        emailConfirmationTokenService.verifyTokenValidity(emailToken);

        User user = emailToken.getUser();
        String oldEmail = user.getEmail();
        String newEmail = emailToken.getNewEmailAddress();

        // ===== FINAL DEFENSIVE VALIDATION =====
        // In case email became invalid between request and confirmation
        EmailValidationResult result = emailPolicyService.validateEmail(newEmail);
        if (!result.isValid()) {
            log.warn("Email became invalid during confirmation: {} for user: {}",
                    newEmail, user.getUsername());
            throw new InvalidEmailException(
                    "Email no longer valid: " + String.join(", ", result.errors())
            );
        }

        // Update user email
        user.setEmail(newEmail);
        userService.saveUser(user);

        // Publish event to send confirmation to BOTH addresses
        eventPublisher.publishEvent(
                new EmailChangeConfirmationEvent(user, token, oldEmail, newEmail)
        );

        // Revoke token after successful confirmation
        emailConfirmationTokenService.revokeToken(emailToken);

        log.info("Email change confirmed for user: {} - New email: {}", user.getUsername(), newEmail);
    }

    /**
     * Defensive validation at business layer level (Defense in Depth).
     * Validates the email change request even if PL validation passes.
     *
     * @param request Email change request to validate
     * @param user Current authenticated user
     * @throws InvalidEmailException if validation fails
     */
    private void validateEmailChangeRequestSecurely(EmailChangeRequest request, User user) {
        log.debug("Starting defensive validation for email change request");

        // 1. Null safety (should never happen, but defense in depth)
        if (request == null) {
            throw new IllegalArgumentException("Email change request cannot be null");
        }
        if (request.newEmail() == null || request.newEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (request.confirmEmail() == null || request.confirmEmail().isBlank()) {
            throw new IllegalArgumentException("Confirm email cannot be null or blank");
        }

        // 2. Consistency validation
        if (!request.newEmail().equals(request.confirmEmail())) {
            throw new IllegalArgumentException("Email and confirm email must match");
        }

        // 3. Critical business validation (reuses same service as PL)
        EmailValidationResult result = emailPolicyService.validateEmail(request.newEmail());
        if (!result.isValid()) {
            log.warn("Business validation failed for email: {} - errors: {}",
                    request.newEmail(), result.errors());
            throw new InvalidEmailException(
                    "Email validation failed: " + String.join(", ", result.errors())
            );
        }

        // 4. User context-specific validation
        if (request.newEmail().equalsIgnoreCase(user.getEmail())) {
            throw new InvalidEmailException("New email must be different from current email");
        }

        log.debug("Defensive validation successful for email: {}", request.newEmail());
    }
}
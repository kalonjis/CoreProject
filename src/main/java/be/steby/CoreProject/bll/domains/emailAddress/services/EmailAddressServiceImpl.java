package be.steby.CoreProject.bll.domains.emailAddress.services;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.domains.emailAddress.events.*;
import be.steby.CoreProject.bll.domains.emailAddress.exceptions.InvalidEmailException;
import be.steby.CoreProject.bll.domains.emailAddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.exceptions.TokenConfirmationStatusException;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.emailAddress.services.tokens.EmailConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.EmailConfirmationToken;
import be.steby.CoreProject.pl.models.emailAddress.ChangeEmailForm;
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
    private final EmailPolicyService emailPolicyService; // ← Pour validation défensive
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void changeEmailRequest(ChangeEmailForm form, HttpServletRequest request) {
        User user = userService.getAuthenticatedUser();

        // ===== VALIDATION DÉFENSIVE (Defense in Depth) =====
        validateEmailChangeRequestSecurely(form, user);

        // Logique métier sécurisée
        EmailConfirmationToken token = emailConfirmationTokenService.createEmailConfirmationToken(user);
        token.setNewEmailAddress(form.email());
        emailConfirmationTokenService.saveToken(token);


        eventPublisher.publishEvent(
                new EmailChangeRequestEvent(
                    user, form.email(), token.getToken()
                    )
        );
    }

    @Override
    public void cancelEmailChange(String token, HttpServletRequest request) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        if (!emailConfirmationToken.isRevoked()) {
            emailConfirmationTokenService.revokeToken(emailConfirmationToken);
        }

        eventPublisher.publishEvent(new EmailChangeCancellationEvent(
                emailConfirmationToken.getUser(),
                emailConfirmationToken.getNewEmailAddress()
        ));
    }

    @Override
    public void changeEmailVerification(String token, HttpServletRequest request) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        emailConfirmationTokenService.verifyTokenValidity(emailConfirmationToken);

        if (emailConfirmationToken.isConfirmed()) {
            throw new TokenConfirmationStatusException(
                    "The request is already confirmed, please check " +
                            emailConfirmationToken.getNewEmailAddress() + " mail box for the next step"
            );
        }

        emailConfirmationToken.setConfirmed(true);
        emailConfirmationTokenService.saveToken(emailConfirmationToken);

        eventPublisher.publishEvent(new EmailChangeVerificationEvent(
                emailConfirmationToken.getUser(),
                emailConfirmationToken.getToken(),
                emailConfirmationToken.getNewEmailAddress()
        ));
    }

    @Transactional
    @Override
    public void confirmEmail(String token, HttpServletRequest request) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        emailConfirmationTokenService.verifyTokenValidity(emailConfirmationToken);

        User user = emailConfirmationToken.getUser();
        String oldEmail = user.getEmail();
        String newEmail = emailConfirmationToken.getNewEmailAddress();

        // ===== VALIDATION DÉFENSIVE FINALE =====
        // Au cas où l'email serait devenu invalide entre temps
        EmailValidationResult result = emailPolicyService.validateEmail(newEmail);
        if (!result.isValid()) {
            log.warn("Email devenu invalide lors de la confirmation: {} pour user {}",
                    newEmail, user.getUsername());
            throw new InvalidEmailException(
                    "Email no longer valid: " + String.join(", ", result.errors())
            );
        }

        user.setEmail(newEmail);
        userService.saveUser(user);


        eventPublisher.publishEvent(
                new EmailChangeConfirmationEvent(
                    user, token, oldEmail, newEmail)
        );

        emailConfirmationTokenService.revokeToken(emailConfirmationToken);
    }

    /**
     * Validation défensive pour sécuriser le service.
     * Protège contre le contournement de la validation PL.
     *
     * @param form Le formulaire à valider
     * @param user L'utilisateur authentifié
     * @throws IllegalArgumentException Pour les erreurs de structure
     * @throws InvalidEmailException Pour les erreurs métier
     */
    private void validateEmailChangeRequestSecurely(ChangeEmailForm form, User user) {
        log.debug("Validation défensive pour changement email - user: {}", user.getUsername());

        // 1. Validation structurelle (protection contre null/vide)
        if (form == null) {
            throw new IllegalArgumentException("Email change form cannot be null");
        }
        if (form.email() == null || form.email().isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        if (form.confirmEmail() == null || form.confirmEmail().isBlank()) {
            throw new IllegalArgumentException("Confirm email cannot be null or blank");
        }

        // 2. Validation de cohérence
        if (!form.email().equals(form.confirmEmail())) {
            throw new IllegalArgumentException("Email and confirm email must match");
        }

        // 3. Validation métier critique (réutilise le même service que la PL)
        EmailValidationResult result = emailPolicyService.validateEmail(form.email());
        if (!result.isValid()) {
            log.warn("Validation métier échouée pour email: {} - errors: {}",
                    form.email(), result.errors());
            throw new InvalidEmailException(
                    "Email validation failed: " + String.join(", ", result.errors())
            );
        }

        // 4. Validation spécifique au contexte utilisateur
        if (form.email().equalsIgnoreCase(user.getEmail())) {
            throw new InvalidEmailException("New email must be different from current email");
        }

        log.debug("Validation défensive réussie pour email: {}", form.email());
    }
}
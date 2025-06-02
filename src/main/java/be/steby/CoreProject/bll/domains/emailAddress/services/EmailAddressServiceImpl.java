package be.steby.CoreProject.bll.domains.emailAddress.services;

import be.steby.CoreProject.bll.domains.emailAddress.events.EmailChangeCancellationEvent;
import be.steby.CoreProject.bll.domains.emailAddress.events.EmailChangeConfirmationEvent;
import be.steby.CoreProject.bll.domains.emailAddress.events.EmailChangeRequestEvent;
import be.steby.CoreProject.bll.domains.emailAddress.events.EmailChangeVerificationEvent;
import be.steby.CoreProject.bll.domains.emailAddress.models.EmailChangeRequest;
import be.steby.CoreProject.bll.domains.emailAddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.exceptions.AlreadyExistException;
import be.steby.CoreProject.bll.domains.emailAddress.exceptions.InvalidEmailException;
import be.steby.CoreProject.bll.exceptions.TokenConfirmationStatusException;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.emailAddress.services.tokens.EmailConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.EmailConfirmationToken;
import be.steby.CoreProject.pl.models.user.ChangeEmailForm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
public class EmailAddressServiceImpl implements EmailAddressService {

    private final UserService userService;
    private final EmailConfirmationTokenServiceImpl emailConfirmationTokenService;
    private final EmailPolicyService emailPolicyService;
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void changeEmailRequest(ChangeEmailForm form, HttpServletRequest request) {
        User user = userService.getAuthenticatedUser();

        // Utiliser le nouveau modèle pour valider les données
        EmailChangeRequest emailChangeRequest = EmailChangeRequest.fromForm(form);

        // Valider l'email avec le service de politique
        EmailValidationResult validationResult = emailPolicyService.validateEmail(emailChangeRequest.newEmail());
        if (!validationResult.isValid()) {
            throw new InvalidEmailException("Email invalide: " + String.join(", ", validationResult.errors()));
        }

        EmailConfirmationToken token = emailConfirmationTokenService.createEmailConfirmationToken(user);
        token.setNewEmailAddress(emailChangeRequest.newEmail());
        emailConfirmationTokenService.saveToken(token);

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new EmailChangeRequestEvent(
                user,
                emailChangeRequest.newEmail(),
                token.getToken(),
                requestContext
        ));
    }

    @Override
    public void cancelEmailChange(String token, HttpServletRequest request) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        if (!emailConfirmationToken.isRevoked()) {
            emailConfirmationTokenService.revokeToken(emailConfirmationToken);
        }

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new EmailChangeCancellationEvent(
                emailConfirmationToken.getUser(),
                emailConfirmationToken.getNewEmailAddress(),
                requestContext
        ));
    }

    @Override
    public void changeEmailVerification(String token, HttpServletRequest request) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        emailConfirmationTokenService.verifyTokenValidity(emailConfirmationToken);

        // Re-valider l'email au moment de la vérification
        EmailValidationResult validationResult = emailPolicyService.validateEmail(emailConfirmationToken.getNewEmailAddress());
        if (!validationResult.isValid()) {
            throw new InvalidEmailException("Email invalide: " + String.join(", ", validationResult.errors()));
        }

        if (emailConfirmationToken.isConfirmed()) {
            throw new TokenConfirmationStatusException(
                    "The request is already confirmed, please check " +
                            emailConfirmationToken.getNewEmailAddress() + " mail box for the next step"
            );
        }

        emailConfirmationToken.setConfirmed(true);
        emailConfirmationTokenService.saveToken(emailConfirmationToken);

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new EmailChangeVerificationEvent(
                emailConfirmationToken.getUser(),
                requestContext,
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

        // Validation finale avant confirmation
        EmailValidationResult validationResult = emailPolicyService.validateEmail(newEmail);
        if (!validationResult.isValid()) {
            throw new InvalidEmailException("Email invalide: " + String.join(", ", validationResult.errors()));
        }

        user.setEmail(newEmail);
        userService.saveUser(user);

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new EmailChangeConfirmationEvent(
                user,
                token,
                oldEmail,
                newEmail,
                requestContext
        ));

        emailConfirmationTokenService.revokeToken(emailConfirmationToken);
    }
}

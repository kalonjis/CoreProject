package be.steby.CoreProject.bll.domain.emailAddress.services;

import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeCancellationEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeConfirmationEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeRequestEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeVerificationEvent;
import be.steby.CoreProject.bll.exceptions.AlreadyExistException;
import be.steby.CoreProject.bll.exceptions.InvalidEmailException;
import be.steby.CoreProject.bll.exceptions.TokenConfirmationStatusException;
import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.RequestContextService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.impl.EmailConfirmationTokenServiceImpl;
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
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void changeEmailRequest(ChangeEmailForm form, HttpServletRequest request ) {
        User user = userService.getAuthenticatedUser();
        String email = form.email();
        checkEmailValidity(email);
        if( !email.equals( form.confirmEmail() ) ){
            throw new InvalidEmailException("Les adresses email doivent être identiques");
        }
        checkEmailAvailability(email);

        EmailConfirmationToken token = emailConfirmationTokenService.createEmailConfirmationToken(user);
        token.setNewEmailAddress(email);
        emailConfirmationTokenService.saveToken(token);

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new EmailChangeRequestEvent(user, email, token.getToken(), requestContext));

    }


    @Override
    public void cancelEmailChange(String token, HttpServletRequest request) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        if (!emailConfirmationToken.isRevoked()){
            emailConfirmationTokenService.revokeToken(emailConfirmationToken);
        }

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new EmailChangeCancellationEvent(
                emailConfirmationToken.getUser(), emailConfirmationToken.getNewEmailAddress(), requestContext));

    }

    @Override
    public void changeEmailVerification(String token, HttpServletRequest request) {

        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        emailConfirmationTokenService.verifyTokenValidity(emailConfirmationToken);
        checkEmailAvailability(emailConfirmationToken.getNewEmailAddress());

        if( emailConfirmationToken.isConfirmed() ){
            throw new TokenConfirmationStatusException("The request is already confirmed, please check " + emailConfirmationToken.getNewEmailAddress() + " mail box for the next step");
        }

        emailConfirmationToken.setConfirmed(true);
        emailConfirmationTokenService.saveToken(emailConfirmationToken);

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent(new EmailChangeVerificationEvent(emailConfirmationToken.getUser(), requestContext, emailConfirmationToken.getToken(), emailConfirmationToken.getNewEmailAddress() ) );


    }



    @Transactional
    @Override
    public void confirmEmail(String token, HttpServletRequest request) {

        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        emailConfirmationTokenService.verifyTokenValidity(emailConfirmationToken);

        User user = emailConfirmationToken.getUser();
        String oldEmail = user.getEmail();
        String newEmail = emailConfirmationToken.getNewEmailAddress();

        checkEmailAvailability(newEmail);

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


    private void checkEmailValidity(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if(!Pattern.compile(emailRegex).matcher(email).matches()){
            throw new InvalidEmailException("Email address " + email + " is not in a valid format");
        }
    }


    private void checkEmailAvailability(String email){
        if( userService.existsByEmail(email) ) {
            throw new AlreadyExistException("The email address " + email + " is already used by another user");
        }

    }


}

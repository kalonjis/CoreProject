package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.events.account.ConfirmNewUserAccountEvent;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.bll.events.security.email_events.ChangeEmailRequestEvent;
import be.steby.CoreProject.bll.events.security.email_events.ChangeEmailVerificationEvent;
import be.steby.CoreProject.bll.events.security.password_events.PasswordChangedEvent;
import be.steby.CoreProject.bll.events.security.password_events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.exceptions.*;
import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.RequestContextService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.AuthService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.EmailConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.pl.models.user.ChangeEmailForm;
import be.steby.CoreProject.pl.security.models.ChangePasswordForm;
import be.steby.CoreProject.pl.security.models.PasswordResetForm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final MailerService mailerService;
    private final DeviceService deviceService;
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final AccountConfirmationAttemptServiceImpl accountConfirmationAttemptService;
    private final EmailConfirmationTokenServiceImpl emailConfirmationTokenService;


    @Value("${url.front_server}")
    private String FRONT_URL;



    @Override
    public User signup(User user, HttpServletRequest request) {
        userService.checkIfUserExists(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.saveUser(user);

        RequestContext requestContext = requestContextService.captureRequestContext(request);
        eventPublisher.publishEvent( new SignupEvent( user, requestContext ));

        return user;
    }

    @Override
    public User login(String username, String password) {
        User user = (User) loadUserByUsername(username);
        if (!user.isEnabled()) {
            if (!user.isEverActivated()) {
                throw new AccountActivationException("Your account has never been activated. Please check your email and follow the activation instructions.", user.getUsername());
            } else {
                throw new UserEnabledStatusException("User account has been disabled by an administrator. Please contact support.", 403);
            }
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException("Incorrect password");
        }
        return user;
    }

    @Override
    public void logout() {

    }

    @Override
    public User getAuthenticatedUser() {
        return userService.getAuthenticatedUser();
    }

    @Override
    public UserDetails loadUserByUsername(String username){
        return userService.getUserByUsername(username);
    }




    // region AccountConfirmation

    @Override
    public User confirmNewUserAccount(String token, HttpServletRequest request) {
        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.getToken(token);
        accountConfirmationTokenService.verifyTokenValidity(accountConfirmationToken);

        User user = accountConfirmationToken.getUser();

        userService.activateUser(user.getId());
        userService.setUserMailVerified(user);

        accountConfirmationTokenService.revokeToken(accountConfirmationToken);
        accountConfirmationAttemptService.clearAttempts(user);

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent( new ConfirmNewUserAccountEvent(user, requestContext) );

        return user;

    }

    @Override
    public void requestActivation(String token) {
        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.getToken(token);
        if( accountConfirmationToken.getUser().isEnabled()) {
            throw new UserEnabledStatusException("This user account is already activated.", 409);
        }
        if(accountConfirmationToken.isValid()) {
            String url = FRONT_URL + "/api/account-confirmation/activation=" + token ;
            throw new TokenValidityException("This token, is still valid. Please follow this link: " + url);
        }
        accountConfirmationTokenService.revokeToken(accountConfirmationToken);
        User user = accountConfirmationToken.getUser();
        AccountConfirmationToken newToken = accountConfirmationTokenService.createAccountConfirmationToken(user);
        mailerService.sendNewAccountConfirmation(newToken.getToken(), user);

    }

    @Override
    public void requestConfirmationLinkByUsername(String username) {
        User user = userService.getUserByUsername(username);
        if(user.isEnabled()){
            throw new UserEnabledStatusException("User account is already activated");
        }
        if(user.isEverActivated()){
            log.info("Tentative de réactivation d'un compte désactivé par un admin: {}", username);
            throw new AccountActivationException("User has been diactivated by administrator, please contact support for more information", user.getUsername());
        }
        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.createAccountConfirmationToken(user);
        mailerService.sendNewAccountConfirmation(accountConfirmationToken.getToken(), user);
    }

    // endregion


    // region password
    @Override
    public void resetPassword(PasswordResetForm form, String token, HttpServletRequest request) {
        checkIsNotAnonymous();

        PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(token);
        passwordResetTokenService.verifyTokenValidity(passwordResetToken);
        passwordResetTokenService.revokeToken(passwordResetToken);

        User user = passwordResetToken.getUser();

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        savePassword(form.password(), user, requestContext);
    }


    @Override
    public void changePassword(ChangePasswordForm form, HttpServletRequest request) {
        User authenticatedUser = userService.getAuthenticatedUser();

        if(!passwordEncoder.matches(form.currentPassword(), authenticatedUser.getPassword())){
            throw new InvalidPasswordException("The current password is not correct", 400);
        }

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        savePassword(form.password(), authenticatedUser,requestContext);
    }



    @Override
    public void requestPasswordReset(String email, HttpServletRequest request) {
        checkIsNotAnonymous();

        User user = userService.getUserByEmail(email);

        RequestContext requestContext = requestContextService.captureRequestContext(request);
        eventPublisher.publishEvent(new RequestPasswordResetEvent(user, requestContext));
    }


    @Override
    public void requestPasswordToken(String token){
        checkIsNotAnonymous();
        PasswordResetToken passwordResetToken = passwordResetTokenService.getToken(token);
        if(passwordResetToken.isValid()) {
            String url = FRONT_URL + "/api/password/reset-password?token=" + token ;
            throw new TokenValidityException("This token, is still valid. Please follow this link: " + url);
        }
        passwordResetTokenService.revokeToken(passwordResetToken);
        User user = passwordResetToken.getUser();
        PasswordResetToken newToken = passwordResetTokenService.createPasswordResetToken(user);
        mailerService.sendPasswordResetRefresh(newToken.getToken(), user);
    }

    // endregion


    // region changeEmail
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

        eventPublisher.publishEvent(new ChangeEmailRequestEvent(user, email, token.getToken(), requestContext));
        
    }


    @Override
    public void cancelEmailChange(String token) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        if (!emailConfirmationToken.isRevoked()){
            emailConfirmationTokenService.revokeToken(emailConfirmationToken);
        }
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

        eventPublisher.publishEvent(new ChangeEmailVerificationEvent(emailConfirmationToken.getUser(), requestContext, emailConfirmationToken.getToken(), emailConfirmationToken.getNewEmailAddress() ) );
        

    }



    @Override
    public void confirmEmail(String token) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenService.getToken(token);
        emailConfirmationTokenService.verifyTokenValidity(emailConfirmationToken);
        emailConfirmationTokenService.revokeToken(emailConfirmationToken);
        User user = emailConfirmationToken.getUser();
        String oldEmail = user.getEmail();
        String newEmail = emailConfirmationToken.getNewEmailAddress();
        checkEmailAvailability(newEmail);
        mailerService.sendChangeEmailConfirmation(token, user, oldEmail, newEmail );
        user.setEmail(newEmail);
        userService.saveUser(user);
    }

    // endregion




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




    private void checkIsNotAnonymous(){
        if(!userService.isAnonymous()) {
            String url = FRONT_URL + "/password/change-password";
            String message = "You are logged in. Please use the change password feature instead: ";
            throw new UserAuthenticationStateException(message + url, 403);
        }
    }



    private void savePassword(String password, User user, RequestContext requestContext){
        user.setPassword( passwordEncoder.encode(password) );
        if(user.isMustChangePassword()){
            user.setMustChangePassword(false);
        }
        userService.saveUser(user);

        eventPublisher.publishEvent(new PasswordChangedEvent(user, requestContext));
    }

}


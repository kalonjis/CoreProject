package be.steby.CoreProject.bll.services.security.impl;

import be.steby.CoreProject.bll.domain.password.exceptions.InvalidPasswordException;
import be.steby.CoreProject.bll.events.account.ConfirmNewUserAccountEvent;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.bll.exceptions.*;
import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.RequestContextService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.AuthService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final MailerService mailerService;
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final AccountConfirmationAttemptServiceImpl accountConfirmationAttemptService;


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








}


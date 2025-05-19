package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.domains.account.events.ConfirmNewUserAccountEvent;
import be.steby.CoreProject.bll.exceptions.AccountActivationException;
import be.steby.CoreProject.bll.exceptions.TokenValidityException;
import be.steby.CoreProject.bll.exceptions.UserEnabledStatusException;
import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.RequestContextService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.impl.AccountConfirmationAttemptServiceImpl;
import be.steby.CoreProject.bll.services.security.impl.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
@Slf4j
public class AccountConfirmationServiceImpl implements AccountConfirmationService {

    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final AccountConfirmationAttemptServiceImpl accountConfirmationAttemptService;
    private final UserService userService;
    private final RequestContextService requestContextService;
    private final ApplicationEventPublisher eventPublisher;


    @Value("${url.front_server}")
    private String FRONT_URL;


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
        //mailerService.sendNewAccountConfirmation(newToken.getToken(), user);

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
        //mailerService.sendNewAccountConfirmation(accountConfirmationToken.getToken(), user);
    }


}

package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.domains.account.events.AccountConfirmationEvent;
import be.steby.CoreProject.bll.domains.account.events.RequestAccountActivationEvent;
import be.steby.CoreProject.bll.domains.account.events.RequestAccountDeactivationEvent;
import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.bll.domains.account.services.tokens.deactivation.AccountDeactivationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.deactivation.AccountDeactivationTokenServiceImpl;
import be.steby.CoreProject.bll.domains.auth.services.RefreshTokenServiceImpl;
import be.steby.CoreProject.bll.exceptions.TokenValidityException;
import be.steby.CoreProject.bll.exceptions.UserEnabledStatusException;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationAttemptServiceImpl;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.AccountDeactivationToken;
import be.steby.CoreProject.pl.security.models.AccountDeactivationForm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final AccountConfirmationAttemptServiceImpl accountConfirmationAttemptService;
    private final AccountDeactivationTokenServiceImpl accountDeactivationTokenService;
    private final AccountDeactivationAttemptServiceImpl accountDeactivationAttemptService;
    private final RefreshTokenServiceImpl refreshTokenService;
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

        eventPublisher.publishEvent( new AccountConfirmationEvent(user, requestContext) );

        return user;

    }

    @Override
    public void requestActivation(String token, HttpServletRequest request) {
        AccountConfirmationToken accountConfirmationToken = accountConfirmationTokenService.getToken(token);
        User user = accountConfirmationToken.getUser();

        if( user.isEnabled()) {
            throw new UserEnabledStatusException("This user account is already activated.", 409);
        }
        if(accountConfirmationToken.isValid()) {
            String url = FRONT_URL + "/api/account-confirmation/activation=" + token ;
            throw new TokenValidityException("This token, is still valid. Please follow this link: " + url);
        }
        accountConfirmationTokenService.revokeToken(accountConfirmationToken);
        AccountConfirmationToken newToken = accountConfirmationTokenService.createAccountConfirmationToken(user);

        RequestContext requestContext = requestContextService.captureRequestContext(request);

        eventPublisher.publishEvent( new RequestAccountActivationEvent(user, newToken.getToken(), requestContext) );

    }

    /**
     * @param user
     * @param request
     */
    @Override
    public void requestDeactivation(User user, DeactivationRequest deactivationRequest, HttpServletRequest request) {
        if (!user.isEnabled()) {
            throw new UserEnabledStatusException("the account is already deactivated");
        }

        AccountDeactivationToken token = accountDeactivationTokenService.createAccountDeactivationToken(
                user,
                deactivationRequest.deactivationReason(),
                deactivationRequest.reasonDetails()
        );

        RequestContext requestContext = requestContextService.captureRequestContext(request);
        RequestAccountDeactivationEvent event = new RequestAccountDeactivationEvent(user, token.getToken(), requestContext);
        eventPublisher.publishEvent(event);

    }


    /**
     * @param token
     * @param httpRequest
     * @return
     */
    @Override
    public User deactivateAccount(String token, HttpServletRequest httpRequest) {
        AccountDeactivationToken accountDeactivationToken = accountDeactivationTokenService.getToken(token);
        accountDeactivationTokenService.verifyTokenValidity(accountDeactivationToken);
        User user = accountDeactivationToken.getUser();

        userService.deactivateUser(user.getId());
        refreshTokenService.revokeAllUserTokens(user);

        accountDeactivationToken.setConfirmed(true);
        accountDeactivationTokenService.saveToken(accountDeactivationToken);

        // 4. TODO: Publier l'événement
        // eventPublisher.publishEvent(new AccountDeactivationConfirmedEvent(...));
        return user;
    }
}

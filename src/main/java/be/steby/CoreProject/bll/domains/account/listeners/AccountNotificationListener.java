package be.steby.CoreProject.bll.domains.account.listeners;

import be.steby.CoreProject.bll.common.events.account.SignupEvent;
import be.steby.CoreProject.bll.domains.account.events.*;
import be.steby.CoreProject.bll.domains.account.services.AccountMailerService;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for account-related events that handles email notifications.
 * Now uses the domain-specific AccountMailerService instead of the generic MailerService.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class AccountNotificationListener {

    private final AccountMailerService accountMailerService;

    @EventListener
    @Async("emailExecutor")
    public void handleSignupEvent(SelfSignupCompletedEvent event){
        log.debug("Handling SignupEvent for user: {}", event.user().getEmail());

        accountMailerService.sendSignUpConfirmation(event.activationToken(), event.user());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleConfirmNewUserAccountEvent(AccountConfirmationEvent event){
        log.debug("Handling AccountConfirmationEvent for user: {}", event.user().getEmail());

        accountMailerService.sendWelcome(event.user());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountActivation(RequestAccountActivationEvent event){
        log.debug("Handling RequestAccountActivationEvent for user: {}", event.user().getEmail());

        accountMailerService.sendNewAccountConfirmation(event.token(), event.user());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountDeactivation(RequestAccountDeactivationEvent event){
        log.debug("Handling RequestAccountDeactivationEvent for user: {}", event.user().getEmail());

        accountMailerService.sendAccountDeactivationRequest(
                event.token(),
                event.user(),
                event.deactivationReason(),
                event.reasonDetails()
        );
    }

    @EventListener
    @Async("emailExecutor")
    public void handleAccountDeactivationConfirmed(AccountDeactivationConfirmedEvent event){
        log.info("Handling AccountDeactivationConfirmedEvent for user: {}", event.user().getEmail());

        accountMailerService.sendAccountDeactivationConfirmation(
                event.user(),
                event.deactivationReason(),
                event.reasonDetails()
        );
    }

    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountReactivation(RequestAccountReactivationEvent event){
        log.debug("Handling RequestAccountReactivationEvent for user: {}", event.user().getEmail());

        accountMailerService.sendAccountReactivationRequest(event.token(), event.user());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleAccountReactivationConfirmed(AccountReactivationConfirmedEvent event){
        log.info("Handling AccountReactivationConfirmedEvent for user: {}", event.user().getEmail());

        accountMailerService.sendAccountReactivationConfirmation(event.user());
    }
}
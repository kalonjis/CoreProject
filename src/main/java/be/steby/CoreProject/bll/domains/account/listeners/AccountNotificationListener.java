package be.steby.CoreProject.bll.domains.account.listeners;

import be.steby.CoreProject.bll.domains.account.events.*;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class AccountNotificationListener {

    private final MailerService mailerService;
    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;



    @EventListener
    @Async("emailExecutor")
    public void handleSignupEvent(SignupEvent event){
        AccountConfirmationToken token = accountConfirmationTokenService.createAccountConfirmationToken(event.user());
        mailerService.sendSignUpConfirmation(token.getToken(), event.user());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleConfirmNewUserAccountEvent(AccountConfirmationEvent event){
        mailerService.sendWelcome(event.user());
    }


    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountActivation(RequestAccountActivationEvent event){
        mailerService.sendNewAccountConfirmation(event.token(), event.user());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountDeactivation(RequestAccountDeactivationEvent event){
        mailerService.sendAccountDeactivationRequest(event.token(), event.user(), event.deactivationReason(), event.reasonDetails());
    }


    @EventListener
    @Async("emailExecutor")
    public void handleAccountDeactivationConfirmed(AccountDeactivationConfirmedEvent event){
        log.info("Sending deactivation confirmation email to user: {}", event.user().getEmail());
        mailerService.sendAccountDeactivationConfirmation(
                event.user(),
                event.deactivationReason(),
                event.reasonDetails()
        );
    }


    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountReactivation(RequestAccountReactivationEvent event){
        mailerService.sendAccountReactivationRequest(event.token(), event.user());
    }


    @EventListener
    @Async("emailExecutor")
    public void handleAccountReactivationConfirmed(AccountReactivationConfirmedEvent event){
        log.info("Sending reactivation confirmation email to user: {}", event.user().getEmail());
        mailerService.sendAccountReactivationConfirmation(event.user());
    }
}

package be.steby.CoreProject.bll.listeners;


import be.steby.CoreProject.bll.events.account.ConfirmNewUserAccountEvent;
import be.steby.CoreProject.bll.events.account.SignupEvent;
import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.security.impl.AccountConfirmationTokenServiceImpl;
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
    public void handleConfirmNexUserAccountEvent(ConfirmNewUserAccountEvent event){

        mailerService.sendWelcome(event.user());
    }

}

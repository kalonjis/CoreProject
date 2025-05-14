package be.steby.CoreProject.bll.listeners;


import be.steby.CoreProject.bll.events.account.ConfirmNewUserAccountEvent;
import be.steby.CoreProject.bll.services.MailerService;
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

    @EventListener
    @Async
    public void handleConfirmNexUserAccountEvent(ConfirmNewUserAccountEvent event){

        mailerService.sendWelcome(event.user());
    }

}

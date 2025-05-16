package be.steby.CoreProject.bll.listeners;


import be.steby.CoreProject.bll.events.security.email_events.ChangeEmailRequestEvent;
import be.steby.CoreProject.bll.events.security.email_events.ChangeEmailVerificationEvent;
import be.steby.CoreProject.bll.services.MailerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailChangeNotificationListener {

    private final MailerService mailerService;


    @EventListener
    @Async
    public void handleEmailChangeRequestEvent(ChangeEmailRequestEvent event){
        mailerService.sendChangeEmailRequest(event.token(), event.user());
    }

    @EventListener
    @Async
    public void handleChangeEmailVerificationEvent(ChangeEmailVerificationEvent event){
        mailerService.sendChangeEmailVerification(event.token(), event.user(), event.email());
    }
}

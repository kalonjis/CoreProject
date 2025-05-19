package be.steby.CoreProject.bll.domain.emailAddress.listeners;

import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeCancellationEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeConfirmationEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeRequestEvent;
import be.steby.CoreProject.bll.domain.emailAddress.events.EmailChangeVerificationEvent;
import be.steby.CoreProject.bll.services.MailerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EmailChangeNotificationListener {

    private final MailerService mailerService;


    @EventListener
    @Async("emailExecutor")
    public void handleEmailChangeRequestEvent(EmailChangeRequestEvent event){
        mailerService.sendChangeEmailRequest(event.token(), event.user());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleEmailChangeCancellationEvent(EmailChangeCancellationEvent event){
        mailerService.sendChangeEmailCancellation(event.user());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleEmailChangeVerificationEvent(EmailChangeVerificationEvent event){
        mailerService.sendChangeEmailVerification(event.token(), event.user(), event.email());
    }

    @EventListener
    @Async("emailExecutor")
    public void handleEmailAddressConfirmation(EmailChangeConfirmationEvent event){
        mailerService.sendChangeEmailConfirmation(event.token(), event.user(), event.oldAddress(), event.newAddress());
    }
}

package be.steby.CoreProject.bll.domains.emailaddress.listeners;

import be.steby.CoreProject.bll.common.services.notification.mailer.MailerService;
import be.steby.CoreProject.bll.domains.emailaddress.events.EmailChangeCancellationEvent;
import be.steby.CoreProject.bll.domains.emailaddress.events.EmailChangeConfirmationEvent;
import be.steby.CoreProject.bll.domains.emailaddress.events.EmailChangeRequestEvent;
import be.steby.CoreProject.bll.domains.emailaddress.events.EmailChangeVerificationEvent;
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

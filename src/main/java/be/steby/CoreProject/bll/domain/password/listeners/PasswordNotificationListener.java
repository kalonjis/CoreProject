package be.steby.CoreProject.bll.domain.password.listeners;

import be.steby.CoreProject.bll.domain.password.events.PasswordChangedEvent;
import be.steby.CoreProject.bll.domain.password.events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.domain.password.events.RequestPasswordTokenEvent;
import be.steby.CoreProject.bll.services.MailerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordNotificationListener {

    private final MailerService mailerService;


    @EventListener
    @Async("emailExecutor")
    public void handleRequestPasswordReset(RequestPasswordResetEvent event){
        mailerService.sendPasswordReset(event.token(), event.user());
    }


    @EventListener
    @Async("emailExecutor")
    public void handlePasswordChanged(PasswordChangedEvent event){
        mailerService.sendPasswordChangeConfirmation(event.user());
    }


    @EventListener
    @Async("emailExecutor")
    public void handleRequestPasswordToken(RequestPasswordTokenEvent event){
        mailerService.sendPasswordResetRefresh(event.newToken(), event.user());
    }
}

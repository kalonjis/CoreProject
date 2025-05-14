package be.steby.CoreProject.bll.listeners;


import be.steby.CoreProject.bll.events.security.password_events.PasswordChangedEvent;
import be.steby.CoreProject.bll.events.security.password_events.RequestPasswordResetEvent;
import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.security.impl.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordNotificationListener {

    private final MailerService mailerService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;


    @EventListener
    @Async("emailExecutor")
    public void handleRequestPasswordReset(RequestPasswordResetEvent event){
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(event.user());
        mailerService.sendPasswordReset(token.getToken(), event.user());
    }


    @EventListener
    @Async("emailExecutor")
    public void handlePasswordChanged(PasswordChangedEvent event){
        mailerService.sendPasswordChangeConfirmation(event.user());
    }
}

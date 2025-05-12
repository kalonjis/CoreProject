package be.steby.CoreProject.bll.listeners;

import be.steby.CoreProject.bll.events.device.DeviceDetectedEvent;
import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.security.impl.DeviceConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.tokens.DeviceConfirmationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;


@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class DeviceNotificationListener {

    private final MailerService mailerService;
    private final DeviceConfirmationTokenServiceImpl deviceConfirmationTokenService;

    @EventListener
    public void handleDeviceDetectedEvent(DeviceDetectedEvent event){

        boolean shouldSendNotification = determineNotificationNeeded(event);

        if( shouldSendNotification){

            DeviceConfirmationToken token = deviceConfirmationTokenService.createDeviceConfirmationToken(
                    event.user(),
                    event.device().getId()
            );

            if(event.isBlacklisted()){
                mailerService.sendBlacklistedDeviceAlert(
                        event.user(),
                        event.device(),
                        token.getToken()
                );
            } else{
                mailerService.sendNewDeviceAlert(
                        event.user(),
                        event.device(),
                        token.getToken()
                );
            }

        }
    }


    private boolean determineNotificationNeeded(DeviceDetectedEvent event) {

        if (event.isBlacklisted()){
            log.info("notification true car device est blacklisté");
            return true;
        }


        if (event.isFirstDevice() && event.user().getActivatedAt() != null) {
            long minutesSinceActivation = Duration.between(
                    event.user().getActivatedAt(),
                    Instant.now()
            ).toMinutes();

            // il faudra alors créer un evenement pour notifier en front "do you trust this device?"
            if (minutesSinceActivation <= 10) {

                log.info("pas de notif car activation recente sur ce meme appareil");

                return false;
            }
        }

        if (event.isNewDevice() || !event.isConfirmed()){
            log.info("notification true car device n'est pas encore confirmé");

            return true;
        }

        log.info("notification true ");

        return true;
    }
}

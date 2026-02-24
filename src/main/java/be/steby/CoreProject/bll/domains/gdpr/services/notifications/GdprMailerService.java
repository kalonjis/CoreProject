package be.steby.CoreProject.bll.domains.gdpr.services.notifications;

import be.steby.CoreProject.bll.common.services.notification.mailer.BaseMailerService;
import be.steby.CoreProject.bll.domains.account.services.DeactivationMessageService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.il.mail.EmailComposer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;


@Service
@Slf4j
public class GdprMailerService extends BaseMailerService {

    @Value("${url.front_server}")
    private String frontUrl;

    @Value("${app.gdpr.export.archive-ttl-hours:72}")
    private int gdprArchiveTtlHours;


    public GdprMailerService(
            EmailComposer emailComposer
    ) {
        super(emailComposer);
    }


    public void sendGdprExportConfirmation(String confirmToken, User user) {
        String confirmUrl = frontUrl + "/account/export/confirm?token=" + confirmToken;

        Context context = createBaseContext(user);
        context.setVariable("confirmUrl", confirmUrl);
        context.setVariable("ttlHours", gdprArchiveTtlHours);

        emailComposer.sendMail(
                "Confirm your data export request",
                "gdpr/GdprExportRequest",
                context,
                user.getEmail()
        );
    }


    public void sendGdprExportReady(String downloadToken, String downloadUrl, User user) {

        Context context = createBaseContext(user);
        context.setVariable("downloadUrl", downloadUrl);
        context.setVariable("ttlHours", gdprArchiveTtlHours);

        emailComposer.sendMail(
                "Your personal data export is ready",
                "gdpr/GdprExportReady",
                context,
                user.getEmail()
        );
    }
}
package be.steby.CoreProject.bll.domains.crm.outreach.services;

import be.steby.CoreProject.bll.common.services.notification.mailer.BaseMailerService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.il.mail.EmailComposer;
import be.steby.CoreProject.il.sanitizer.RichTextSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

/**
 * CRM domain mailer service for commercial outreach emails.
 *
 * <p>Handles emails sent by a commercial user directly to a CRM contact from within
 * the platform. Unlike transactional system emails (2FA, password reset), outreach
 * emails are human-to-human: the {@code From} address is the system no-reply address
 * (required for SMTP authorization), but the {@code Reply-To} is set to the
 * commercial's email so that the contact's reply lands in the right inbox.</p>
 *
 * <h3>Reply-To strategy</h3>
 * <p>Setting {@code Reply-To = commercial.email} is the standard approach used by
 * HubSpot and Salesforce for CRM outreach. It avoids SMTP SPF/DKIM issues while
 * ensuring replies reach the responsible commercial.</p>
 *
 * <h3>Rich-text body — server-side sanitization</h3>
 * <p>The body is HTML produced by TipTap on the frontend. Before injection into the
 * Thymeleaf template, it is sanitized by {@link RichTextSanitizer} using Jsoup's
 * allowlist approach. This ensures that a crafted HTTP request bypassing TipTap's
 * client-side whitelist cannot introduce dangerous content ({@code <script>},
 * {@code javascript:} URLs, {@code on*} event handlers) into the outbound email.</p>
 *
 * <h3>Interaction logging</h3>
 * <p>This service only handles the email send. The caller ({@link CrmOutreachServiceImpl})
 * is responsible for publishing the event that triggers the {@code EMAIL} interaction log.</p>
 */
@Service
@Slf4j
public class CrmOutreachMailerService extends BaseMailerService {

    private final RichTextSanitizer richTextSanitizer;

    public CrmOutreachMailerService(EmailComposer emailComposer, RichTextSanitizer richTextSanitizer) {
        super(emailComposer);
        this.richTextSanitizer = richTextSanitizer;
    }

    /**
     * Sends a CRM outreach email from a commercial to a contact.
     *
     * <p>The body is sanitized server-side before being passed to the template.
     * The email is dispatched asynchronously with the commercial's address as
     * {@code Reply-To}, so the contact's reply lands in the commercial's inbox.</p>
     *
     * @param contact    the CRM contact receiving the email
     * @param subject    the email subject written by the commercial
     * @param body       rich-text HTML body from TipTap; sanitized before use
     * @param commercial the commercial sending the email (provides Reply-To address)
     */
    public void sendOutreach(Contact contact, String subject, String body, User commercial) {
        log.info("Sending CRM outreach — to: {}, from commercial: {}, reply-to: {}",
                contact.getEmail(), commercial.getUsername(), commercial.getEmail());

        String sanitizedBody = richTextSanitizer.sanitize(body);

        Context context = new Context();
        context.setVariable("recipientFirstName", contact.getFirstName());
        context.setVariable("body",               sanitizedBody);
        context.setVariable("commercialName",     fullName(commercial));
        context.setVariable("commercialEmail",    commercial.getEmail());

        sendEmailWithReplyTo(
                subject,
                "crm/outreach",
                context,
                commercial.getEmail(),
                contact.getEmail()
        );

        log.debug("CRM outreach email dispatched to SmtpMailSender — subject: '{}'", subject);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private String fullName(User user) {
        if (user.getFirstname() != null && user.getLastname() != null) {
            return user.getFirstname() + " " + user.getLastname();
        }
        return user.getUsername();
    }
}

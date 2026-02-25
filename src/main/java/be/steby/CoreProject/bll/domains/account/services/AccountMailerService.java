package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.common.services.notification.mailer.BaseMailerService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.il.mail.EmailComposer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

/**
 * Account domain-specific mailer service.
 * Handles all email notifications related to account operations:
 * - Account creation and confirmation
 * - Account activation
 * - Account deactivation and reactivation
 * - Welcome messages
 */
@Service
@Slf4j
public class AccountMailerService extends BaseMailerService {

    private final DeactivationMessageService deactivationMessageService;

    public AccountMailerService(
            EmailComposer emailComposer,
            DeactivationMessageService deactivationMessageService
    ) {
        super(emailComposer);
        this.deactivationMessageService = deactivationMessageService;
    }

    /**
     * Sends signup confirmation email with account activation link.
     *
     * @param token The confirmation token
     * @param user The user who signed up
     */
    public void sendSignUpConfirmation(String token, User user) {
        log.info("Sending signup confirmation email to: {}", user.getEmail());

        String confirmationUrl = buildUrl("/account/confirmation", "token", token);

        Context context = createBaseContext(user);
        context.setVariable("temporaryPassword", "The password you defined");
        context.setVariable("url", confirmationUrl);

        sendEmail("Account confirmation", "accounts/signupConfirmation", context, user.getEmail());

        log.debug("Signup confirmation email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends welcome email to admin-created user with temporary password.
     *
     * This email is sent when an administrator creates a user account.
     * The user receives their temporary password and must log in to activate
     * their account and change the password.
     *
     * @param user The user created by admin
     * @param temporaryPassword The temporary password to include in email
     */
    public void sendAdminCreatedUserEmail(User user, String temporaryPassword) {
        log.info("Sending admin creation email with temporary password to: {}", user.getEmail());

        String loginUrl = buildUrl("/login");

        Context context = createBaseContext(user);
        context.setVariable("temporaryPassword", temporaryPassword);
        context.setVariable("loginUrl", loginUrl);
        context.setVariable("username", user.getUsername());

        sendEmail("Welcome - Your Account Has Been Created",
                "accounts/adminCreatedUser",
                context,
                user.getEmail());

        log.debug("Admin creation email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends new account confirmation request email.
     *
     * @param token The confirmation token
     * @param user The user requesting activation
     */
    public void sendNewAccountConfirmation(String token, User user) {
        log.info("Sending new account confirmation request to: {}", user.getEmail());

        String confirmationUrl = buildUrl("/account/confirmation", "token", token);

        Context context = createBaseContext(user);
        context.setVariable("url", confirmationUrl);

        sendEmail("Account confirmation", "accounts/newAccountConfirmationRequest", context, user.getEmail());

        log.debug("New account confirmation email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends welcome email after successful account confirmation.
     *
     * @param user The newly confirmed user
     */
    public void sendWelcome(User user) {
        log.info("Sending welcome email to: {}", user.getEmail());

        Context context = createBaseContext(user);

        sendEmail("Welcome", "accounts/GreetingComfirmedUser", context, user.getEmail());

        log.debug("Welcome email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends account deactivation request confirmation email.
     *
     * @param token The deactivation confirmation token
     * @param user The user requesting deactivation
     * @param deactivationReason The reason for deactivation
     * @param reasonDetails Additional details about the deactivation (can be null)
     */
    public void sendAccountDeactivationRequest(String token, User user,
                                               DeactivationReason deactivationReason,
                                               String reasonDetails) {
        log.info("Sending account deactivation request email to: {}", user.getEmail());

        String confirmationUrl = buildUrl("/account/deactivation", "token", token);

        String requestMessage = deactivationMessageService.getDeactivationRequestMessage(
                deactivationReason, reasonDetails);
        String subject = deactivationMessageService.getDeactivationRequestSubjectLine(deactivationReason);

        boolean canReactivate = deactivationReason != null &&
                deactivationReason.allowsReactivation();

        Context context = createBaseContext(user);
        context.setVariable("url", confirmationUrl);
        context.setVariable("deactivationReason", deactivationReason);
        context.setVariable("reason", deactivationReason.getDisplayName());
        context.setVariable("reasonDetails", reasonDetails);
        context.setVariable("requestMessage", requestMessage);
        context.setVariable("canReactivate", canReactivate);

        sendEmail(subject, "accounts/accountDeactivationRequest", context, user.getEmail());

        log.debug("Account deactivation request email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends account deactivation confirmation email.
     *
     * @param user The user whose account was deactivated
     * @param deactivationReason The reason for deactivation
     * @param reasonDetails Additional details about the deactivation (can be null)
     */
    public void sendAccountDeactivationConfirmation(User user,
                                                    DeactivationReason deactivationReason,
                                                    String reasonDetails) {
        log.info("Sending account deactivation confirmation email to: {}", user.getEmail());

        String confirmationMessage = deactivationMessageService.getConfirmationMessage(
                deactivationReason, reasonDetails);
        String subject = deactivationMessageService.getSubjectLine(deactivationReason);

        boolean canReactivate = deactivationReason != null &&
                deactivationReason.allowsReactivation();

        String reactivationUrl = canReactivate ?
                buildUrl("/account/reactivation") : null;

        Context context = createBaseContext(user);
        context.setVariable("deactivationReason", deactivationReason);
        context.setVariable("reason", deactivationReason.getDisplayName());
        context.setVariable("reasonDetails", reasonDetails);
        context.setVariable("message", confirmationMessage);
        context.setVariable("confirmationMessage", confirmationMessage);
        context.setVariable("canReactivate", canReactivate);
        context.setVariable("url", reactivationUrl);

        sendEmail(subject, "accounts/accountDeactivationConfirmation", context, user.getEmail());

        log.debug("Account deactivation confirmation email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends account reactivation request email.
     *
     * @param token The reactivation confirmation token
     * @param user The user requesting reactivation
     */
    public void sendAccountReactivationRequest(String token, User user) {
        log.info("Sending account reactivation request email to: {}", user.getEmail());

        String confirmationUrl = buildUrl("/account/reactivation", "token", token);

        Context context = createBaseContext(user);
        context.setVariable("url", confirmationUrl);

        sendEmail("Account Reactivation Request", "accounts/accountReactivationRequest",
                context, user.getEmail());

        log.debug("Account reactivation request email sent successfully to: {}", user.getEmail());
    }

    /**
     * Sends account reactivation confirmation email.
     *
     * @param user The user whose account was successfully reactivated
     */
    public void sendAccountReactivationConfirmation(User user) {
        log.info("Sending account reactivation confirmation email to: {}", user.getEmail());

        Context context = createBaseContext(user);

        sendEmail("Welcome Back - Account Reactivated", "accounts/accountReactivationConfirmation",
                context, user.getEmail());

        log.debug("Account reactivation confirmation email sent successfully to: {}", user.getEmail());
    }

    // =========================================================================
    // Step 1 — Deletion request confirmation email
    // =========================================================================

    /**
     * Sends the deletion confirmation email containing the single-use link.
     *
     * <p>The user must click the link to trigger the actual anonymization.
     * No data is modified at this stage.
     *
     * @param confirmToken the public ID of the {@link be.steby.CoreProject.dl.entities.tokens.AccountDeletionToken}
     * @param user         the user who initiated the deletion request
     */
    public void sendDeletionRequest(String confirmToken, User user) {
        log.info("Sending GDPR deletion request email to: {}", user.getEmail());

        String confirmUrl = buildUrl("/account/deletion", "token", confirmToken);

        Context context = createBaseContext(user);
        context.setVariable("confirmUrl", confirmUrl);

        sendEmail(
                "Confirm your account deletion request",
                "accounts/AccountDeletionRequest",
                context,
                user.getEmail()
        );

        log.debug("GDPR deletion request email sent to: {}", user.getEmail());
    }

    // =========================================================================
    // Step 2 — Post-deletion acknowledgement email
    // =========================================================================

    /**
     * Sends an acknowledgement email after GDPR anonymization has been executed.
     *
     * <p>Because the user entity is anonymized at this point, username and email
     * must be passed explicitly — they are captured before anonymization by
     * @param username the original username (for email personalisation)
     * @param email    the original email address (routing only — no longer stored)
     */
    public void sendDeletionConfirmed(String username, String email) {
        log.info("Sending GDPR deletion confirmation email to: {}", email);

        Context context = new Context();
        context.setVariable("username", username);

        sendEmail(
                "Your account has been deleted",
                "accounts/AccountDeletionConfirmed",
                context,
                email
        );

        log.debug("GDPR deletion confirmation email sent to: {}", email);
    }
}
package be.steby.CoreProject.bll.domains.account.listeners;

import be.steby.CoreProject.bll.common.events.account.SignupEvent;
import be.steby.CoreProject.bll.domains.account.events.*;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Account notification event listener with automatic public_id usage for URLs.
 *
 * This listener uses the public_id field automatically for secure URL transmission,
 * following the GitHub/Stripe pattern for token security.
 *
 * @author Your Team
 * @since 1.0.0
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class AccountNotificationListener {

    private final MailerService mailerService;
    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;

    // =========================================================================
    // Account Confirmation Events (with automatic public_id usage)
    // =========================================================================

    /**
     * Handles user signup by creating token and sending encrypted public_id in email.
     *
     * The token.publicId is automatically encrypted and safe for URL transmission.
     *
     * @param event SignupEvent containing user information
     */
    @EventListener
    @Async("emailExecutor")
    public void handleSignupEvent(SignupEvent event) {
        log.debug("Processing secure signup event for user: {}", event.user().getUsername());

        // Create token (automatic public_id encryption)
        AccountConfirmationToken token = accountConfirmationTokenService.createAccountConfirmationToken(event.user());

        // Use public_id for email URL (already encrypted)
        String urlSafeToken = token.getPublicId();
        mailerService.sendSignUpConfirmation(urlSafeToken, event.user());

        log.debug("Secure signup confirmation email sent with public_id");
    }

    /**
     * Handles new activation token requests with automatic public_id usage.
     *
     * @param event RequestAccountActivationEvent containing token and user info
     */
    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountActivation(RequestAccountActivationEvent event) {
        log.debug("Processing secure activation token request for user: {}", event.user().getUsername());

        // Note: The event.token() should now be the public_id from the service
        // If your event creation needs updating, use the token's public_id instead
        mailerService.sendNewAccountConfirmation(event.token(), event.user());

        log.debug("Secure activation token email sent");
    }

    // =========================================================================
    // Other Account Events (no token security needed)
    // =========================================================================

    /**
     * Handles account confirmation completion.
     *
     * @param event AccountConfirmationEvent containing confirmed user
     */
    @EventListener
    @Async("emailExecutor")
    public void handleConfirmNewUserAccountEvent(AccountConfirmationEvent event) {
        log.debug("Processing account confirmation completion for user: {}", event.user().getUsername());

        // Welcome email doesn't contain tokens, so no security needed
        mailerService.sendWelcome(event.user());

        log.debug("Welcome email sent");
    }

    /**
     * Handles account deactivation requests.
     * Note: Will be enhanced with token security in future phases.
     */
    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountDeactivation(RequestAccountDeactivationEvent event) {
        log.debug("Processing account deactivation request for user: {}", event.user().getUsername());

        // TODO: Enhance with public_id usage in future phase
        mailerService.sendAccountDeactivationRequest(
                event.token(),
                event.user(),
                event.deactivationReason(),
                event.reasonDetails()
        );

        log.debug("Account deactivation request email sent");
    }

    /**
     * Handles account deactivation confirmation.
     */
    @EventListener
    @Async("emailExecutor")
    public void handleAccountDeactivationConfirmed(AccountDeactivationConfirmedEvent event) {
        log.info("Sending deactivation confirmation email to user: {}", event.user().getEmail());

        mailerService.sendAccountDeactivationConfirmation(
                event.user(),
                event.deactivationReason(),
                event.reasonDetails()
        );
    }

    /**
     * Handles account reactivation requests.
     * Note: Will be enhanced with token security in future phases.
     */
    @EventListener
    @Async("emailExecutor")
    public void handleRequestAccountReactivation(RequestAccountReactivationEvent event) {
        log.debug("Processing account reactivation request for user: {}", event.user().getUsername());

        // TODO: Enhance with public_id usage in future phase
        mailerService.sendAccountReactivationRequest(event.token(), event.user());

        log.debug("Account reactivation request email sent");
    }

    /**
     * Handles account reactivation confirmation.
     */
    @EventListener
    @Async("emailExecutor")
    public void handleAccountReactivationConfirmed(AccountReactivationConfirmedEvent event) {
        log.info("Sending reactivation confirmation email to user: {}", event.user().getEmail());

        mailerService.sendAccountReactivationConfirmation(event.user());
    }
}
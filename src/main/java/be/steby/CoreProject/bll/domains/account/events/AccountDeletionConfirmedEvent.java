package be.steby.CoreProject.bll.domains.account.events;

/**
 * Published after GDPR anonymization has been successfully executed.
 *
 * <p>Note: personal data is captured <strong>before</strong> anonymization,
 * since the user entity no longer holds the original values after the operation.
 * The email address is kept here solely to route the post-deletion confirmation email
 * — it is not stored anywhere after that.
 *
 * <p>Consumed by:
 * {@link be.steby.CoreProject.bll.domains.account.listeners.AccountDeletionNotificationListener}
 *
 * @param username  the username at the time of deletion (for email personalisation)
 * @param email     the email address at the time of deletion (routing only)
 */
public record AccountDeletionConfirmedEvent(
        String username,
        String email
) {}
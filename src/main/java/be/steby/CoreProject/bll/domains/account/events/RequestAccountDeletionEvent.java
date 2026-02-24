package be.steby.CoreProject.bll.domains.account.events;


import be.steby.CoreProject.dl.entities.User;

/**
 * Published when a user submits a GDPR deletion request.
 *
 * <p>Triggers the sending of a confirmation email containing the single-use
 * deletion link. The user must click that link to complete the process.
 *
 * <p>Consumed by:
 * {@link be.steby.CoreProject.bll.domains.account.listeners.AccountDeletionNotificationListener}
 *
 * @param user          the user requesting deletion
 * @param confirmToken  the public ID of the {@link be.steby.CoreProject.dl.entities.tokens.AccountDeletionToken}
 *                      to embed in the confirmation link
 */
public record RequestAccountDeletionEvent(
        User user,
        String confirmToken
) {}
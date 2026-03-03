package be.steby.CoreProject.bll.domains.password.exceptions;

/**
 * Thrown when a user attempts to reuse a recent password.
 *
 * <p>Results in HTTP 409 (Conflict) — the request is valid but conflicts
 * with the current password history policy.
 *
 * <p>The number of passwords checked is configurable via
 * {@code security.password.history.count}.
 */
public class PasswordAlreadyUsedException extends PasswordDomainException {

    public PasswordAlreadyUsedException(int historyCount) {
        super("This password has already been used recently. " +
              "Please choose a password different from your last " + historyCount + ".", 409);
    }
}
package be.steby.CoreProject.bll.domains.account.exceptions.deletion;

import be.steby.CoreProject.bll.common.exceptions.TokenRevokedException;

/**
 * Thrown when a GDPR deletion confirmation token has already been revoked.
 *
 * <p>A token is revoked after successful deletion confirmation, or when
 * the user explicitly cancels their deletion request.
 *
 * <p>HTTP status: 410 Gone — the token existed but is no longer valid.
 */
public class DeletionTokenRevokedException extends TokenRevokedException {

    public DeletionTokenRevokedException(String message) {
        super(message, 410);
    }
}
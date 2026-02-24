package be.steby.CoreProject.bll.domains.account.exceptions.deletion;

import be.steby.CoreProject.bll.common.exceptions.TokenExpiredException;

/**
 * Thrown when a GDPR deletion confirmation token has passed its expiry date.
 *
 * <p>The user must submit a new deletion request to obtain a fresh token.
 *
 * <p>HTTP status: 498 (Token Expired).
 */
public class DeletionTokenExpiredException extends TokenExpiredException {

    public DeletionTokenExpiredException(String message) {
        super(message, 498);
    }
}
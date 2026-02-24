package be.steby.CoreProject.dl.entities.tokens;

import jakarta.persistence.Entity;
import lombok.*;

/**
 * Token entity used to confirm an irreversible GDPR account deletion request.
 *
 * <p>Extends {@link BaseToken}, inheriting all core token mechanics:
 * single-use value, expiry date, revocation flag, and user association.
 *
 * <p>Mapped to the {@code base_token} table via {@link jakarta.persistence.InheritanceType#JOINED}
 * strategy inherited from {@link BaseToken}.
 *
 * @see BaseToken
 * @see be.steby.CoreProject.dl.entities.tokens.enums.TokenType#ACCOUNT_DELETION
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccountDeletionToken extends BaseToken {
}
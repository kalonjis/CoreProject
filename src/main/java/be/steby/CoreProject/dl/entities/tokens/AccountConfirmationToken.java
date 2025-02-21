package be.steby.CoreProject.dl.entities.tokens;


import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;


/**
 * <p>Represents a token entity used for verifying/enabling user accounts in the application.</p>
 *
 * <p>This class extends the {@link BaseToken} class, inheriting its core functionality
 * such as token management and linkage to a user entity.</p>
 *
 * <h4>Database Mapping:</h4>
 * <ul>
 *   <li>This entity will inherit fields and relationships defined in {@link BaseToken}.</li>
 *   <li>Mapped to a database table (by default, named `account_confirmation_token`).</li>
 * </ul>
 *
 * <p>Usage: This entity is typically used during the user registration process, where a token
 * is generated and linked to a user, allowing secure user accounts activation.</p
 */
@AllArgsConstructor
@Entity
@Getter
@EqualsAndHashCode(callSuper = true)
public class AccountConfirmationToken extends BaseToken{
}

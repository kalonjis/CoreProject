package be.steby.CoreProject.dl.entities.tokens;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.regex.Pattern;


/**
 * <p>Represents a token entity used for verifying/enabling a new email address for a user in the application.</p>
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
 * <p>Usage: This entity is typically used during the user change email address process, where a token
 * is generated and linked to a user, allowing secure user email address activation.</p
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class EmailConfirmationToken extends BaseToken{

    private String newEmailAddress;

    private boolean confirmed = false;

}

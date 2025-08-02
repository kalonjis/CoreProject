package be.steby.CoreProject.dl.entities.tokens;

import be.steby.CoreProject.dl.enums.DeactivationReason;
import jakarta.persistence.*;
import lombok.*;

/**
 * <p>Represents a token entity used for account deactivation confirmation.</p>
 *
 * <p>This class extends the {@link BaseToken} class, inheriting its core functionality
 * such as token management and linkage to a user entity.</p>
 *
 * <h4>Database Mapping:</h4>
 * <ul>
 *   <li>This entity will inherit fields and relationships defined in {@link BaseToken}.</li>
 *   <li>Mapped to a database table (by default, named `account_deactivation_token`).</li>
 * </ul>
 *
 * <p>Usage: This entity is used during the account deactivation process, where a token
 * is generated and linked to a user, requiring email confirmation before account deactivation.</p>
 */
@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccountDeactivationToken extends BaseToken {

    @Column(nullable = false)
    private boolean confirmed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "deactivation_reason")
    private DeactivationReason deactivationReason;

    @Column(name = "reason_details", length = 500)
    private String reasonDetails;
}
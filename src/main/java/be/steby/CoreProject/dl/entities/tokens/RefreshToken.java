package be.steby.CoreProject.dl.entities.tokens;


import be.steby.CoreProject.dl.entities.Device;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;


/**
 * <p>Represents a token entity used for resetting user passwords in the application.</p>
 *
 * <p>This class extends the {@link BaseToken} class, inheriting its core functionality,
 * such as token management and linkage to a user entity.</p>
 *
 * <h4>Database Mapping:</h4>
 * <ul>
 *   <li>This entity will inherit fields and relationships defined in {@link BaseToken}.</li>
 *   <li>Mapped to a database table (by default, named `password_reset_token`).</li>
 * </ul>
 *
 * <p>Usage: This entity is typically used during the password reset process, where a token
 * is generated and linked to a user, allowing secure password updates.</p>
 *
 * @see BaseToken
 */
@Entity
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RefreshToken extends BaseToken {

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;
}
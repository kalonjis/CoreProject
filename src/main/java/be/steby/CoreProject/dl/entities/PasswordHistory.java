package be.steby.CoreProject.dl.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores the hashed history of a user's past passwords.
 *
 * <p>Used to prevent password reuse during reset or change operations.
 * Only hashes are stored — the plain password is never persisted.
 *
 * <p>The number of entries retained per user is configurable via
 * {@code security.password.history.count}.
 */
@Entity
@Table(
        name = "password_history",
        indexes = {
                @Index(name = "idx_password_history_user", columnList = "user_id"),
                @Index(name = "idx_password_history_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = false, exclude = {"passwordHash", "user"})
public class PasswordHistory extends BaseEntity<Long> {

    /**
     * The user who owns this password history entry.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * BCrypt hash of the past password.
     * Never store the plain password here.
     */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    /**
     * Password history entries don't need a public UUID.
     * They are internal records never exposed via API.
     */
    @Override
    protected boolean shouldGeneratePublicId() {
        return false;
    }
}
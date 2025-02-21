package be.steby.CoreProject.dl.entities.tokens;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * BaseToken is an abstract entity that serves as a common parent for different types of tokens,
 * such as UserVerificationToken and PasswordResetToken. It provides common properties
 * and functionalities related to token management.
 */
@Entity
@EqualsAndHashCode(callSuper = false)
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED) // Specifies inheritance strategy to map child entities in separate tables
public abstract class BaseToken extends BaseEntity<Long> {

    /**
     * The unique token value. This is used for identifying and verifying the token.
     */
    @Column(nullable = false, unique = true)
    private String token;


    /**
     * The user associated with this token. Establishes a many-to-one relationship
     * since a user can have multiple tokens (e.g., for verification or password reset).
     */
    @ManyToOne(fetch = FetchType.EAGER) // The associated User is eagerly fetched when retrieving a token
    @JoinColumn(nullable = false, name = "user_id")
    private User user;//



    /**
     * Expiry date and time for the token. This ensures the token becomes invalid
     * after the specified duration.
     */
    @Column(nullable = false)
    private Instant expiryDate;


    @Column(nullable = false)
    private boolean revoked = false;
}

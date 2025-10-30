package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing an OAuth2 account linked to a user.
 * Allows a single user to authenticate via multiple OAuth providers.
 * Unidirectional relationship to User (no back reference in User entity).
 */
@Entity
@Table(name = "oauth_account", 
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_oauth_provider_user_id",
               columnNames = {"provider", "provider_user_id"}
           )
       },
       indexes = {
           @Index(name = "idx_oauth_user_id", columnList = "user_id"),
           @Index(name = "idx_oauth_provider_email", columnList = "provider, email")
       })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OAuthAccount extends BaseEntity<Long> {
    
    /**
     * The user this OAuth account belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * OAuth provider (GITHUB, GOOGLE, MICROSOFT).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OAuthProvider provider;
    
    /**
     * User's unique ID in the OAuth provider's system.
     */
    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;
    
    /**
     * Email address returned by the OAuth provider.
     * May differ from User.email if user has multiple emails.
     */
    @Column(length = 254)
    private String email;
    
    /**
     * OAuth access token (optional, for future API calls to provider).
     */
    @Column(name = "access_token", length = 1000)
    private String accessToken;
    
    /**
     * OAuth refresh token (optional, for token renewal).
     */
    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;
    
    /**
     * When the access token expires.
     */
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;
}
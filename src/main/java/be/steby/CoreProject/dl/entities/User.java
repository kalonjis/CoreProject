package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.ReactivationPolicy;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Table(name = "user_", indexes = {
        @Index(name = "idx_user_public_id", columnList = "public_id"),           // UUID API
        @Index(name = "idx_user_email", columnList = "email"),                   // Login
        @Index(name = "idx_user_username", columnList = "username"),             // Login
        @Index(name = "idx_user_enabled", columnList = "enabled")               // Filtres admin
})
@ToString(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseEntity<Long> implements UserDetails {

    //region Fields

    /**
     * The email address of the user.
     * This is unique for each user.
     */
    @Column(unique = true, nullable = false, length = 254)
    private String email;

    /**
     * The recovery email address of the user.
     * This is unique for each user (when provided).
     * Used only in case of primary email address compromise.
     *
     * SECURITY NOTES:
     * - Should be verified before being used for password recovery
     * - Changes should have a 30-day grace period before becoming usable
     * - User should be notified on both primary and recovery email when changed
     */
    @Column(name = "recovery_email", unique = true, length = 254)
    private String recoveryEmail;

    /**
     * Timestamp when recovery email was last changed.
     * Used to enforce grace period before recovery email can be used.
     */
    @Column(name = "recovery_email_changed_at")
    private Instant recoveryEmailChangedAt;

    /**
     * Previous recovery email (for audit trail and rollback).
     */
    @Column(name = "recovery_email_previous", length = 254)
    private String recoveryEmailPrevious;

    /**
     * The username of the user.
     * Unique for each user.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /**
     * The first name of the user.
     */
    @Column(length = 50)
    private String firstname;

    /**
     * The last name of the user.
     */
    @Column(length = 50)
    private String lastname;

    /**
     * The phone number of the user.
     */
    @Column(length = 50)
    private String phoneNumber;


    /**
     * Indicates whether the user's phone number has been verified via SMS.
     * Must be true before enabling SMS-based two-factor authentication.
     * Automatically reset to false when phone number is updated.
     */
    @Column(name = "phone_number_verified", nullable = false)
    private boolean phoneNumberVerified = false;

    /**
     * The password of the user.
     * This will be encoded using bcrypt before storage.
     */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Instant passwordChangedAt;


    /**
     * Whether the user has completed their profile according to application requirements.
     * Used to enforce profile completion during onboarding.
     */
    @Column(nullable = false)
    private boolean profileComplete = false;

    /**
     * User biography (optional profile field).
     */
    @Column(length = 500)
    private String bio;

    /**
     * User avatar URL (optional profile field).
     */
    @Column(length = 500)
    private String avatarUrl;

    /**
     * The Role associated with the user.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * Whether the user has verified their email.
     */
    @Column(nullable = false)
    private boolean emailVerified;

    /**
     * Whether the user is enabled (able to log in).
     */
    @Column(nullable = false)
    private boolean enabled;


    /**
     * Whether the user account has ever been activated.
     * Used to distinguish between accounts that have never been activated
     * and accounts that have been deactivated by an admin.
     */
    @Column(nullable = false)
    private boolean everActivated;


    /**
     * Whether the user must change their password after logging in.
     */
    @Column(nullable = false)
    private boolean mustChangePassword;

    private Instant activatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activated_by")
    private User activatedBy;

    private Instant reactivatedAt;

    private Instant deactivatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "deactivation_reason")
    private DeactivationReason deactivationReason;

    @Column(name = "reason_details", length = 500)
    private String deactivationDetails;

    // Désactivation administrative
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_deactivation_reason")
    private AdminDeactivationCategory adminDeactivationReason;

    @Column(name = "admin_deactivation_details", length = 500)
    private String adminDeactivationDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_deactivated_by")
    private User adminDeactivatedBy;

    @Column(name = "admin_deactivated_at")
    private Instant adminDeactivatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reactivated_by")
    private User reactivatedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "reactivation_policy")
    private ReactivationPolicy reactivationPolicy;

    /**
     * Whether the user has activated one or more twoFactor method for authentication validation.
     */
    @Column(nullable = false)
    private boolean twoFactorEnabled;

    // endregion


    // region Constructors

    /**
     * Constructor to initialize only necessary fields (new user auto creation).
     */
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.passwordChangedAt = Instant.now();
        this.userRoles = UserRole.setRoles(UserRole.USER);
        this.enabled = false;
        this.emailVerified = false;
        this.mustChangePassword = false;
    }


    public User(String firstname, String lastname, String email, String phoneNumber) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.enabled = false;
        this.emailVerified = false;
        this.mustChangePassword = true;
    }



    /**
     * Constructor to initialize common fields (used by admin creation).
     */
    public User(String username, String firstname, String lastname, String email, String phoneNumber, Set<UserRole> userRoles) {
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userRoles = userRoles;
        this.enabled = false;
        this.emailVerified = false;
        this.mustChangePassword = true;
    }

    /**
     * Constructor with password (admin-created user).
     */
    public User(String username, String firstname, String lastname, String email, String phoneNumber, String password, Set<UserRole> userRoles) {

        this(username, firstname, lastname, email, phoneNumber, userRoles);
        this.password = password;
        this.passwordChangedAt = Instant.now();
    }


    // endregion

    // region check role
    /**
     * Checks if this user has the SUPER_ADMIN role.
     * @return true if user is a super admin
     */
    public boolean isSuperAdmin() {
        return this.userRoles.contains(UserRole.SUPER_ADMIN);
    }

    /**
     * Checks if this user has the ADMIN role (but not necessarily SUPER_ADMIN).
     * @return true if user is an admin
     */
    public boolean isAdmin() {
        return this.userRoles.contains(UserRole.ADMIN);
    }

    /**
     * Checks if this user has admin privileges (ADMIN or SUPER_ADMIN).
     * @return true if user has admin privileges
     */
    public boolean hasAdminPrivileges() {
        return isAdmin() || isSuperAdmin();
    }

    /**
     * Checks if this user has a specific role.
     * @param role the role to check
     * @return true if user has the role
     */
    public boolean hasRole(UserRole role) {
        return this.userRoles.contains(role);
    }


    /**
     * Gets the highest role of this user.
     * Useful for logging and permission checks.
     */
    public UserRole getHighestRole() {
        if (isSuperAdmin()) return UserRole.SUPER_ADMIN;
        if (isAdmin()) return UserRole.ADMIN;
        if (hasRole(UserRole.MODERATOR)) return UserRole.MODERATOR;
        if (hasRole(UserRole.USER)) return UserRole.USER;
        return UserRole.GUEST;
    }
    // endregion


    // region UserDetails methods implementation
    /**
     * Retrieves the GrantedAuthority for the user's role.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userRoles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * Indicates whether the user account has expired.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user account is locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether user credentials have expired.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user account is enabled (can login).
     */
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }


    // endregion


    /**
     * Détermine si l'utilisateur a été désactivé par un admin
     */
    public boolean isAdminDeactivated() {
        return !enabled && adminDeactivationReason != null;
    }

    /**
     * Détermine si l'utilisateur s'est auto-désactivé
     */
    public boolean isSelfDeactivated() {
        return !enabled && deactivationReason != null && adminDeactivationReason == null;
    }

    /**
     * Récupère la raison de désactivation (admin ou self)
     */
    public String getDeactivationDisplayReason() {
        if (adminDeactivationReason != null) {
            return adminDeactivationReason.getDisplayName();
        }
        if (deactivationReason != null) {
            return deactivationReason.getDisplayName();
        }
        return null;
    }


    public boolean wasCreatedByAdmin() {
        return createdBy != null;  // Simple et clean maintenant !
    }

    public boolean isSelfSignup() {
        return createdBy == null;
    }


    /**
     * Calculates the number of days since the recovery email was last changed.
     * Returns null if recovery email was never changed.
     *
     * @return Number of days since last change, or null if never changed
     */
    public Long getDaysSinceRecoveryEmailChanged() {
        if (recoveryEmailChangedAt == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(recoveryEmailChangedAt, Instant.now());
    }

    /**
     * Checks if recovery email is within the grace period (30 days by default).
     * If recovery email was never changed, returns false (safe to use).
     *
     * @return true if within grace period and should not be used yet
     */
    public boolean isRecoveryEmailInGracePeriod() {
        return isRecoveryEmailInGracePeriod(30);
    }

    /**
     * Checks if recovery email is within a custom grace period.
     *
     * @param gracePeriodDays Number of days for grace period
     * @return true if within grace period
     */
    public boolean isRecoveryEmailInGracePeriod(int gracePeriodDays) {
        if (recoveryEmailChangedAt == null) {
            return false; // Never changed = safe to use
        }
        Long daysSinceChange = getDaysSinceRecoveryEmailChanged();
        return daysSinceChange != null && daysSinceChange < gracePeriodDays;
    }

    /**
     * Checks if recovery email can be safely used for password recovery.
     *
     * @return true if recovery email exists and is not in grace period
     */
    public boolean canUseRecoveryEmail() {
        return recoveryEmail != null
                && !recoveryEmail.isBlank()
                && !isRecoveryEmailInGracePeriod();
    }

}

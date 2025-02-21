package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
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
@Table(name = "user_")
@ToString(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class User extends BaseEntity<Long> implements UserDetails {

    //region Fields

    /**
     * The email address of the user.
     * This is unique for each user.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String email;

    /**
     * The username of the user.
     * Unique for each user.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    /**
     * The first name of the user.
     */
    @Column(nullable = false, length = 50)
    private String firstname;

    /**
     * The last name of the user.
     */
    @Column(nullable = false, length = 50)
    private String lastname;

    /**
     * The phone number of the user.
     */
    @Column(length = 50)
    private String phoneNumber;

    /**
     * The password of the user.
     * This will be encoded using bcrypt before storage.
     */
    @Column(nullable = false)
    private String password;

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
     * Whether the user must change their password after logging in.
     */
    @Column(nullable = false)
    private boolean mustChangePassword;


    @Column(nullable = false)
    private int resetPasswordAttempts;

    @Column
    private Instant resetPasswordLastAttemptTime;

    @Column(nullable = false)
    private int accountConfirmationAttempts;

    @Column
    private Instant accountConfirmationLastAttemptTime;

    // endregion


    // region Constructors
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
        this.resetPasswordAttempts = 0;
        this.resetPasswordLastAttemptTime = null;
        this.accountConfirmationAttempts = 0;
        this.accountConfirmationLastAttemptTime = null;
    }

    /**
     * Constructor with password (admin-created user).
     */
    public User(String username, String firstname, String lastname, String email, String phoneNumber, String password, Set<UserRole> userRoles) {

        this(username, firstname, lastname, email, phoneNumber, userRoles);
        this.password = password;
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

}

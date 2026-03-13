package be.steby.CoreProject.dl.entities;

import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    // =========================================================================
    // Constructeurs
    // =========================================================================

    @Nested
    class Constructeurs {

        @Test
        void constructeurAutoInscription_initialiseLesBonsDefauts() {
            User u = new User("alice", "alice@test.com", "hashed");

            assertThat(u.getUsername()).isEqualTo("alice");
            assertThat(u.getEmail()).isEqualTo("alice@test.com");
            assertThat(u.isEnabled()).isFalse();
            assertThat(u.isEmailVerified()).isFalse();
            assertThat(u.isMustChangePassword()).isFalse();
            assertThat(u.getUserRoles()).contains(UserRole.USER);
            assertThat(u.getPasswordChangedAt()).isNotNull();
        }

        @Test
        void constructeurAdminSansPassword_mustChangePasswordEstTrue() {
            User u = new User("bob", "Bob", "Smith", "bob@test.com", "0600000000",
                    Set.of(UserRole.USER));

            assertThat(u.isMustChangePassword()).isTrue();
            assertThat(u.isEnabled()).isFalse();
            assertThat(u.getPassword()).isNull();
        }

        @Test
        void constructeurAdminAvecPassword_passwordChangedAtEstRenseigne() {
            User u = new User("bob", "Bob", "Smith", "bob@test.com", "0600000000",
                    "hashed", Set.of(UserRole.USER));

            assertThat(u.getPassword()).isEqualTo("hashed");
            assertThat(u.getPasswordChangedAt()).isNotNull();
        }
    }

    // =========================================================================
    // Rôles
    // =========================================================================

    @Nested
    class Roles {

        @Test
        void isSuperAdmin_avecRoleSuperAdmin_retourneTrue() {
            user.setUserRoles(Set.of(UserRole.SUPER_ADMIN));
            assertThat(user.isSuperAdmin()).isTrue();
        }

        @Test
        void isSuperAdmin_avecRoleUser_retourneFalse() {
            user.setUserRoles(Set.of(UserRole.USER));
            assertThat(user.isSuperAdmin()).isFalse();
        }

        @Test
        void isAdmin_avecRoleAdmin_retourneTrue() {
            user.setUserRoles(Set.of(UserRole.ADMIN));
            assertThat(user.isAdmin()).isTrue();
        }

        @Test
        void hasAdminPrivileges_avecRoleAdmin_retourneTrue() {
            user.setUserRoles(Set.of(UserRole.ADMIN));
            assertThat(user.hasAdminPrivileges()).isTrue();
        }

        @Test
        void hasAdminPrivileges_avecRoleSuperAdmin_retourneTrue() {
            user.setUserRoles(Set.of(UserRole.SUPER_ADMIN));
            assertThat(user.hasAdminPrivileges()).isTrue();
        }

        @Test
        void hasAdminPrivileges_avecRoleUser_retourneFalse() {
            user.setUserRoles(Set.of(UserRole.USER));
            assertThat(user.hasAdminPrivileges()).isFalse();
        }

        @Test
        void getHighestRole_avecPlusieursRoles_retourneLePlusEleve() {
            user.setUserRoles(Set.of(UserRole.USER, UserRole.ADMIN));
            assertThat(user.getHighestRole()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        void getHighestRole_sansPlusHautRole_retourneGuest() {
            user.setUserRoles(Set.of());
            assertThat(user.getHighestRole()).isEqualTo(UserRole.GUEST);
        }

        @Test
        void getAuthorities_retourneUneAuthorityParRole() {
            user.setUserRoles(Set.of(UserRole.USER, UserRole.MODERATOR));
            assertThat(user.getAuthorities()).hasSize(2);
            assertThat(user.getAuthorities())
                    .extracting("authority")
                    .containsExactlyInAnyOrder("USER", "MODERATOR");
        }
    }

    // =========================================================================
    // États du compte
    // =========================================================================

    @Nested
    class EtatsDuCompte {

        @Test
        void isGdprDeleted_quandGdprDeletedAtNull_retourneFalse() {
            user.setGdprDeletedAt(null);
            assertThat(user.isGdprDeleted()).isFalse();
        }

        @Test
        void isGdprDeleted_quandGdprDeletedAtRenseigne_retourneTrue() {
            user.setGdprDeletedAt(Instant.now());
            assertThat(user.isGdprDeleted()).isTrue();
        }

        @Test
        void isAdminDeactivated_quandDesactiveParAdmin_retourneTrue() {
            user.setEnabled(false);
            user.setAdminDeactivationReason(AdminDeactivationCategory.BANNED_INAPPROPRIATE_BEHAVIOR);
            assertThat(user.isAdminDeactivated()).isTrue();
        }

        @Test
        void isAdminDeactivated_quandActif_retourneFalse() {
            user.setEnabled(true);
            user.setAdminDeactivationReason(null);
            assertThat(user.isAdminDeactivated()).isFalse();
        }

        @Test
        void isSelfDeactivated_quandDesactivationVolontaire_retourneTrue() {
            user.setEnabled(false);
            user.setDeactivationReason(DeactivationReason.PRIVACY_CONCERNS);
            user.setAdminDeactivationReason(null);
            user.setGdprDeletedAt(null);
            assertThat(user.isSelfDeactivated()).isTrue();
        }

        @Test
        void isSelfDeactivated_quandGdprSupprime_retourneFalse() {
            user.setEnabled(false);
            user.setDeactivationReason(DeactivationReason.PRIVACY_CONCERNS);
            user.setAdminDeactivationReason(null);
            user.setGdprDeletedAt(Instant.now());
            assertThat(user.isSelfDeactivated()).isFalse();
        }

        @Test
        void hasPassword_quandPasswordNull_retourneFalse() {
            user.setPassword(null);
            assertThat(user.hasPassword()).isFalse();
        }

        @Test
        void hasPassword_quandPasswordRenseigne_retourneTrue() {
            user.setPassword("hashed_password");
            assertThat(user.hasPassword()).isTrue();
        }
    }

    // =========================================================================
    // Création du compte
    // =========================================================================

    @Nested
    class CreationDuCompte {

        @Test
        void wasCreatedByAdmin_quandCreatedByNull_retourneFalse() {
            user.createdBy = null; // champ protected hérité de BaseEntity
            assertThat(user.wasCreatedByAdmin()).isFalse();
        }

        @Test
        void wasCreatedByAdmin_quandCreatedByRenseigne_retourneTrue() {
            user.createdBy = "admin"; // champ protected hérité de BaseEntity
            assertThat(user.wasCreatedByAdmin()).isTrue();
        }

        @Test
        void isSelfSignup_quandCreatedByNull_retourneTrue() {
            user.createdBy = null; // champ protected hérité de BaseEntity
            assertThat(user.isSelfSignup()).isTrue();
        }
    }

    // =========================================================================
    // Recovery email & grace period
    // =========================================================================

    @Nested
    class RecoveryEmail {

        @Test
        void isRecoveryEmailInGracePeriod_quandJamaisChange_retourneFalse() {
            user.setRecoveryEmailChangedAt(null);
            assertThat(user.isRecoveryEmailInGracePeriod()).isFalse();
        }

        @Test
        void isRecoveryEmailInGracePeriod_quandChangeRecemment_retourneTrue() {
            user.setRecoveryEmailChangedAt(Instant.now().minus(5, ChronoUnit.DAYS));
            assertThat(user.isRecoveryEmailInGracePeriod()).isTrue();
        }

        @Test
        void isRecoveryEmailInGracePeriod_quandChangementAncien_retourneFalse() {
            user.setRecoveryEmailChangedAt(Instant.now().minus(31, ChronoUnit.DAYS));
            assertThat(user.isRecoveryEmailInGracePeriod()).isFalse();
        }

        @Test
        void canUseRecoveryEmail_quandEmailNullEtHorsGracePeriod_retourneFalse() {
            user.setRecoveryEmail(null);
            user.setRecoveryEmailChangedAt(null);
            assertThat(user.canUseRecoveryEmail()).isFalse();
        }

        @Test
        void canUseRecoveryEmail_quandEmailValideEtHorsGracePeriod_retourneTrue() {
            user.setRecoveryEmail("recovery@test.com");
            user.setRecoveryEmailChangedAt(Instant.now().minus(31, ChronoUnit.DAYS));
            assertThat(user.canUseRecoveryEmail()).isTrue();
        }

        @Test
        void canUseRecoveryEmail_quandEmailValideEtDansGracePeriod_retourneFalse() {
            user.setRecoveryEmail("recovery@test.com");
            user.setRecoveryEmailChangedAt(Instant.now().minus(5, ChronoUnit.DAYS));
            assertThat(user.canUseRecoveryEmail()).isFalse();
        }

        @Test
        void getDaysSinceRecoveryEmailChanged_quandJamaisChange_retourneNull() {
            user.setRecoveryEmailChangedAt(null);
            assertThat(user.getDaysSinceRecoveryEmailChanged()).isNull();
        }

        @Test
        void getDaysSinceRecoveryEmailChanged_quandChange10JoursAvant_retourne10() {
            user.setRecoveryEmailChangedAt(Instant.now().minus(10, ChronoUnit.DAYS));
            assertThat(user.getDaysSinceRecoveryEmailChanged()).isEqualTo(10L);
        }
    }
}
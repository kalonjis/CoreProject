package be.steby.CoreProject.bll.domains.user.services;

import be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.bll.common.exceptions.UserPermissionException;
import be.steby.CoreProject.bll.common.exceptions.UsernameAlreadyTakenException;
import be.steby.CoreProject.bll.common.models.reactivation.ReactivationEligibility;
import be.steby.CoreProject.bll.common.services.reactivation.ReactivationPolicyService;
import be.steby.CoreProject.bll.domains.emailaddress.exceptions.EmailAlreadyUsedException;
import be.steby.CoreProject.bll.domains.user.events.UserPersistedEvent;
import be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.ReactivationPolicy;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ReactivationPolicyService reactivationPolicyService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserAuthenticationService userAuthenticationService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private User admin;

    @BeforeEach
    void setUp() {
        user = new User("alice", "alice@test.com", "hashed");
        TestUtils.setEntityId(user, 1L);

        admin = new User("adminBob", "admin@test.com", "hashed");
        admin.setUserRoles(Set.of(UserRole.ADMIN));
        TestUtils.setEntityId(admin, 2L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // Lookup — getUserById
    // =========================================================================

    @Nested
    class GetUserById {

        @Test
        void trouvé_retourneLUtilisateur() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            assertThat(userService.getUserById(1L)).isSameAs(user);
        }

        @Test
        void nonTrouvé_leveUserNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // =========================================================================
    // Lookup — getUserByUsername / Email / PublicId / Identifier
    // =========================================================================

    @Nested
    class GetUserByIdentifier {

        @Test
        void getUserByUsername_trouvé() {
            when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.of(user));
            assertThat(userService.getUserByUsername("alice")).isSameAs(user);
        }

        @Test
        void getUserByUsername_nonTrouvé_leveUserNotFoundException() {
            when(userRepository.findByUsernameIgnoreCase("inconnu")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserByUsername("inconnu"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void getUserByEmail_trouvé() {
            when(userRepository.findByEmailIgnoreCase("alice@test.com")).thenReturn(Optional.of(user));
            assertThat(userService.getUserByEmail("alice@test.com")).isSameAs(user);
        }

        @Test
        void getUserByEmail_nonTrouvé_leveUserNotFoundException() {
            when(userRepository.findByEmailIgnoreCase("inconnu@test.com")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserByEmail("inconnu@test.com"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void getUserByPublicId_trouvé() {
            when(userRepository.findByPublicId("uuid-123")).thenReturn(Optional.of(user));
            assertThat(userService.getUserByPublicId("uuid-123")).isSameAs(user);
        }

        @Test
        void getUserByPublicId_nonTrouvé_leveUserNotFoundException() {
            when(userRepository.findByPublicId("uuid-xxx")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserByPublicId("uuid-xxx"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void getUserByUsernameOrByEmail_trouvé() {
            when(userRepository.findByUsernameOrEmail("alice")).thenReturn(Optional.of(user));
            assertThat(userService.getUserByUsernameOrByEmail("alice")).isSameAs(user);
        }

        @Test
        void getUserByUsernameOrByEmail_nonTrouvé_leveUserNotFoundException() {
            when(userRepository.findByUsernameOrEmail("inconnu")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUserByUsernameOrByEmail("inconnu"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void getTotalUsers_délègueAuRepository() {
            when(userRepository.count()).thenReturn(42L);
            assertThat(userService.getTotalUsers()).isEqualTo(42L);
        }
    }

    // =========================================================================
    // saveUser
    // =========================================================================

    @Nested
    class SaveUser {

        @Test
        void sauvegarde_et_publieUserPersistedEvent() {
            when(userRepository.save(user)).thenReturn(user);

            User result = userService.saveUser(user);

            assertThat(result).isSameAs(user);
            verify(userRepository).save(user);
            verify(eventPublisher).publishEvent(any(UserPersistedEvent.class));
        }
    }

    // =========================================================================
    // activateUser
    // =========================================================================

    @Nested
    class ActivateUser {

        @Test
        void utilisateurNonTrouvé_leveUserNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.activateUser(99L))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void déjàActif_leveAttributeUnchangedException() {
            user.setEnabled(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            assertThatThrownBy(() -> userService.activateUser(1L))
                    .isInstanceOf(AttributeUnchangedException.class);
        }

        @Test
        void succès_activeLesBonsChamps() {
            user.setEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            userService.activateUser(1L);

            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isEverActivated()).isTrue();
            assertThat(user.getActivatedAt()).isNotNull();
            verify(userRepository).save(user);
            verify(eventPublisher).publishEvent(any(UserPersistedEvent.class));
        }
    }

    // =========================================================================
    // deactivateUser
    // =========================================================================

    @Nested
    class DeactivateUser {

        @Test
        void utilisateurNonTrouvé_leveUserNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.deactivateUser(99L, DeactivationReason.PRIVACY_CONCERNS, null))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void déjàDésactivé_leveAttributeUnchangedException() {
            user.setEnabled(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            assertThatThrownBy(() -> userService.deactivateUser(1L, DeactivationReason.PRIVACY_CONCERNS, null))
                    .isInstanceOf(AttributeUnchangedException.class);
        }

        @Test
        void succès_désactiveLesBonsChamps() {
            user.setEnabled(true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            userService.deactivateUser(1L, DeactivationReason.PRIVACY_CONCERNS, "raison perso");

            assertThat(user.isEnabled()).isFalse();
            assertThat(user.getDeactivatedAt()).isNotNull();
            assertThat(user.getDeactivationReason()).isEqualTo(DeactivationReason.PRIVACY_CONCERNS);
            assertThat(user.getDeactivationDetails()).isEqualTo("raison perso");
        }
    }

    // =========================================================================
    // reactivateUser
    // =========================================================================

    @Nested
    class ReactivateUser {

        @Test
        void déjàActif_leveAttributeUnchangedException() {
            user.setEnabled(true);
            assertThatThrownBy(() -> userService.reactivateUser(user))
                    .isInstanceOf(AttributeUnchangedException.class);
        }

        @Test
        void nonÉligible_leveUserPermissionException() {
            user.setEnabled(false);
            when(reactivationPolicyService.checkEligibility(user, user))
                    .thenReturn(ReactivationEligibility.notEligible("banned"));
            assertThatThrownBy(() -> userService.reactivateUser(user))
                    .isInstanceOf(UserPermissionException.class);
        }

        @Test
        void succès_activeEtEffaceLesChampsDeDésactivation() {
            user.setEnabled(false);
            user.setDeactivationReason(DeactivationReason.PRIVACY_CONCERNS);
            user.setDeactivationDetails("quelques détails");

            when(reactivationPolicyService.checkEligibility(user, user))
                    .thenReturn(ReactivationEligibility.eligible());
            when(reactivationPolicyService.determineReactivationPolicy(user, user))
                    .thenReturn(ReactivationPolicy.SELF_SERVICE);
            when(userRepository.save(any())).thenReturn(user);

            userService.reactivateUser(user);

            assertThat(user.isEnabled()).isTrue();
            assertThat(user.getReactivatedAt()).isNotNull();
            assertThat(user.getReactivatedBy()).isSameAs(user);
            assertThat(user.getReactivationPolicy()).isEqualTo(ReactivationPolicy.SELF_SERVICE);
            assertThat(user.getDeactivationReason()).isNull();
            assertThat(user.getDeactivationDetails()).isNull();
            assertThat(user.getDeactivatedAt()).isNull();
        }
    }

    // =========================================================================
    // anonymizeUser
    // =========================================================================

    @Nested
    class AnonymizeUser {

        @Test
        void succès_anonymiseLesChamps() {
            user.setEnabled(true);
            when(userRepository.save(any())).thenReturn(user);

            userService.anonymizeUser(user);

            assertThat(user.getEmail()).isEqualTo("deleted_1@anonymized.local");
            assertThat(user.getFirstname()).isEqualTo("User");
            assertThat(user.getLastname()).isEqualTo("Deleted");
            assertThat(user.getPhoneNumber()).isNull();
            assertThat(user.isEnabled()).isFalse();
            assertThat(user.getGdprDeletedAt()).isNotNull();
        }
    }

    // =========================================================================
    // adminActivateUser
    // =========================================================================

    @Nested
    class AdminActivateUser {

        @Test
        void déjàActif_leveAttributeUnchangedException() {
            user.setEnabled(true);
            assertThatThrownBy(() -> userService.adminActivateUser(user, admin))
                    .isInstanceOf(AttributeUnchangedException.class);
        }

        @Test
        void déjàActivéAvant_leveIllegalStateException() {
            user.setEnabled(false);
            user.setEverActivated(true);
            assertThatThrownBy(() -> userService.adminActivateUser(user, admin))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void succès_activeLesBonsChamps() {
            user.setEnabled(false);
            user.setEverActivated(false);
            when(userRepository.save(any())).thenReturn(user);

            userService.adminActivateUser(user, admin);

            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isEverActivated()).isTrue();
            assertThat(user.getActivatedAt()).isNotNull();
            assertThat(user.getActivatedBy()).isSameAs(admin);
            assertThat(user.isEmailVerified()).isTrue();
        }
    }

    // =========================================================================
    // adminDeactivateUser
    // =========================================================================

    @Nested
    class AdminDeactivateUser {

        @Test
        void déjàDésactivé_leveAttributeUnchangedException() {
            user.setEnabled(false);
            assertThatThrownBy(() -> userService.adminDeactivateUser(
                    user, admin, AdminDeactivationCategory.BANNED_INAPPROPRIATE_BEHAVIOR, "details"))
                    .isInstanceOf(AttributeUnchangedException.class);
        }

        @Test
        void succès_désactiveLesBonsChamps() {
            user.setEnabled(true);
            when(userRepository.save(any())).thenReturn(user);

            userService.adminDeactivateUser(
                    user, admin, AdminDeactivationCategory.BANNED_INAPPROPRIATE_BEHAVIOR, "comportement");

            assertThat(user.isEnabled()).isFalse();
            assertThat(user.getAdminDeactivatedAt()).isNotNull();
            assertThat(user.getAdminDeactivationReason())
                    .isEqualTo(AdminDeactivationCategory.BANNED_INAPPROPRIATE_BEHAVIOR);
            assertThat(user.getAdminDeactivationDetails()).isEqualTo("comportement");
            assertThat(user.getAdminDeactivatedBy()).isSameAs(admin);
        }
    }

    // =========================================================================
    // adminReactivateUser
    // =========================================================================

    @Nested
    class AdminReactivateUser {

        @Test
        void déjàActif_leveAttributeUnchangedException() {
            user.setEnabled(true);
            user.setEverActivated(true);
            assertThatThrownBy(() -> userService.adminReactivateUser(user, admin))
                    .isInstanceOf(AttributeUnchangedException.class);
        }

        @Test
        void jamaisActivé_leveIllegalStateException() {
            user.setEnabled(false);
            user.setEverActivated(false);
            assertThatThrownBy(() -> userService.adminReactivateUser(user, admin))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void nonÉligible_leveUserPermissionException() {
            user.setEnabled(false);
            user.setEverActivated(true);
            when(reactivationPolicyService.checkEligibility(user, admin))
                    .thenReturn(ReactivationEligibility.insufficientPermissions("admin requis"));
            assertThatThrownBy(() -> userService.adminReactivateUser(user, admin))
                    .isInstanceOf(UserPermissionException.class);
        }

        @Test
        void succès_activeEtEffaceLesChampsDeSelfDésactivation() {
            user.setEnabled(false);
            user.setEverActivated(true);
            user.setDeactivationReason(DeactivationReason.PRIVACY_CONCERNS);

            when(reactivationPolicyService.checkEligibility(user, admin))
                    .thenReturn(ReactivationEligibility.eligible());
            when(reactivationPolicyService.determineReactivationPolicy(user, admin))
                    .thenReturn(ReactivationPolicy.ADMIN_ONLY);
            when(userRepository.save(any())).thenReturn(user);

            userService.adminReactivateUser(user, admin);

            assertThat(user.isEnabled()).isTrue();
            assertThat(user.getReactivatedBy()).isSameAs(admin);
            assertThat(user.getDeactivationReason()).isNull();
        }

        @Test
        void succès_effaceÉgalementLesChampsDAdminSiDésactivéParAdmin() {
            user.setEnabled(false);
            user.setEverActivated(true);
            user.setAdminDeactivationReason(AdminDeactivationCategory.BANNED_INAPPROPRIATE_BEHAVIOR);
            user.setAdminDeactivationDetails("spam");

            when(reactivationPolicyService.checkEligibility(user, admin))
                    .thenReturn(ReactivationEligibility.eligible());
            when(reactivationPolicyService.determineReactivationPolicy(user, admin))
                    .thenReturn(ReactivationPolicy.ADMIN_ONLY);
            when(userRepository.save(any())).thenReturn(user);

            userService.adminReactivateUser(user, admin);

            assertThat(user.getAdminDeactivationReason()).isNull();
            assertThat(user.getAdminDeactivationDetails()).isNull();
        }
    }

    // =========================================================================
    // deleteUser
    // =========================================================================

    @Nested
    class DeleteUser {

        @Test
        void utilisateurNonTrouvé_leveUserNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void succès_appelleRepositoryDelete() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            userService.deleteUser(1L);

            verify(userRepository).delete(user);
        }
    }

    // =========================================================================
    // checkIfUserExists / existsByUsername / existsByEmail
    // =========================================================================

    @Nested
    class CheckIfUserExists {

        @Test
        void usernameExistant_leveUsernameAlreadyTakenException() {
            when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);
            assertThatThrownBy(() -> userService.checkIfUserExists(user))
                    .isInstanceOf(UsernameAlreadyTakenException.class);
        }

        @Test
        void emailExistant_leveEmailAlreadyUsedException() {
            when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
            when(userRepository.existsByEmailIgnoreCase("alice@test.com")).thenReturn(true);
            assertThatThrownBy(() -> userService.checkIfUserExists(user))
                    .isInstanceOf(EmailAlreadyUsedException.class);
        }

        @Test
        void aucunConflict_neLevePasException() {
            when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(false);
            when(userRepository.existsByEmailIgnoreCase("alice@test.com")).thenReturn(false);
            // aucune exception
            userService.checkIfUserExists(user);
        }

        @Test
        void existsByUsername_délègueAuRepository() {
            when(userRepository.existsByUsernameIgnoreCase("alice")).thenReturn(true);
            assertThat(userService.existsByUsername("alice")).isTrue();
        }

        @Test
        void existsByEmail_délègueAuRepository() {
            when(userRepository.existsByEmailIgnoreCase("alice@test.com")).thenReturn(false);
            assertThat(userService.existsByEmail("alice@test.com")).isFalse();
        }
    }

    // =========================================================================
    // isAnonymous / authenticatedHasRole
    // =========================================================================

    @Nested
    class AuthenticationContext {

        @Test
        void isAnonymous_quandAuthNull_retourneTrue() {
            SecurityContextHolder.clearContext();
            assertThat(userService.isAnonymous()).isTrue();
        }

        @Test
        void isAnonymous_quandPrincipalAnonymousUser_retourneTrue() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getPrincipal()).thenReturn("anonymousUser");
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThat(userService.isAnonymous()).isTrue();
        }

        @Test
        void isAnonymous_quandAuthentifié_retourneFalse() {
            Authentication auth = mock(Authentication.class);
            when(auth.isAuthenticated()).thenReturn(true);
            when(auth.getPrincipal()).thenReturn(user);
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThat(userService.isAnonymous()).isFalse();
        }

        @Test
        @SuppressWarnings("unchecked")
        void authenticatedHasRole_avecRolePrésent_retourneTrue() {
            Authentication auth = mock(Authentication.class);
            Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ADMIN"));
            doReturn(authorities).when(auth).getAuthorities();
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThat(userService.authenticatedHasRole(UserRole.ADMIN)).isTrue();
        }

        @Test
        @SuppressWarnings("unchecked")
        void authenticatedHasRole_avecRoleAbsent_retourneFalse() {
            Authentication auth = mock(Authentication.class);
            Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("USER"));
            doReturn(authorities).when(auth).getAuthorities();
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertThat(userService.authenticatedHasRole(UserRole.ADMIN)).isFalse();
        }
    }
}

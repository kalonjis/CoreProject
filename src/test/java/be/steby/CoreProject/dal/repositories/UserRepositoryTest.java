package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dal.specifications.UserSpecification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.utils.TestJpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    // alice → USER | bob → ADMIN | charlie → SUPER_ADMIN
    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        alice = em.persistAndFlush(
                new User("alice", "Alice", "Dupont", "alice@example.com", "0611111111", "hashed", Set.of(UserRole.USER)));
        bob = em.persistAndFlush(
                new User("bobAdmin", "Bob", "Martin", "bob@example.com", "0622222222", "hashed", Set.of(UserRole.ADMIN)));
        charlie = em.persistAndFlush(
                new User("charlie", "Charlie", "Super", "charlie@example.com", "0633333333", "hashed", Set.of(UserRole.SUPER_ADMIN)));
        em.clear();
    }

    // =========================================================================
    // findByUsernameOrEmail
    // =========================================================================

    @Nested
    class FindByUsernameOrEmail {

        @Test
        void trouvéParUsername() {
            Optional<User> result = userRepository.findByUsernameOrEmail("alice");
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("alice");
        }

        @Test
        void trouvéParEmail() {
            Optional<User> result = userRepository.findByUsernameOrEmail("bob@example.com");
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("bobAdmin");
        }

        @Test
        void username_caseInsensitive() {
            Optional<User> result = userRepository.findByUsernameOrEmail("ALICE");
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("alice");
        }

        @Test
        void email_caseInsensitive() {
            Optional<User> result = userRepository.findByUsernameOrEmail("CHARLIE@EXAMPLE.COM");
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("charlie");
        }

        @Test
        void nonTrouvé_retourneEmpty() {
            assertThat(userRepository.findByUsernameOrEmail("inconnu")).isEmpty();
        }
    }

    // =========================================================================
    // countUsersWithAdminRoles
    // =========================================================================

    @Nested
    class CountUsersWithAdminRoles {

        @Test
        void compteAdminEtSuperAdmin_excluUser() {
            // bob = ADMIN, charlie = SUPER_ADMIN, alice = USER (excluded)
            assertThat(userRepository.countUsersWithAdminRoles()).isEqualTo(2L);
        }

        @Test
        void utilisateurAvecPlusieursRolesNonCompteDouble() {
            em.persistAndFlush(
                    new User("multi", "Multi", "Role", "multi@example.com", "0699999999", "hashed",
                            Set.of(UserRole.USER, UserRole.ADMIN)));
            em.clear();
            assertThat(userRepository.countUsersWithAdminRoles()).isEqualTo(3L);
        }
    }

    // =========================================================================
    // searchByMultipleFields (@Query)
    // =========================================================================

    @Nested
    class SearchByMultipleFields {

        @Test
        void correspondanceSurUsername() {
            Page<User> result = userRepository.searchByMultipleFields("alice", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
        }

        @Test
        void correspondanceSurLastname_caseInsensitive() {
            Page<User> result = userRepository.searchByMultipleFields("MARTIN", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("bobAdmin");
        }

        @Test
        void correspondanceSurEmail() {
            Page<User> result = userRepository.searchByMultipleFields("charlie@", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("charlie");
        }

        @Test
        void correspondanceSurTéléphone() {
            Page<User> result = userRepository.searchByMultipleFields("0622", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("bobAdmin");
        }

        @Test
        void termeCommun_retourneTous() {
            // "example" apparaît dans tous les emails
            Page<User> result = userRepository.searchByMultipleFields("example", PageRequest.of(0, 10));
            assertThat(result.getTotalElements()).isEqualTo(3L);
        }

        @Test
        void aucuneCorrespondance_retournePageVide() {
            Page<User> result = userRepository.searchByMultipleFields("zzznomatch", PageRequest.of(0, 10));
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        void paginationRespectée() {
            Page<User> page0 = userRepository.searchByMultipleFields("example", PageRequest.of(0, 2));
            Page<User> page1 = userRepository.searchByMultipleFields("example", PageRequest.of(1, 2));
            assertThat(page0.getContent()).hasSize(2);
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page0.getTotalElements()).isEqualTo(3L);
        }
    }

    // =========================================================================
    // searchByCriteria (@Query)
    // =========================================================================

    @Nested
    class SearchByCriteria {

        @Test
        void tousNulls_retourneTous() {
            Page<User> result = userRepository.searchByCriteria(null, null, null, null, null, PageRequest.of(0, 10));
            assertThat(result.getTotalElements()).isEqualTo(3L);
        }

        @Test
        void filtreUsernameSeul() {
            Page<User> result = userRepository.searchByCriteria("alice", null, null, null, null, PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
        }

        @Test
        void filtreLastname_caseInsensitive() {
            Page<User> result = userRepository.searchByCriteria(null, null, "SUPER", null, null, PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("charlie");
        }

        @Test
        void filtresCombiné_ET_logique() {
            Page<User> result = userRepository.searchByCriteria(null, "Bob", "Martin", null, null, PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("bobAdmin");
        }

        @Test
        void filtresContradictoires_retourneVide() {
            // alice comme username, Martin comme lastname → aucun user ne correspond
            Page<User> result = userRepository.searchByCriteria("alice", null, "Martin", null, null, PageRequest.of(0, 10));
            assertThat(result.getContent()).isEmpty();
        }
    }

    // =========================================================================
    // UserSpecification.searchInAllFields
    // =========================================================================

    @Nested
    class SpecSearchInAllFields {

        @Test
        void termNull_aucunFiltre_retourneTous() {
            List<User> result = userRepository.findAll(UserSpecification.searchInAllFields(null));
            assertThat(result).hasSize(3);
        }

        @Test
        void termVide_aucunFiltre_retourneTous() {
            List<User> result = userRepository.findAll(UserSpecification.searchInAllFields(""));
            assertThat(result).hasSize(3);
        }

        @Test
        void correspondancePartielle_username() {
            List<User> result = userRepository.findAll(UserSpecification.searchInAllFields("alice"));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("alice");
        }

        @Test
        void correspondanceSurFirstname() {
            List<User> result = userRepository.findAll(UserSpecification.searchInAllFields("Char"));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("charlie");
        }

        @Test
        void caseInsensitive() {
            List<User> result = userRepository.findAll(UserSpecification.searchInAllFields("BOB"));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("bobAdmin");
        }

        @Test
        void aucuneCorrespondance_retourneVide() {
            List<User> result = userRepository.findAll(UserSpecification.searchInAllFields("zzznomatch"));
            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // UserSpecification.searchByCriteria
    // =========================================================================

    @Nested
    class SpecSearchByCriteria {

        @Test
        void tousNulls_aucunFiltre_retourneTous() {
            List<User> result = userRepository.findAll(UserSpecification.searchByCriteria(null, null, null, null, null));
            assertThat(result).hasSize(3);
        }

        @Test
        void filtreUsernameSeul() {
            List<User> result = userRepository.findAll(UserSpecification.searchByCriteria("alice", null, null, null, null));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("alice");
        }

        @Test
        void filtresCombiné_ET_logique() {
            List<User> result = userRepository.findAll(UserSpecification.searchByCriteria("bobAdmin", "Bob", null, null, null));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("bobAdmin");
        }

        @Test
        void filtresContradictoires_retourneVide() {
            List<User> result = userRepository.findAll(UserSpecification.searchByCriteria("alice", "Bob", null, null, null));
            assertThat(result).isEmpty();
        }

        @Test
        void filtreParTéléphone() {
            List<User> result = userRepository.findAll(UserSpecification.searchByCriteria(null, null, null, null, "0633"));
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUsername()).isEqualTo("charlie");
        }
    }
}

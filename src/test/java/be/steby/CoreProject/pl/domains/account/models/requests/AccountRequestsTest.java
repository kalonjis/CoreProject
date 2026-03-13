package be.steby.CoreProject.pl.domains.account.models.requests;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.common.services.validation.password.PasswordPolicyService;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.pl.domains.emailaddress.validators.ValidEmailDomainValidator;
import be.steby.CoreProject.pl.domains.password.validators.StrongPasswordValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for account request DTOs.
 *
 * <p>Uses a custom ConstraintValidatorFactory to inject mocked services into
 * service-dependent validators (@ValidEmailDomain, @StrongPassword), avoiding
 * the need for a Spring context.
 *
 * <p>Custom validators (@ValidEmailDomain, @StrongPassword) are tested in their
 * own test classes. Here they are stubbed to pass by default so each test can
 * focus on a single constraint.
 */
@ExtendWith(MockitoExtension.class)
class AccountRequestsTest {

    @Mock
    private EmailPolicyService emailPolicyService;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        // Custom validators pass by default — lenient() because some tests use values
        // that short-circuit before the service is called (e.g. blank email/password)
        lenient().when(emailPolicyService.validateEmail(anyString())).thenReturn(EmailValidationResult.valid());
        lenient().when(passwordPolicyService.validatePassword(anyString())).thenReturn(PasswordValidationResult.valid());
        validator = buildValidator(emailPolicyService, passwordPolicyService);
    }

    /**
     * Builds a Jakarta Validator that injects mocked services into the custom validators.
     * Standard Bean Validation validators are instantiated normally via reflection.
     */
    private Validator buildValidator(EmailPolicyService emailSvc, PasswordPolicyService pwdSvc) {
        return Validation.byDefaultProvider()
                .configure()
                .constraintValidatorFactory(new ConstraintValidatorFactory() {
                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        if (key == ValidEmailDomainValidator.class)
                            return key.cast(new ValidEmailDomainValidator(emailSvc));
                        if (key == StrongPasswordValidator.class)
                            return key.cast(new StrongPasswordValidator(pwdSvc));
                        try {
                            return key.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException("Cannot instantiate validator: " + key, e);
                        }
                    }

                    @Override
                    public void releaseInstance(ConstraintValidator<?, ?> instance) {
                    }
                })
                .buildValidatorFactory()
                .getValidator();
    }

    private <T> Set<String> messages(T obj) {
        return validator.validate(obj).stream()
                .map(v -> v.getMessage())
                .collect(Collectors.toSet());
    }

    // =========================================================================
    // SignupRequest
    // =========================================================================

    @Nested
    class SignupRequestTest {

        private SignupRequest valid() {
            return new SignupRequest("alice_42", "alice@example.com", "Str0ng!Pass", "Str0ng!Pass");
        }

        @Test
        void requêteValide_aucuneViolation() {
            assertThat(validator.validate(valid())).isEmpty();
        }

        // --- username ---

        @Test
        void username_vide_violation() {
            assertThat(messages(new SignupRequest("", "alice@example.com", "Str0ng!Pass", "Str0ng!Pass")))
                    .contains("Username is required");
        }

        @Test
        void username_tropCourt_violation() {
            assertThat(messages(new SignupRequest("ab", "alice@example.com", "Str0ng!Pass", "Str0ng!Pass")))
                    .contains("Username must be between 3 and 50 characters");
        }

        @Test
        void username_tropLong_violation() {
            assertThat(messages(new SignupRequest("a".repeat(51), "alice@example.com", "Str0ng!Pass", "Str0ng!Pass")))
                    .contains("Username must be between 3 and 50 characters");
        }

        @Test
        void username_caractèresInvalides_violation() {
            assertThat(messages(new SignupRequest("alice-42", "alice@example.com", "Str0ng!Pass", "Str0ng!Pass")))
                    .contains("Username can only contain letters, numbers, and underscores");
        }

        // --- email ---

        @Test
        void email_vide_violation() {
            assertThat(messages(new SignupRequest("alice_42", "", "Str0ng!Pass", "Str0ng!Pass")))
                    .contains("Email is required");
        }

        @Test
        void email_tropLong_violation() {
            assertThat(messages(new SignupRequest("alice_42", "a".repeat(95) + "@x.com", "Str0ng!Pass", "Str0ng!Pass")))
                    .contains("Email cannot exceed 100 characters");
        }

        @Test
        void email_rejetéParPolicyService_violation() {
            when(emailPolicyService.validateEmail(anyString()))
                    .thenReturn(EmailValidationResult.invalid("Domain not allowed"));
            assertThat(messages(new SignupRequest("alice_42", "alice@banned.com", "Str0ng!Pass", "Str0ng!Pass")))
                    .contains("Domain not allowed");
        }

        // --- password ---

        @Test
        void password_vide_violation() {
            assertThat(messages(new SignupRequest("alice_42", "alice@example.com", "", ""))).isNotEmpty();
        }

        @Test
        void password_rejetéParPolicyService_violation() {
            when(passwordPolicyService.validatePassword(anyString()))
                    .thenReturn(PasswordValidationResult.invalid("Password too weak"));
            assertThat(messages(new SignupRequest("alice_42", "alice@example.com", "weak", "weak")))
                    .contains("Password too weak");
        }

        // --- isPasswordsMatch ---

        @Test
        void motsDePasse_identiques_isPasswordsMatchRetourneTrue() {
            assertThat(valid().isPasswordsMatch()).isTrue();
        }

        @Test
        void motsDePasse_différents_violation() {
            assertThat(messages(new SignupRequest("alice_42", "alice@example.com", "Str0ng!Pass", "Different1!")))
                    .contains("Passwords do not match");
        }

        @Test
        void isPasswordsMatch_passwordNull_retourneFalse() {
            assertThat(new SignupRequest("alice_42", "alice@example.com", null, "anything").isPasswordsMatch())
                    .isFalse();
        }

        // --- toBllModel ---

        @Test
        void toBllModel_mappeCorrectement() {
            var bll = valid().toBllModel();
            assertThat(bll.username()).isEqualTo("alice_42");
            assertThat(bll.email()).isEqualTo("alice@example.com");
            assertThat(bll.password()).isEqualTo("Str0ng!Pass");
        }
    }

    // =========================================================================
    // DeactivateAccountRequest
    // =========================================================================

    @Nested
    class DeactivateAccountRequestTest {

        private DeactivateAccountRequest valid() {
            return new DeactivateAccountRequest(
                    DeactivationReason.TAKING_A_BREAK,
                    "Je prends une pause bien méritée."
            );
        }

        @Test
        void requêteValide_aucuneViolation() {
            assertThat(validator.validate(valid())).isEmpty();
        }

        @Test
        void reason_null_violation() {
            assertThat(messages(new DeactivateAccountRequest(null, "Je prends une pause bien méritée.")))
                    .contains("Deactivation reason is required");
        }

        @Test
        void reasonDetails_vide_violation() {
            assertThat(messages(new DeactivateAccountRequest(DeactivationReason.OTHER, "")))
                    .contains("Please provide details about your deactivation reason");
        }

        @Test
        void reasonDetails_tropCourt_violation() {
            // 9 characters — below the min=10 threshold
            assertThat(messages(new DeactivateAccountRequest(DeactivationReason.OTHER, "a".repeat(9))))
                    .contains("Reason details must be between 10 and 500 characters");
        }

        @Test
        void reasonDetails_tropLong_violation() {
            assertThat(messages(new DeactivateAccountRequest(DeactivationReason.OTHER, "a".repeat(501))))
                    .contains("Reason details must be between 10 and 500 characters");
        }

        @Test
        void toBusiness_mappeCorrectement() {
            var bll = valid().toBusiness();
            assertThat(bll.deactivationReason()).isEqualTo(DeactivationReason.TAKING_A_BREAK);
            assertThat(bll.reasonDetails()).isEqualTo("Je prends une pause bien méritée.");
        }
    }

    // =========================================================================
    // ReactivateAccountRequest
    // =========================================================================

    @Nested
    class ReactivateAccountRequestTest {

        @Test
        void requêteValide_aucuneViolation() {
            assertThat(validator.validate(new ReactivateAccountRequest("alice_42"))).isEmpty();
        }

        @Test
        void identifier_vide_violation() {
            assertThat(messages(new ReactivateAccountRequest("")))
                    .contains("Email or username is required");
        }

        @Test
        void identifier_tropLong_violation() {
            assertThat(messages(new ReactivateAccountRequest("a".repeat(101))))
                    .contains("Email or username cannot exceed 100 characters");
        }

        @Test
        void normalizedIdentifier_convertitEnMinuscules() {
            assertThat(new ReactivateAccountRequest("ALICE@EXAMPLE.COM").toBusiness().identifier())
                    .isEqualTo("alice@example.com");
        }

        @Test
        void normalizedIdentifier_supprimeLesEspaces() {
            assertThat(new ReactivateAccountRequest("  alice  ").toBusiness().identifier())
                    .isEqualTo("alice");
        }

        @Test
        void toBusiness_mappeCorrectement() {
            assertThat(new ReactivateAccountRequest("Alice_42").toBusiness().identifier())
                    .isEqualTo("alice_42");
        }
    }
}

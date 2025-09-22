// src/test/java/be/steby/CoreProject/pl/models/auth/LoginFormTest.java
package be.steby.CoreProject.pl.models.auth;

import be.steby.CoreProject.pl.domains.auth.models.requests.LoginRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour LoginForm.
 * On teste ici la validation des données (annotations @NotBlank).
 */
@DisplayName("LoginForm - Tests de validation")
class LoginRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        // On crée un validateur pour tester les annotations de validation
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("LoginForm valide doit passer la validation")
    void validLoginForm_shouldPassValidation() {
        // ARRANGE (Préparer les données)
        LoginRequest loginRequest = new LoginRequest("john_doe", "password123");

        // ACT (Exécuter l'action)
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // ASSERT (Vérifier le résultat)
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Username vide doit échouer la validation")
    void emptyUsername_shouldFailValidation() {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest("", "password123");

        // ACT
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // ASSERT
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("username");
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("ne doit pas être vide");
    }

    @Test
    @DisplayName("Username null doit échouer la validation")
    void nullUsername_shouldFailValidation() {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest(null, "password123");

        // ACT
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // ASSERT
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("Password vide doit échouer la validation")
    void emptyPassword_shouldFailValidation() {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest("john_doe", "");

        // ACT
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // ASSERT
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString())
                .isEqualTo("password");
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("ne doit pas être vide");
    }

    @Test
    @DisplayName("Username et password vides doivent échouer la validation")
    void bothEmpty_shouldFailValidation() {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest("", "");

        // ACT
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // ASSERT
        assertThat(violations).hasSize(2); // 2 erreurs
    }

    @Test
    @DisplayName("Username avec espaces seulement doit échouer")
    void usernameWithOnlySpaces_shouldFailValidation() {
        // ARRANGE
        LoginRequest loginRequest = new LoginRequest("   ", "password123");

        // ACT
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(loginRequest);

        // ASSERT
        assertThat(violations).hasSize(1);
    }
}
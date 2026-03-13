package be.steby.CoreProject.pl.domains.account.validators;

import be.steby.CoreProject.bll.domains.account.models.DeactivationValidationResult;
import be.steby.CoreProject.bll.domains.account.services.DeactivationPolicyService;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.pl.domains.account.models.requests.DeactivateAccountRequest;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for DeactivationRequestValidator.
 *
 * <p>Tests the validator's own logic (null guard, delegation, error propagation,
 * exception handling). Does NOT test DeactivationPolicyService rules — those
 * belong to the BLL service's own tests.
 */
@ExtendWith(MockitoExtension.class)
class DeactivationRequestValidatorTest {

    @Mock
    private DeactivationPolicyService deactivationPolicyService;

    @InjectMocks
    private DeactivationRequestValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private static final DeactivateAccountRequest VALID_REQUEST =
            new DeactivateAccountRequest(DeactivationReason.TAKING_A_BREAK, "Raison valide suffisamment longue.");

    @BeforeEach
    void setUp() {
        // lenient() because NullGuard and RequêteValide tests never add violations
        lenient().when(context.buildConstraintViolationWithTemplate(any())).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    // =========================================================================
    // Null guard
    // =========================================================================

    @Nested
    class NullGuard {

        @Test
        void requêteNull_retourneTrue() {
            assertThat(validator.isValid(null, context)).isTrue();
        }

        @Test
        void requêteNull_nInterrogesPasLeService() {
            validator.isValid(null, context);
            verifyNoInteractions(deactivationPolicyService);
        }
    }

    // =========================================================================
    // Requête valide
    // =========================================================================

    @Nested
    class RequêteValide {

        @Test
        void serviceRetourneValid_retourneTrue() {
            when(deactivationPolicyService.validateDeactivationReason(any()))
                    .thenReturn(DeactivationValidationResult.valid());

            assertThat(validator.isValid(VALID_REQUEST, context)).isTrue();
        }

        @Test
        void serviceRetourneValid_nAjoutePasDeViolation() {
            when(deactivationPolicyService.validateDeactivationReason(any()))
                    .thenReturn(DeactivationValidationResult.valid());

            validator.isValid(VALID_REQUEST, context);

            verify(context, never()).disableDefaultConstraintViolation();
        }
    }

    // =========================================================================
    // Requête invalide
    // =========================================================================

    @Nested
    class RequêteInvalide {

        @Test
        void serviceRetourneInvalid_retourneFalse() {
            when(deactivationPolicyService.validateDeactivationReason(any()))
                    .thenReturn(DeactivationValidationResult.invalid("Raison non autorisée"));

            assertThat(validator.isValid(VALID_REQUEST, context)).isFalse();
        }

        @Test
        void serviceRetourneInvalid_ajouteLaViolationAuContexte() {
            when(deactivationPolicyService.validateDeactivationReason(any()))
                    .thenReturn(DeactivationValidationResult.invalid("Raison non autorisée"));

            validator.isValid(VALID_REQUEST, context);

            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Raison non autorisée");
            verify(violationBuilder).addConstraintViolation();
        }
    }

    // =========================================================================
    // Exception du service
    // =========================================================================

    @Nested
    class ExceptionDuService {

        @Test
        void serviceLeveException_retourneFalse() {
            when(deactivationPolicyService.validateDeactivationReason(any()))
                    .thenThrow(new RuntimeException("Service error"));

            assertThat(validator.isValid(VALID_REQUEST, context)).isFalse();
        }

        @Test
        void serviceLeveException_ajouteMessageGénériqueAuContexte() {
            when(deactivationPolicyService.validateDeactivationReason(any()))
                    .thenThrow(new RuntimeException("Service error"));

            validator.isValid(VALID_REQUEST, context);

            verify(context).disableDefaultConstraintViolation();
            verify(context).buildConstraintViolationWithTemplate("Error during request validation");
        }
    }
}

package be.steby.CoreProject.pl.validators.account;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.bll.domains.account.models.DeactivationValidationResult;
import be.steby.CoreProject.bll.domains.account.services.DeactivationPolicyService;
import be.steby.CoreProject.pl.domains.account.models.requests.DeactivateAccountRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validator pour les demandes de désactivation de compte.
 * Utilise le service DeactivationPolicyService pour appliquer les règles métier.
 */
@RequiredArgsConstructor
@Slf4j
public class DeactivationRequestValidator implements ConstraintValidator<ValidDeactivationRequest, DeactivateAccountRequest> {

    private final DeactivationPolicyService deactivationPolicyService;

    @Override
    public void initialize(ValidDeactivationRequest constraintAnnotation) {
        // Rien à initialiser
    }

    @Override
    public boolean isValid(DeactivateAccountRequest form, ConstraintValidatorContext context) {
        if (form == null) {
            return true; // Let @NotNull handle this
        }

        try {
            // Conversion vers le modèle métier
            DeactivationRequest request = form.toBusiness();

            // Validation via le service métier (validation de base sans utilisateur)
            DeactivationValidationResult result = deactivationPolicyService.validateDeactivationReason(request);

            if (!result.isValid()) {
                // Désactive le message par défaut
                context.disableDefaultConstraintViolation();

                // Ajoute chaque erreur comme violation de contrainte
                result.errors().forEach(error -> {
                    context.buildConstraintViolationWithTemplate(error)
                            .addConstraintViolation();
                });

                log.debug("Validation of deactivation request failed: {}", result.errors());
                return false;
            }

            log.debug("Successful validation of deactivation request");
            return true;

        } catch (Exception e) {
            log.error("Error validating deactivation request", e);

            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Error during request validation")
                    .addConstraintViolation();

            return false;
        }
    }
}

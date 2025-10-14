package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.bll.domains.account.models.DeactivationValidationResult;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class DeactivationPolicyServiceImpl implements DeactivationPolicyService{

    // Self-deactivation configuration properties
    @Value("${security.account-deactivation.allow-admin-deactivation:false}")
    private boolean allowAdminSelfDeactivation;

    @Value("${security.account-deactivation.allow-super-admin-deactivation:false}")
    private boolean allowSuperAdminSelfDeactivation;

    @Value("${security.account-deactivation.allow-moderator-deactivation:true}")
    private boolean allowModeratorSelfDeactivation;

    @Value("${security.account-deactivation.require-details-for-other:true}")
    private boolean requireDetailsForOtherReason;


    @Value("${security.account-deactivation.details-min-length:10}")
    private int detailsMinLength;

    @Value("${security.account-deactivation.details-max-length:500}")
    private int detailsMaxLength;



    /**
     * @param request The deactivation request to validate
     * @param user    The user requesting deactivation
     * @return
     */
    @Override
    public DeactivationValidationResult validateDeactivationRequest(DeactivationRequest request, User user) {
        if (request == null){
            return DeactivationValidationResult.invalid("Deactivation request cannot be null");
        }

        if (user == null){
            return DeactivationValidationResult.invalid("User cannot be null");
        }

        List<String> validationErrors = new ArrayList<>();

        // 1. Validation des règles de base (raison + détails)
        DeactivationValidationResult basicValidation = validateDeactivationReason(request);
        if (!basicValidation.isValid()) {
            validationErrors.addAll(basicValidation.errors());
        }

        // 2. Validation spécifique à l'utilisateur
        validateUserSpecificRules(user, validationErrors);

        // 3. Validation de l'état du compte
        validateAccountState(user, validationErrors);

        return validationErrors.isEmpty()
                ? DeactivationValidationResult.valid()
                : DeactivationValidationResult.invalid(validationErrors);
    }

    /**
     * @param request The deactivation request to validate
     * @return
     */
    @Override
    public DeactivationValidationResult validateDeactivationReason(DeactivationRequest request) {
        if (request == null){
            return DeactivationValidationResult.invalid("Deactivation request cannot be null");
        }

        List<String> validationErrors = new ArrayList<>();

        // reason validation
        if (request.deactivationReason() == null) {
            validationErrors.add("The reason for deactivation is mandatory");
        }

        // details validation
        validateReasonDetails(request, validationErrors);

        return validationErrors.isEmpty()
                ? DeactivationValidationResult.valid()
                : DeactivationValidationResult.invalid(validationErrors);

    }

    /**
     * Valide les détails selon la raison de désactivation
     */
    private void validateReasonDetails(DeactivationRequest request, List<String> validationErrors) {
        String details = request.reasonDetails();
        DeactivationReason reason = request.deactivationReason();

        if (reason == null) {
            return; // Déjà géré dans validateDeactivationReason
        }

        // Détails obligatoires pour certaines raisons
        if (requireDetailsForOtherReason && reason == DeactivationReason.OTHER) {
            if (details == null || details.trim().isEmpty()) {
                validationErrors.add("Les détails sont obligatoires pour la raison 'Autre'");
                return;
            }
        }

        // Validation de la longueur des détails si fournis
        if (details != null && !details.trim().isEmpty()) {
            if (details.length() < detailsMinLength) {
                validationErrors.add("Les détails doivent contenir au moins " + detailsMinLength + " caractères");
            }

            if (details.length() > detailsMaxLength) {
                validationErrors.add("Les détails ne peuvent pas dépasser " + detailsMaxLength + " caractères");
            }

            // Validation du contenu des détails
            if (containsInappropriateContent(details)) {
                validationErrors.add("Les détails contiennent du contenu inapproprié");
            }
        }

        // Validation spécifique par type de raison
        validateSpecificReasonRules(reason, details, validationErrors);
    }

    /**
     * Valide les règles spécifiques à l'utilisateur (rôles, etc.)
     */
    private void validateUserSpecificRules(User user, List<String> validationErrors) {

        UserRole highestRole = user.getHighestRole();

        if (!allowSuperAdminSelfDeactivation && highestRole == UserRole.SUPER_ADMIN) {
            validationErrors.add("PROCÉDURE ADMINISTRATIVE REQUISE : Les super-administrateurs ne peuvent pas se désactiver. " +
                    "Contactez un autre super-administrateur pour gérer la désactivation de votre compte et " +
                    "assurer la transition des responsabilités.");
        } else if (!allowAdminSelfDeactivation && highestRole == UserRole.ADMIN) {
            validationErrors.add("PROCÉDURE ADMINISTRATIVE REQUISE : Les administrateurs ne peuvent pas se désactiver. " +
                    "Contactez un autre administrateur pour gérer la désactivation de votre compte et " +
                    "assurer la transition des responsabilités.");
        } else if (!allowModeratorSelfDeactivation && highestRole == UserRole.MODERATOR) {
            validationErrors.add("PROCÉDURE ADMINISTRATIVE REQUISE : Les modérateurs ne peuvent pas se désactiver. " +
                    "Contactez un administrateur pour gérer la désactivation de votre compte et " +
                    "assurer la transition des responsabilités de modération.");
        }

    }


    /**
     * Valide l'état actuel du compte
     */
    private void validateAccountState(User user, List<String> validationErrors) {
        // Vérifier si le compte est déjà désactivé
        if (!user.isEnabled()) {
            validationErrors.add("Le compte est déjà désactivé");
        }

        // ✅ CORRECTION: Vérifier si l'email est vérifié (pas isEnabled())
        if (!user.isEmailVerified()) {
            validationErrors.add("Impossible de désactiver un compte non confirmé");
        }
    }

    /**
     * Valide les règles spécifiques selon le type de raison
     */
    // ========================================
// EXEMPLE DE RÈGLES SPÉCIFIQUES PAR RAISON
// ========================================
    private void validateSpecificReasonRules(DeactivationReason reason, String details, List<String> validationErrors) {
        switch (reason) {
            case PRIVACY_CONCERNS:
                // Pour les préoccupations de confidentialité, on pourrait exiger plus de détails
                if (details == null || details.trim().length() < 20) {
                    validationErrors.add("Pour des préoccupations de confidentialité, merci de préciser vos inquiétudes (minimum 20 caractères)");
                }
                break;

            case WORK_REQUIREMENTS:
                // Pour les exigences professionnelles, on pourrait proposer des alternatives
                if (details != null && details.toLowerCase().contains("temporaire")) {
                    // Peut-être logger une suggestion pour une suspension temporaire
                    log.info("User mentions temporary work requirement - consider suggesting account suspension instead of deactivation");
                }
                break;

            case TAKING_A_BREAK:
                // Pour une pause, on pourrait suggérer une durée
                if (details != null && !details.toLowerCase().matches(".*\\b(semaine|mois|temporaire|pause)\\b.*")) {
                    // Optionnel: suggérer de préciser la durée
                    log.debug("User taking a break but didn't specify duration in details");
                }
                break;

            case SWITCHING_ACCOUNTS:
                // Pour le changement de compte, on pourrait vérifier s'il y a des données à migrer
                // Cette validation pourrait être plus complexe et vérifier les données utilisateur
                break;

            case OTHER:
                // Déjà géré dans validateReasonDetails - détails obligatoires
                break;

            default:
                // Autres raisons (NOT_USEFUL, TOO_MUCH_TIME, ACCOUNT_CLEANUP) - pas de règles spéciales
                break;
        }
    }

    /**
     * Vérifie si le contenu contient des éléments inappropriés
     */
    private boolean containsInappropriateContent(String content) {
        if (content == null) {
            return false;
        }

        String lowerContent = content.toLowerCase();

        // Liste de mots/expressions à filtrer (à adapter selon vos besoins)
        String[] inappropriateWords = {
                // Ajoutez ici les mots à filtrer
        };

        for (String word : inappropriateWords) {
            if (lowerContent.contains(word.toLowerCase())) {
                log.warn("Contenu inapproprié détecté dans les détails de désactivation");
                return true;
            }
        }

        return false;
    }
}

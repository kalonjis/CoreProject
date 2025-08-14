package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.domains.admin.models.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.emailAddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.emailAddress.services.EmailPolicyService;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implémentation hybride du service de politique administrative.
 * Se concentre sur les validations complexes où il apporte une vraie valeur ajoutée.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPolicyServiceImpl implements AdminPolicyService {

    private final EmailPolicyService emailPolicyService;

    // Configuration via properties
    @Value("${admin.validation.username.min-length:2}")
    private int usernameMinLength;

    @Value("${admin.validation.username.max-length:50}")
    private int usernameMaxLength;

    @Value("${admin.validation.name.min-length:2}")
    private int nameMinLength;

    @Value("${admin.validation.name.max-length:100}")
    private int nameMaxLength;

    @Value("${admin.validation.details.min-length:10}")
    private int detailsMinLength;

    @Value("${admin.validation.details.max-length:500}")
    private int detailsMaxLength;

    /**
     * Validation complexe de création utilisateur.
     * Ici AdminPolicyService apporte une vraie valeur ajoutée.
     */
    @Override
    public AdminValidationResult validateUserCreation(AdminUserCreationRequest request) {
        log.debug("Validation de création utilisateur - username: {}, email: {}",
                request.username(), request.email());

        List<String> errors = new ArrayList<>();

        // 1. Validation des champs texte basiques
        validateUsername(request.username(), errors);
        validateName(request.firstname(), "prénom", errors);
        validateName(request.lastname(), "nom de famille", errors);
        validatePhoneNumber(request.phoneNumber(), errors);

        // 2. ✅ VALEUR AJOUTÉE : Validation email via service existant
        EmailValidationResult emailResult = emailPolicyService.validateEmail(request.email());
        if (!emailResult.isValid()) {
            errors.addAll(emailResult.errors());
        }

        // 3. ✅ VALEUR AJOUTÉE : Validation des rôles (règles de sécurité)
        validateUserRoles(request.userRoles(), errors);

        // 4. ✅ VALEUR AJOUTÉE : Validation du contexte de requête (audit)
        if (request.requestContext() == null) {
            errors.add("Le contexte de requête est requis pour l'audit des opérations");
        }

        log.debug("Validation création utilisateur terminée - {} erreurs", errors.size());
        return errors.isEmpty() ? AdminValidationResult.valid() : AdminValidationResult.invalid(errors);
    }

    /**
     * Validation spécifique aux détails de désactivation.
     * Plus simple et ciblé que la validation complète précédente.
     */
    @Override
    public AdminValidationResult validateDeactivationDetails(AdminDeactivationRequest request) {
        log.debug("Validation détails désactivation - category: {}", request.deactivationCategory());

        List<String> errors = new ArrayList<>();

        // 1. Validation de la catégorie
        validateDeactivationCategory(request.deactivationCategory(), errors);

        // 2. Validation des détails
        validateDeactivationDetailsText(request.adminDeactivationDetails(), errors);

        log.debug("Validation détails désactivation terminée - {} erreurs", errors.size());
        return errors.isEmpty() ? AdminValidationResult.valid() : AdminValidationResult.invalid(errors);
    }

    // ===============================
    // MÉTHODES PRIVÉES DE VALIDATION
    // ===============================

    private void validateUsername(String username, List<String> errors) {
        if (username == null || username.isBlank()) {
            errors.add("Le nom d'utilisateur ne peut pas être vide");
            return;
        }

        String trimmed = username.trim();
        if (trimmed.length() < usernameMinLength) {
            errors.add("Le nom d'utilisateur doit contenir au moins " + usernameMinLength + " caractères");
        }

        if (trimmed.length() > usernameMaxLength) {
            errors.add("Le nom d'utilisateur ne peut pas dépasser " + usernameMaxLength + " caractères");
        }

        // Caractères autorisés : lettres, chiffres, tirets, underscores
        if (!trimmed.matches("^[a-zA-Z0-9_-]+$")) {
            errors.add("Le nom d'utilisateur ne peut contenir que des lettres, chiffres, tirets et underscores");
        }
    }

    private void validateName(String name, String fieldName, List<String> errors) {
        if (name == null || name.isBlank()) {
            errors.add("Le " + fieldName + " ne peut pas être vide");
            return;
        }

        String trimmed = name.trim();
        if (trimmed.length() < nameMinLength) {
            errors.add("Le " + fieldName + " doit contenir au moins " + nameMinLength + " caractères");
        }

        if (trimmed.length() > nameMaxLength) {
            errors.add("Le " + fieldName + " ne peut pas dépasser " + nameMaxLength + " caractères");
        }
    }

    private void validatePhoneNumber(String phoneNumber, List<String> errors) {
        // Téléphone optionnel, validation seulement si fourni
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            String cleaned = phoneNumber.replaceAll("\\s+", "");

            // Format basique : + suivi de 8 à 15 chiffres
            if (!cleaned.matches("^\\+\\d{8,15}$")) {
                errors.add("Le numéro de téléphone doit être au format international (+33123456789)");
            }
        }
    }

    private void validateUserRoles(Set<UserRole> roles, List<String> errors) {
        if (roles == null || roles.isEmpty()) {
            errors.add("Au moins un rôle doit être attribué");
            return;
        }

        // Toujours inclure USER
        if (!roles.contains(UserRole.USER)) {
            errors.add("Le rôle USER est obligatoire pour tout utilisateur");
        }

        // Interdire la création directe de SUPER_ADMIN
        if (roles.contains(UserRole.SUPER_ADMIN)) {
            errors.add("Les super-administrateurs ne peuvent pas être créés via ce formulaire");
        }

        // Cohérence : ADMIN implique MODERATOR
        if (roles.contains(UserRole.ADMIN) && !roles.contains(UserRole.MODERATOR)) {
            errors.add("Le rôle ADMIN nécessite également le rôle MODERATOR");
        }
    }

    private void validateDeactivationCategory(AdminDeactivationCategory category, List<String> errors) {
        if (category == null) {
            errors.add("La catégorie de désactivation est obligatoire");
        }
        // Pas de validation spécifique par catégorie pour rester simple
    }

    private void validateDeactivationDetailsText(String details, List<String> errors) {
        if (details == null || details.isBlank()) {
            errors.add("Les détails de désactivation sont obligatoires");
            return;
        }

        String trimmed = details.trim();
        if (trimmed.length() < detailsMinLength) {
            errors.add("Les détails doivent contenir au moins " + detailsMinLength + " caractères");
        }

        if (trimmed.length() > detailsMaxLength) {
            errors.add("Les détails ne peuvent pas dépasser " + detailsMaxLength + " caractères");
        }

        // Validation basique de contenu inapproprié
        if (containsInappropriateContent(trimmed)) {
            errors.add("Les détails contiennent du contenu inapproprié");
        }
    }

    /**
     * Vérifie si le contenu contient des éléments inappropriés.
     * Validation basique - peut être étendue selon les besoins.
     */
    private boolean containsInappropriateContent(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }

        String lowerContent = content.toLowerCase();

        // Liste basique de mots/patterns inappropriés
        String[] inappropriatePatterns = {
                "<script", "javascript:", "onclick=", "onerror=",
                "eval(", "alert(", "prompt(", "confirm(",
                // Ajoutez d'autres patterns selon vos besoins
        };

        for (String pattern : inappropriatePatterns) {
            if (lowerContent.contains(pattern)) {
                return true;
            }
        }

        return false;
    }
}
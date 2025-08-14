package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.domains.admin.models.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.emailAddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.emailAddress.services.EmailPolicyService;
import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
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
 * Implémentation du service de politique administrative.
 * Se concentre sur les validations complexes où il apporte une vraie valeur ajoutée.
 * Suit le pattern password/emailAddress avec configuration externalisée.
 * ✅ UTILISE UserPermissionService existant pour éviter la redondance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPolicyServiceImpl implements AdminPolicyService {

    private final EmailPolicyService emailPolicyService;
    // ✅ UTILISE le service existant au lieu de créer des Utils redondants
    private final UserPermissionService userPermissionService;

    // Configuration des validations depuis admin.yml
    @Value("${security.admin.validation.username.min-length:2}")
    private int usernameMinLength;

    @Value("${security.admin.validation.username.max-length:50}")
    private int usernameMaxLength;

    @Value("${security.admin.validation.firstname.min-length:2}")
    private int firstnameMinLength;

    @Value("${security.admin.validation.firstname.max-length:100}")
    private int firstnameMaxLength;

    @Value("${security.admin.validation.lastname.min-length:2}")
    private int lastnameMinLength;

    @Value("${security.admin.validation.lastname.max-length:100}")
    private int lastnameMaxLength;

    @Value("${security.admin.validation.phone.min-length:9}")
    private int phoneMinLength;

    @Value("${security.admin.validation.phone.max-length:15}")
    private int phoneMaxLength;

    @Value("${security.admin.validation.phone.pattern:^[0-9]+$}")
    private String phonePattern;

    @Value("${security.admin.validation.details.min-length:10}")
    private int detailsMinLength;

    @Value("${security.admin.validation.details.max-length:500}")
    private int detailsMaxLength;

    // Configuration de l'audit
    @Value("${security.admin.audit.log-all-operations:true}")
    private boolean logAllOperations;

    @Value("${security.admin.operations.deactivation.require-detailed-reason:true}")
    private boolean requireDetailedReason;

    /**
     * Validation complexe de création utilisateur.
     * Ici AdminPolicyService apporte une vraie valeur ajoutée.
     */
    @Override
    public AdminValidationResult validateUserCreation(AdminUserCreationRequest request) {
        if (logAllOperations) {
            log.debug("Validation de création utilisateur - username: {}, email: {}",
                    request.username(), request.email());
        }

        List<String> errors = new ArrayList<>();

        // 1. Validation des champs texte basiques
        validateUsername(request.username(), errors);
        validateFirstname(request.firstname(), errors);
        validateLastname(request.lastname(), errors);
        validatePhoneNumber(request.phoneNumber(), errors);

        // 2. ✅ VALEUR AJOUTÉE : Validation email via service existant
        EmailValidationResult emailResult = emailPolicyService.validateEmail(request.email());
        if (!emailResult.isValid()) {
            errors.addAll(emailResult.errors());
        }

        // 3. ✅ VALEUR AJOUTÉE : Validation des rôles - UTILISE UserPermissionService
        validateUserRoles(request.userRoles(), errors);

        // 4. ✅ VALEUR AJOUTÉE : Validation du contexte de requête (audit)
        if (request.requestContext() == null) {
            errors.add("Le contexte de requête est requis pour l'audit des opérations");
        }

        if (logAllOperations) {
            log.debug("Validation création utilisateur terminée - {} erreurs", errors.size());
        }

        return errors.isEmpty() ? AdminValidationResult.valid() : AdminValidationResult.invalid(errors);
    }

    /**
     * Validation spécifique aux détails de désactivation.
     */
    @Override
    public AdminValidationResult validateDeactivationDetails(AdminDeactivationRequest request) {
        if (logAllOperations) {
            log.debug("Validation détails désactivation - category: {}", request.deactivationCategory());
        }

        List<String> errors = new ArrayList<>();

        // 1. Validation de la catégorie
        validateDeactivationCategory(request.deactivationCategory(), errors);

        // 2. Validation des détails
        validateDeactivationDetailsText(request.adminDeactivationDetails(), errors);

        if (logAllOperations) {
            log.debug("Validation détails désactivation terminée - {} erreurs", errors.size());
        }

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

    private void validateFirstname(String firstname, List<String> errors) {
        if (firstname == null || firstname.isBlank()) {
            errors.add("Le prénom ne peut pas être vide");
            return;
        }

        String trimmed = firstname.trim();
        if (trimmed.length() < firstnameMinLength) {
            errors.add("Le prénom doit contenir au moins " + firstnameMinLength + " caractères");
        }

        if (trimmed.length() > firstnameMaxLength) {
            errors.add("Le prénom ne peut pas dépasser " + firstnameMaxLength + " caractères");
        }
    }

    private void validateLastname(String lastname, List<String> errors) {
        if (lastname == null || lastname.isBlank()) {
            errors.add("Le nom de famille ne peut pas être vide");
            return;
        }

        String trimmed = lastname.trim();
        if (trimmed.length() < lastnameMinLength) {
            errors.add("Le nom de famille doit contenir au moins " + lastnameMinLength + " caractères");
        }

        if (trimmed.length() > lastnameMaxLength) {
            errors.add("Le nom de famille ne peut pas dépasser " + lastnameMaxLength + " caractères");
        }
    }

    private void validatePhoneNumber(String phoneNumber, List<String> errors) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return; // Téléphone optionnel
        }

        String trimmed = phoneNumber.trim();
        if (trimmed.length() < phoneMinLength || trimmed.length() > phoneMaxLength) {
            errors.add(String.format("Le numéro de téléphone doit contenir entre %d et %d caractères",
                    phoneMinLength, phoneMaxLength));
        }

        if (!trimmed.matches(phonePattern)) {
            errors.add("Le numéro de téléphone ne peut contenir que des chiffres");
        }
    }

    // ✅ SIMPLIFIÉ : Validation basique des rôles, UserPermissionService gère le reste
    private void validateUserRoles(Set<UserRole> roles, List<String> errors) {
        // Vérification du rôle de base requis
        if (!roles.contains(UserRole.USER)) {
            errors.add("Le rôle USER est obligatoire pour tout utilisateur");
        }

        // Les autres vérifications de permissions (qui peut attribuer quoi)
        // sont gérées par UserPermissionService dans les services métier

        if (logAllOperations) {
            log.debug("Validation des rôles: {} (permissions vérifiées par UserPermissionService)", roles);
        }
    }

    private void validateDeactivationCategory(AdminDeactivationCategory category, List<String> errors) {
        if (category == null) {
            errors.add("La catégorie de désactivation ne peut pas être null");
        }
    }

    private void validateDeactivationDetailsText(String details, List<String> errors) {
        if (details == null || details.isBlank()) {
            if (requireDetailedReason) {
                errors.add("Les détails de désactivation sont obligatoires");
            }
            return;
        }

        String trimmed = details.trim();
        if (trimmed.length() < detailsMinLength) {
            errors.add("Les détails doivent contenir au moins " + detailsMinLength + " caractères");
        }

        if (trimmed.length() > detailsMaxLength) {
            errors.add("Les détails ne peuvent pas dépasser " + detailsMaxLength + " caractères");
        }
    }
}

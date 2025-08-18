package be.steby.CoreProject.bll.common.services.validation.email;

import be.steby.CoreProject.bll.domains.emailAddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.il.configs.EmailSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailPolicyServiceImpl implements EmailPolicyService {

    private final UserService userService;
    private final EmailSecurityProperties emailSecurityProperties;

    // Pattern regex pour la validation d'email (RFC 5322 simplifié)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @Override
    public EmailValidationResult validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return EmailValidationResult.invalid("L'adresse email ne peut pas être vide");
        }

        List<String> validationErrors = new ArrayList<>();

        // Validation du format
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            validationErrors.add("L'adresse email n'est pas dans un format valide");
        }

        // Validation de la longueur
        if (email.length() > 254) { // RFC 5321 limite
            validationErrors.add("L'adresse email ne peut pas dépasser 254 caractères");
        }

        // Validation du domaine
        String domain = extractDomain(email);
        if (domain != null) {
            log.debug("Validation de l'email '{}' avec domaine '{}'", email, domain);
            log.debug("Domaines blacklistés configurés: {}", emailSecurityProperties.getBlacklistedDomains());

            // Vérifier si le domaine est dans la liste noire
            if (isEmailDomainBlacklisted(email)) {
                validationErrors.add("Le domaine " + domain + " n'est pas autorisé");
                log.warn("Domaine blacklisté détecté: {} pour l'email {}", domain, email);
            }

            // Vérifier si le domaine est autorisé (si la whitelist est activée)
            if (emailSecurityProperties.isRequireDomainWhitelist() && !isEmailDomainAllowed(email)) {
                validationErrors.add("Le domaine " + domain + " n'est pas dans la liste des domaines autorisés");
            }
        }

        // Vérifier la disponibilité
        if (validationErrors.isEmpty() && !isEmailAvailable(email)) {
            validationErrors.add("Cette adresse email est déjà utilisée par un autre utilisateur");
        }

        log.debug("Résultat de validation pour '{}': valide={}, erreurs={}",
                email, validationErrors.isEmpty(), validationErrors);

        return new EmailValidationResult(validationErrors.isEmpty(), validationErrors);
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return !userService.existsByEmail(email);
    }

    @Override
    public boolean isEmailDomainAllowed(String email) {
        Set<String> allowedDomains = emailSecurityProperties.getAllowedDomains();
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return true; // Pas de whitelist configurée
        }

        String domain = extractDomain(email);
        boolean isAllowed = domain != null && allowedDomains.contains(domain.toLowerCase());
        log.debug("Vérification whitelist pour domaine '{}': autorisé={}", domain, isAllowed);
        return isAllowed;
    }

    @Override
    public boolean isEmailDomainBlacklisted(String email) {
        Set<String> blacklistedDomains = emailSecurityProperties.getBlacklistedDomains();
        if (blacklistedDomains == null || blacklistedDomains.isEmpty()) {
            log.debug("Aucun domaine blacklisté configuré");
            return false; // Pas de blacklist configurée
        }

        String domain = extractDomain(email);
        boolean isBlacklisted = domain != null && blacklistedDomains.contains(domain.toLowerCase());
        log.debug("Vérification blacklist pour domaine '{}': blacklisté={}, liste={}",
                domain, isBlacklisted, blacklistedDomains);
        return isBlacklisted;
    }

    /**
     * Extrait le domaine d'une adresse email.
     *
     * @param email L'adresse email
     * @return Le domaine ou null si l'email est invalide
     */
    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return null;
        }

        return parts[1].toLowerCase();
    }
}
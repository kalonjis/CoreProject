package be.steby.CoreProject.bll.domains.emailAddress.services;

import be.steby.CoreProject.bll.domains.emailAddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailPolicyServiceImpl implements EmailPolicyService {

    private final UserService userService;

    @Value("${security.email.allowed-domains:}")
    private Set<String> allowedDomains;

    @Value("${security.email.blacklisted-domains:}")
    private Set<String> blacklistedDomains;

    @Value("${security.email.require-domain-whitelist:false}")
    private boolean requireDomainWhitelist;

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
            // Vérifier si le domaine est dans la liste noire
            if (isEmailDomainBlacklisted(email)) {
                validationErrors.add("Le domaine " + domain + " n'est pas autorisé");
            }

            // Vérifier si le domaine est autorisé (si la whitelist est activée)
            if (requireDomainWhitelist && !isEmailDomainAllowed(email)) {
                validationErrors.add("Le domaine " + domain + " n'est pas dans la liste des domaines autorisés");
            }
        }

        // Vérifier la disponibilité
        if (validationErrors.isEmpty() && !isEmailAvailable(email)) {
            validationErrors.add("Cette adresse email est déjà utilisée par un autre utilisateur");
        }

        return new EmailValidationResult(validationErrors.isEmpty(), validationErrors);
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return !userService.existsByEmail(email);
    }

    @Override
    public boolean isEmailDomainAllowed(String email) {
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return true; // Pas de whitelist configurée
        }

        String domain = extractDomain(email);
        return domain != null && allowedDomains.contains(domain.toLowerCase());
    }

    @Override
    public boolean isEmailDomainBlacklisted(String email) {
        if (blacklistedDomains == null || blacklistedDomains.isEmpty()) {
            return false; // Pas de blacklist configurée
        }

        String domain = extractDomain(email);
        return domain != null && blacklistedDomains.contains(domain.toLowerCase());
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
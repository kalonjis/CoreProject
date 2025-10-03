package be.steby.CoreProject.bll.common.services.validation.email;

import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
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

    // Email validation regex pattern (simplified RFC 5322)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @Override
    public EmailValidationResult validateEmail(String email) {
        if (email == null || email.isBlank()) {
            return EmailValidationResult.invalid("Email address cannot be empty");
        }

        List<String> validationErrors = new ArrayList<>();

        // Format validation
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            validationErrors.add("Email address is not in a valid format");
        }

        // Length validation
        if (email.length() > 254) { // RFC 5321 limit
            validationErrors.add("Email address cannot exceed 254 characters");
        }

        // Domain validation
        String domain = extractDomain(email);
        if (domain != null) {
            log.debug("Validating email '{}' with domain '{}'", email, domain);
            log.debug("Configured blacklisted domains: {}", emailSecurityProperties.getBlacklistedDomains());

            // Check if domain is blacklisted
            if (isEmailDomainBlacklisted(email)) {
                validationErrors.add("The domain " + domain + " is not allowed");
                log.warn("Blacklisted domain detected: {} for email {}", domain, email);
            }

            // Check if domain is whitelisted (if whitelist is enabled)
            if (emailSecurityProperties.isRequireDomainWhitelist() && !isEmailDomainAllowed(email)) {
                validationErrors.add("The domain " + domain + " is not in the list of allowed domains");
            }
        }

        // Check availability
        if (validationErrors.isEmpty() && !isEmailAvailable(email)) {
            validationErrors.add("This email address is already used by another user");
        }

        log.debug("Validation result for '{}': valid={}, errors={}",
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
            return true; // No whitelist configured
        }

        String domain = extractDomain(email);
        boolean isAllowed = domain != null && allowedDomains.contains(domain.toLowerCase());
        log.debug("Whitelist check for domain '{}': allowed={}", domain, isAllowed);
        return isAllowed;
    }

    @Override
    public boolean isEmailDomainBlacklisted(String email) {
        Set<String> blacklistedDomains = emailSecurityProperties.getBlacklistedDomains();
        if (blacklistedDomains == null || blacklistedDomains.isEmpty()) {
            log.debug("No blacklisted domains configured");
            return false; // No blacklist configured
        }

        String domain = extractDomain(email);
        boolean isBlacklisted = domain != null && blacklistedDomains.contains(domain.toLowerCase());
        log.debug("Blacklist check for domain '{}': blacklisted={}, list={}",
                domain, isBlacklisted, blacklistedDomains);
        return isBlacklisted;
    }

    /**
     * Extracts the domain from an email address.
     *
     * @param email The email address
     * @return The domain or null if the email is invalid
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
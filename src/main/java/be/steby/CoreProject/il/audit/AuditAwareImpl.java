package be.steby.CoreProject.il.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditAwareImpl")
public class AuditAwareImpl implements AuditorAware<String> {

    /**
     * Returns the current auditor's name based on the authenticated user.
     *
     * Returns empty Optional for:
     * - No authentication
     * - Anonymous users (self-signup scenario)
     *
     * This ensures createdBy/updatedBy are NULL for self-signup users,
     * making it easy to distinguish from admin-created users.
     *
     * @return Optional of String containing the authenticated username, or empty if anonymous
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // No authentication or anonymous user → return empty (NULL in DB)
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        // Authenticated user → return username
        return Optional.of(authentication.getName());
    }
}
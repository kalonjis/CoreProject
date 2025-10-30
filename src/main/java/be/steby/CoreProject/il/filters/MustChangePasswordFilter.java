package be.steby.CoreProject.il.filters;

import be.steby.CoreProject.bll.domains.auth.exceptions.PasswordChangeRequiredException;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static be.steby.CoreProject.il.utils.SecurityConstants.PUBLIC_ROUTES;

/**
 * Filter that enforces password change requirement for users with mustChangePassword flag.
 *
 * <p>This filter runs AFTER JwtFilter to ensure the user is authenticated first.
 * If a user has mustChangePassword=true, they can ONLY access specific endpoints
 * to change their password or logout.
 *
 * <p>Security flow:
 * 1. JwtFilter authenticates user
 * 2. MustChangePasswordFilter checks mustChangePassword flag
 * 3. If true, only allows access to whitelisted endpoints
 * 4. Otherwise, throws PasswordChangeRequiredException (delegated to ControllerAdvisor)
 *
 * @author Steby Core Team
 * @since 2025-01
 */
@Component
@Order(2) // Execute AFTER JwtFilter (which is Order 1)
@Slf4j
public class MustChangePasswordFilter extends OncePerRequestFilter {

    /**
     * Spring's HandlerExceptionResolver to delegate exception handling to ControllerAdvisor.
     * We specifically use "handlerExceptionResolver" which handles @ControllerAdvice.
     */
    private final HandlerExceptionResolver exceptionResolver;

    /**
     * Constructor with @Qualifier to select the correct HandlerExceptionResolver bean.
     *
     * @param exceptionResolver The HandlerExceptionResolver that processes @ControllerAdvice
     */
    public MustChangePasswordFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    /**
     * Endpoints that users with mustChangePassword=true can access.
     *
     * ✅ CRITICAL: These endpoints allow users to:
     * - Change their password (/api/password/change)
     * - Logout properly (/api/auth/logout)
     * - Check their status (/api/auth/me, /api/auth/status)
     *
     * ⚠️ SECURITY: Keep this list minimal!
     */
    private static final List<String> ALLOWED_ENDPOINTS = List.of(
            "/api/password/change",        // ✅ MUST be accessible to change password
            "/api/auth/logout",             // ✅ Allow proper logout
            "/api/auth/me",                 // ✅ Allow checking own user details
            "/api/auth/status"              // ✅ Allow checking auth status
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Skip filter for public routes (reuses SecurityConstants.PUBLIC_ROUTES)
        if (isPublicRoute(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User user) {

            // Check if user must change password
            if (user.isMustChangePassword()) {
                log.debug("User {} has mustChangePassword=true, checking endpoint access",
                        user.getUsername());

                // Check if current endpoint is allowed
                if (!isEndpointAllowed(requestURI)) {
                    log.warn("User {} attempted to access {} but must change password first",
                            user.getUsername(), requestURI);

                    // 🔥 FIX: Lancer l'exception au lieu de continuer
                    PasswordChangeRequiredException exception = new PasswordChangeRequiredException(
                            "Password change required. Please change your password before accessing this resource."
                    );

                    // Déléguer à ControllerAdvisor via HandlerExceptionResolver
                    exceptionResolver.resolveException(request, response, null, exception);
                    return; // Important: ne pas continuer la chaîne de filtres
                }

                log.debug("User {} accessing allowed endpoint: {}", user.getUsername(), requestURI);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if the request URI matches any public route from SecurityConstants.
     *
     * @param requestURI The request URI
     * @return true if the URI is a public route
     */
    private boolean isPublicRoute(String requestURI) {
        return Arrays.stream(PUBLIC_ROUTES)
                .anyMatch(publicRoute -> {
                    // Handle wildcards (e.g., "/api/account/activate/**")
                    if (publicRoute.endsWith("/**")) {
                        String prefix = publicRoute.substring(0, publicRoute.length() - 3);
                        return requestURI.startsWith(prefix);
                    }
                    // Exact match
                    return requestURI.equals(publicRoute);
                });
    }

    /**
     * Checks if the endpoint is allowed for users with mustChangePassword=true.
     *
     * @param requestURI The request URI
     * @return true if endpoint is allowed
     */
    private boolean isEndpointAllowed(String requestURI) {
        return ALLOWED_ENDPOINTS.stream()
                .anyMatch(requestURI::startsWith);
    }
}
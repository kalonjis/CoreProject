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

// ✅ UPDATED: Import from SecurityRoutesAggregator instead of SecurityConstants
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.PUBLIC_ROUTES;

/**
 * Filter that enforces password change requirement for users with mustChangePassword flag.
 *
 * <p>This filter runs AFTER JwtFilter to ensure the user is authenticated first.
 * If a user has mustChangePassword=true, they can ONLY access specific endpoints
 * to change their password or logout.
 *
 * <p><b>Security Flow:</b>
 * <ol>
 *   <li>JwtFilter authenticates user</li>
 *   <li>MustChangePasswordFilter checks mustChangePassword flag</li>
 *   <li>If true, only allows access to whitelisted endpoints</li>
 *   <li>Otherwise, throws PasswordChangeRequiredException (delegated to ControllerAdvisor)</li>
 * </ol>
 *
 * <p><b>Migration Status:</b>
 * <ul>
 *   <li>✅ PUBLIC_ROUTES: Using SecurityRoutesAggregator</li>
 *   <li>✅ ALLOWED_ENDPOINTS: Hardcoded (specific to password change flow)</li>
 * </ul>
 *
 * @author Steby Core Team
 * @since 2025-01
 * @see be.steby.CoreProject.il.routes.SecurityRoutesAggregator
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
     * <p><b>✅ CRITICAL: These endpoints allow users to:</b>
     * <ul>
     *   <li>Change their password (/api/password/change)</li>
     *   <li>Logout properly (/api/auth/logout)</li>
     *   <li>Check their status (/api/auth/me, /api/auth/status)</li>
     * </ul>
     *
     * <p><b>⚠️ SECURITY: Keep this list minimal!</b>
     * Only endpoints strictly necessary for password change flow should be here.
     *
     * <p><b>Note:</b> These are hardcoded here because they are specific to the
     * password change enforcement flow and not part of general route configuration.
     */
    private static final List<String> ALLOWED_ENDPOINTS = List.of(
            "/api/password/change",      // Change password endpoint
            "/api/password/define",       // Define password (for OAuth users)
            "/api/auth/logout",           // Allow logout
            "/api/auth/me",               // Check user status
            "/api/auth/status"            // Check authentication status
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Skip filter for public routes (they don't require authentication anyway)
        // ✅ UPDATED: Using PUBLIC_ROUTES from SecurityRoutesAggregator
        if (isPublicRoute(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get authenticated user from SecurityContext (set by JwtFilter)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                User user = (User) principal;

                // Check if user must change password
                if (user.isMustChangePassword()) {
                    log.debug("User {} must change password, checking endpoint access", user.getUsername());

                    // Only allow access to whitelisted endpoints
                    if (!isEndpointAllowed(requestURI)) {
                        log.warn("User {} with mustChangePassword=true attempted to access restricted endpoint: {}",
                                user.getUsername(), requestURI);

                        // Create exception
                        PasswordChangeRequiredException exception = new PasswordChangeRequiredException(
                                "Password change required. Please change your password before accessing other resources."
                        );

                        // Delegate to ControllerAdvisor via HandlerExceptionResolver
                        exceptionResolver.resolveException(request, response, null, exception);
                        return; // Important: don't continue the filter chain
                    }

                    log.debug("User {} accessing allowed endpoint: {}", user.getUsername(), requestURI);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Checks if the request URI matches any public route from SecurityRoutesAggregator.
     *
     * <p>Public routes don't require authentication, so this filter doesn't need to
     * process them (mustChangePassword only applies to authenticated users).
     *
     * <p><b>✅ UPDATED:</b> Now uses {@code PUBLIC_ROUTES} from {@link be.steby.CoreProject.il.routes.SecurityRoutesAggregator}
     * instead of the old {@code SecurityConstants}.
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
                    // Handle single wildcard (e.g., "/api/device/*")
                    if (publicRoute.endsWith("/*")) {
                        String prefix = publicRoute.substring(0, publicRoute.length() - 2);
                        return requestURI.startsWith(prefix);
                    }
                    // Exact match
                    return requestURI.equals(publicRoute);
                });
    }

    /**
     * Checks if the endpoint is allowed for users with mustChangePassword=true.
     *
     * <p>Users who must change their password can only access:
     * <ul>
     *   <li>Password change endpoints</li>
     *   <li>Logout endpoint</li>
     *   <li>Status check endpoints</li>
     * </ul>
     *
     * @param requestURI The request URI
     * @return true if endpoint is allowed
     */
    private boolean isEndpointAllowed(String requestURI) {
        return ALLOWED_ENDPOINTS.stream()
                .anyMatch(requestURI::startsWith);
    }
}
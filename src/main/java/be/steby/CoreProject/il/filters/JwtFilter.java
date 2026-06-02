package be.steby.CoreProject.il.filters;

import be.steby.CoreProject.bll.domains.auth.services.jwt.AuthJwtService;
import be.steby.CoreProject.bll.domains.device.services.DeviceAuthenticationService;
import be.steby.CoreProject.bll.domains.device.models.DeviceSecurityResult;
import be.steby.CoreProject.bll.domains.device.utils.DeviceContextProvider;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.dl.entities.Device;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter with enhanced device security validation.
 *
 * This filter validates JWT tokens, performs secure device validation using cache,
 * and sets up Spring Security authentication context for authenticated requests.
 *
 * @author Steby Team
 * @since 2.1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final AuthJwtService authJwtService;
    private final DeviceAuthenticationService deviceAuthenticationService;

    // NO @Value annotations here - we use authJwtService.getAccessTokenCookieName()

    private static final List<String> LOGIN_ENDPOINTS = List.of(
            "/api/auth/initiate-login",
            "/api/auth/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromCookie(request);
        String requestURI = request.getRequestURI();

        // If the user hits a login endpoint with existing auth cookies, clear them
        // so they appear anonymous — required by @PreAuthorize("isAnonymous()") on login methods.
        if (token != null && LOGIN_ENDPOINTS.contains(requestURI)) {
            clearAuthCookies(response);
            filterChain.doFilter(request, response);
            return;
        }

        if (token != null) {
            try {
                // 1. Validate JWT token and extract claims using AuthJwtService
                Claims claims = authJwtService.validateAccessToken(token);

                // 2. Extract security-critical information from claims
                String username = claims.get("username", String.class);
                Long deviceId = claims.get("deviceId", Long.class);
                String deviceFingerprint = claims.get("deviceFingerprint", String.class);

                // 3. Perform secure device validation with cache optimization
                DeviceSecurityResult deviceResult = deviceAuthenticationService
                        .getSecureDeviceFromCacheOrDatabase(deviceId, deviceFingerprint);

                if (!deviceResult.isSuccess()) {
                    log.error("Device security validation failed for device {}: {}",
                            deviceId, deviceResult.getErrorMessage());
                    handleSecurityViolation(response, deviceResult.getErrorMessage());
                    return;
                }

                Device device = deviceResult.getDevice();

                // 4. Apply device-specific security controls
                if (device.isBlacklisted()) {
                    if (requestURI.equals("/api/auth/logout")) {
                        filterChain.doFilter(request, response);
                        return;
                    } else {
                        handleBlacklistedDeviceRequest(response);
                        return;
                    }
                }

                if (device.isLoggedOut()) {
                    handleLoggedOutDeviceRequest(response, device);
                    return;
                }

                // 5. Authenticate user if device validation passed
                UserDetails userDetails = authService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 6. Store validated device and claims for downstream usage
                DeviceContextProvider.setAuthenticatedDevice(request, device);
                request.setAttribute("jwt_claims", claims);

            } catch (Exception e) {
                log.error("JWT processing error: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from HTTP cookie using AuthJwtService for cookie name.
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String cookieName = authJwtService.getAccessTokenCookieName();
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
// =========================================================================
    // SECURITY VIOLATION HANDLERS
    // =========================================================================

    private void handleSecurityViolation(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    private void handleBlacklistedDeviceRequest(HttpServletResponse response) throws IOException {
        clearAuthCookies(response);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Device is blacklisted\", \"code\": \"DEVICE_BLACKLISTED\"}");
    }

    private void handleLoggedOutDeviceRequest(HttpServletResponse response, Device device) throws IOException {
        clearAuthCookies(response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Session expired\", \"code\": \"SESSION_EXPIRED\"}");
    }

    // =========================================================================
    // COOKIE UTILITIES
    // =========================================================================

    /**
     * Clears authentication cookies from the client's browser.
     * Called when a device is blacklisted or disconnected remotely,
     * so the browser doesn't keep sending stale/revoked tokens.
     */
    private void clearAuthCookies(HttpServletResponse response) {
        response.addCookie(buildExpiredCookie(authJwtService.getAccessTokenCookieName()));
        response.addCookie(buildExpiredCookie(authJwtService.getRefreshTokenCookieName()));
    }

    private Cookie buildExpiredCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);          // suppression immédiate par le navigateur
        cookie.setHttpOnly(true);
        cookie.setSecure(true);       // à retirer si tu es en HTTP en dev
        cookie.setPath("/");
        return cookie;
    }
}
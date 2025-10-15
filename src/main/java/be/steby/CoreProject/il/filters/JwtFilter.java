package be.steby.CoreProject.il.filters;

import be.steby.CoreProject.bll.domains.device.services.DeviceAuthenticationService;
import be.steby.CoreProject.bll.domains.device.models.DeviceSecurityResult;
import be.steby.CoreProject.bll.domains.device.utils.DeviceContextProvider;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter with enhanced device security validation.
 * This filter validates JWT tokens, performs secure device validation using cache,
 * and sets up Spring Security authentication context for authenticated requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final DeviceAuthenticationService deviceAuthenticationService;

    @Value("${security.jwt.access-token.name}")
    private String accessTokenCookieName;

    @Value("${security.jwt.refresh-token.name}")
    private String refreshTokenCookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromCookie(request);
        String requestURI = request.getRequestURI();

        if (token != null) {
            try {
                // 1. Validate JWT token and extract claims
                Claims claims = jwtUtil.validateToken(token);

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
                    // Allow logout even with blacklisted device for proper cleanup
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
     * Extracts JWT token from HTTP cookie.
     *
     * @param request HTTP request containing cookies
     * @return JWT token string or null if not found
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (accessTokenCookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Handles security violations with appropriate error response.
     *
     * @param response HTTP response to configure
     * @param errorMessage Specific error message
     * @throws IOException if writing response fails
     */
    private void handleSecurityViolation(HttpServletResponse response, String errorMessage) throws IOException {
        clearSecurityContextAndCookies(response);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"error\":\"security_violation\",\"message\":\"%s\"}",
                errorMessage));
    }

    /**
     * Handles requests from blacklisted devices by returning security error response.
     *
     * @param response HTTP response to configure
     * @throws IOException if writing response fails
     */
    private void handleBlacklistedDeviceRequest(HttpServletResponse response) throws IOException {
        clearSecurityContextAndCookies(response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"device_disconnected\"," +
                        "\"message\":\"This device has been blacklisted for security reasons.\"}"
        );
    }

    /**
     * Handles requests from logged out devices by returning disconnection message.
     *
     * @param response HTTP response to configure
     * @param device The logged out device
     * @throws IOException if writing response fails
     */
    private void handleLoggedOutDeviceRequest(HttpServletResponse response, Device device) throws IOException {
        clearSecurityContextAndCookies(response);

        // Reset device logged out status for future use
        device.setLoggedOut(false);
        device.setLogoutTime(null);
        // Note: Device save will be handled by the service layer

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"device_disconnected\"," +
                        "\"message\":\"You have been logged out from this device. Please log in again.\"}"
        );
    }

    /**
     * Clears Spring Security context and removes authentication cookies.
     *
     * @param response HTTP response to modify cookies
     */
    private void clearSecurityContextAndCookies(HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        deleteAccessTokenCookie(response);
        deleteRefreshTokenCookie(response);
    }

    /**
     * Removes access token cookie by setting it to expire immediately.
     */
    private void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(accessTokenCookieName, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }

    /**
     * Removes refresh token cookie by setting it to expire immediately.
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshTokenCookieName, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }
}
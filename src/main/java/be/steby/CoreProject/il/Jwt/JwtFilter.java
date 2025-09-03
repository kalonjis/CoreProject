package be.steby.CoreProject.il.Jwt;

import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.bll.domains.device.utils.DeviceRequestUtils;
import be.steby.CoreProject.bll.domains.device.utils.DeviceSecurityEvaluator;
import be.steby.CoreProject.dl.entities.Device;
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
 * JWT authentication filter that processes access tokens from HTTP cookies.
 * This filter validates JWT tokens, checks device security status using caching,
 * and sets up Spring Security authentication context for authenticated requests.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final DeviceSecurityEvaluator deviceSecurityEvaluator;

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
                // Validate JWT token and extract claims
                Claims claims = jwtUtil.validateToken(token);
                String username = claims.get("username", String.class);
                Long deviceId = claims.get("deviceId", Long.class);

                // Store JWT claims in request for further processing
                request.setAttribute(DeviceRequestUtils.JWT_CLAIMS_ATTRIBUTE, claims);

                // Check device security status using cached approach
                Device device = deviceSecurityEvaluator.getDevice(deviceId);

                if (device == null) {
                    log.warn("Device not found for ID: {} from token", deviceId);
                    clearSecurityContextAndContinue(request, response, filterChain);
                    return;
                }

                // Store device in request for reuse within this request
                DeviceRequestUtils.setCurrentDevice(request, device);

                // Handle blacklisted devices
                if (device.isBlacklisted()) {
                    // Allow logout even with blacklisted device for proper cleanup
                    if (requestURI.equals("/api/auth/logout")) {
                        log.debug("Allowing logout for blacklisted device {}", deviceId);
                        filterChain.doFilter(request, response);
                        return;
                    } else {
                        log.warn("Rejecting request from blacklisted device {}", deviceId);
                        handleBlacklistedDeviceRequest(response);
                        return;
                    }
                }

                // Handle logged out devices
                if (device.isLoggedOut()) {
                    log.info("Device {} is marked as logged out", deviceId);
                    handleLoggedOutDeviceRequest(response, device);
                    return;
                }

                // Authenticate user if device is valid
                UserDetails userDetails = authService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Successfully authenticated user {} with device {}", username, deviceId);

            } catch (Exception e) {
                log.warn("JWT processing error: {}", e.getMessage());
                // Clear security context on any JWT processing error
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
                "{\"error\":\"device_blacklisted\"," +
                        "\"message\":\"This device has been blacklisted for security reasons.\"}"
        );
    }

    /**
     * Handles requests from logged out devices by returning disconnection message.
     * Also resets the device logout status for future use.
     *
     * @param response HTTP response to configure
     * @param device The logged out device
     * @throws IOException if writing response fails
     */
    private void handleLoggedOutDeviceRequest(HttpServletResponse response, Device device) throws IOException {
        clearSecurityContextAndCookies(response);

        // Reset device logged out status for future use
        // Note: This might need to be moved to a service if transaction management is required
        try {
            device.setLoggedOut(false);
            device.setLogoutTime(null);
            // The device will be updated in the service layer
            log.debug("Reset logout status for device {}", device.getId());
        } catch (Exception e) {
            log.warn("Failed to reset logout status for device {}: {}", device.getId(), e.getMessage());
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"device_disconnected\"," +
                        "\"message\":\"You have been logged out from this device. Please log in again.\"}"
        );
    }

    /**
     * Clears security context and continues filter chain
     */
    private void clearSecurityContextAndContinue(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 FilterChain filterChain) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
    }

    /**
     * Clears Spring Security context and removes authentication cookies.
     *
     * @param response HTTP response to modify cookies
     */
    private void clearSecurityContextAndCookies(HttpServletResponse response) {
        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        // Remove authentication cookies
        deleteAccessTokenCookie(response);
        deleteRefreshTokenCookie(response);
    }

    /**
     * Removes access token cookie by setting it to expire immediately.
     *
     * @param response HTTP response to modify
     */
    private void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie(accessTokenCookieName, "");
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        response.addCookie(accessTokenCookie);
    }

    /**
     * Removes refresh token cookie by setting it to expire immediately.
     *
     * @param response HTTP response to modify
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshTokenCookie = new Cookie(refreshTokenCookieName, "");
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        response.addCookie(refreshTokenCookie);
    }
}
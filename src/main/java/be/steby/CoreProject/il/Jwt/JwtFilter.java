package be.steby.CoreProject.il.Jwt;

import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.dl.entities.Device;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that processes access tokens from HTTP cookies.
 * This filter validates JWT tokens, checks device security status, and sets up
 * Spring Security authentication context for authenticated requests.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final DeviceService deviceService;

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

                // Check device security status
                Device device = deviceService.getDeviceById(deviceId);

                // Handle blacklisted devices
                if (device.isBlacklisted()) {
                    // Allow logout even with blacklisted device for proper cleanup
                    if (requestURI.equals("/api/auth/logout")) {
                        // Continue without authentication to allow logout cleanup
                        filterChain.doFilter(request, response);
                        return;
                    } else {
                        // Reject all other requests from blacklisted devices
                        handleBlacklistedDeviceRequest(response);
                        return;
                    }
                }

                // Handle logged out devices
                if (device.isLoggedOut()) {
                    handleLoggedOutDeviceRequest(response, device);
                    return;
                }

                // Authenticate user if device is valid
                UserDetails userDetails = authService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Store JWT claims in request for further processing
                request.setAttribute("jwt_claims", claims);

            } catch (Exception e) {
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
        deviceService.saveDevice(device);

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
        // Clear Spring Security context
        SecurityContextHolder.clearContext();

        // Remove authentication cookies
        deleteAccessTokenCookie(response);
        deleteRefreshTokenCookie(response);
    }

    /**
     * Removes access token cookie by setting it to expire immediately.
     *
     * @param response HTTP response to add cookie
     */
    private void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(accessTokenCookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately
        response.addCookie(cookie);
    }

    /**
     * Removes refresh token cookie by setting it to expire immediately.
     *
     * @param response HTTP response to add cookie
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshTokenCookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately
        response.addCookie(cookie);
    }
}
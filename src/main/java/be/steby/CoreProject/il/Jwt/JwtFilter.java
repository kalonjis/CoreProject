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

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final DeviceService deviceService;

    @Value("${security.jwt.access-token.name}")
    private String cookieName;

    @Value("${security.jwt.refresh-token.name}")
    private String refreshCookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromCookie(request);

        if (token != null) {
            try {
                Claims claims = jwtUtil.validateToken(token);
                String username = claims.get("username", String.class);
                Long deviceId = claims.get("deviceId", Long.class);

                Device device = deviceService.getDeviceById(deviceId);

                if(device.isBlacklisted()){
                    handleRejectRequest(response);
                    response.getWriter().write("{\"error\":\"device_disconnected\",\"message\":\"\"Ce périphérique a été blacklisté pour des raisons de sécurité.\"\"}");
                    return;
                }

                if(device.isLoggedOut()){
                    handleRejectRequest(response);
                    device.setLoggedOut(false);
                    device.setLogoutTime(null);
                    response.getWriter().write("{\"error\":\"device_disconnected\",\"message\":\"Vous avez été déconnecté de cet appareil. Veuillez vous reconnecter.\"}");
                    return;
                }


                UserDetails userDetails = authService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);

                request.setAttribute("jwt_claims", claims);

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // Nouvelles méthodes pour supprimer les cookies
    private void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(refreshCookieName, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void handleRejectRequest(HttpServletResponse response) {
        // Forcer la déconnexion
        SecurityContextHolder.clearContext();
        // Supprimer les cookies
        deleteAccessTokenCookie(response);
        deleteRefreshTokenCookie(response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
    }
}
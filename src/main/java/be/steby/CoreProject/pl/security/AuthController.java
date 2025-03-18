package be.steby.CoreProject.pl.security;

import be.steby.CoreProject.bll.services.security.AuthService;
import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.bll.services.security.impl.RefreshTokenServiceImpl;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.tokens.RefreshToken;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import be.steby.CoreProject.pl.models.user.ChangeEmailForm;
import be.steby.CoreProject.pl.models.user.UserDTO;
import be.steby.CoreProject.pl.models.user.UserShortDTO;
import be.steby.CoreProject.pl.security.models.LoginForm;
import be.steby.CoreProject.pl.security.models.UserSignupForm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final DeviceService deviceService;

    private static final String COOKIE_PATH = "/api";  // Path unifié pour tous les cookies


    @PostMapping("signup")
    public ResponseEntity<UserShortDTO>signup(@Valid @RequestBody UserSignupForm form){
       User user = authService.signup(form.toEntity());
       String location = "/api/user/" + user.getId();
       return ResponseEntity.created(URI.create(location)).build();
    }


    @PreAuthorize("isAnonymous()")
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody @Valid LoginForm form,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = authService.login(form.username(), form.password());
        Device device = deviceService.detectAndRegisterDevice(request, user, true);
        String accessToken = jwtUtil.generateAccessToken(user, device);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, device);
        String refreshTokenCookie = refreshToken.getId() + "." + refreshToken.getToken();

        addAccessTokenCookie(response, accessToken);
        addRefreshTokenCookie(response, refreshTokenCookie);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            HttpServletResponse response) {
        if (refreshTokenCookie == null) {
            return ResponseEntity.badRequest().body("Refresh Token is required");
        }

        try {
            // Séparer l'ID et le token
            String[] parts = refreshTokenCookie.split("\\.");
            if (parts.length != 2) {
                return ResponseEntity.badRequest().body("Invalid token format");
            }

            Long tokenId = Long.parseLong(parts[0]);
            String tokenValue = parts[1];

            // Vérifier l'ID ET le token
            RefreshToken oldToken = refreshTokenService.verifyToken(tokenId, tokenValue)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

            refreshTokenService.verifyTokenValidity(oldToken);

            // Créer un nouveau refresh token
            RefreshToken newToken = refreshTokenService.rotateToken(oldToken);

            // Générer le nouveau cookie avec ID et token
            String newRefreshTokenCookie = newToken.getId() + "." + newToken.getToken();

            // Générer nouveau access token
            String newAccessToken = jwtUtil.generateAccessToken(newToken.getUser(), newToken.getDevice());

            addAccessTokenCookie(response, newAccessToken);
            addRefreshTokenCookie(response, newRefreshTokenCookie);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    @Transactional
    public ResponseEntity<?> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            HttpServletResponse response) {

        if (refreshTokenCookie != null) {
            try {
                // Séparer l'ID et le token
                String[] parts = refreshTokenCookie.split("\\.");
                if (parts.length == 2) {
                    Long tokenId = Long.parseLong(parts[0]);
                    String tokenValue = parts[1];

                    // Vérifier et révoquer le token
                    refreshTokenService.verifyToken(tokenId, tokenValue)
                            .ifPresent(token -> {
                                token.setRevoked(true);
                                refreshTokenService.saveToken(token);
                                log.info("Token {} révoqué avec succès", tokenId);
                            });
                }
            } catch (NumberFormatException e) {
                log.warn("Format invalide du refresh token cookie: {}", refreshTokenCookie);
            }
        }

        // Supprimer les cookies côté client
        deleteAccessTokenCookie(response);
        deleteRefreshTokenCookie(response);

        return ResponseEntity.noContent().build();
    }

    // region Change-Email

    @PostMapping("change-email-request")
    public ResponseEntity<Void>changeEmailRequest(@Valid @RequestBody ChangeEmailForm form){
        authService.changeEmailRequest(form);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("cancel-email-change")
    public ResponseEntity<Void>cancelEmailChange(@RequestParam String token){
        authService.cancelEmailChange(token);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("change-email-verification")
    public ResponseEntity<Void>changeEmailVerification(@RequestParam String token){
        authService.changeEmailVerification(token);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("change-email-confirmation")
    public ResponseEntity<Void>changeEmailConfirmation(@RequestParam String token){
        authService.confirmEmail(token);
        return ResponseEntity.noContent().build();
    }


    //endregion


    private void addAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(jwtUtil.getAccessTokenCookieName(), token);
        configureCookie(cookie, (int) (jwtUtil.getAccessTokenExpiration() / 1000));
        response.addCookie(cookie);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String tokenId) {
        Cookie cookie = new Cookie(jwtUtil.getRefreshTokenCookieName(), tokenId);
        configureCookie(cookie, refreshTokenService.getRefreshTokenDurationInSeconds());
        response.addCookie(cookie);
    }

    private void deleteAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(jwtUtil.getAccessTokenCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api");  // Utiliser le même path que lors de la création
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(jwtUtil.getRefreshTokenCookieName(), "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api");  // Utiliser le même path que lors de la création
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void configureCookie(Cookie cookie, int maxAge) {
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(maxAge);
    }
}
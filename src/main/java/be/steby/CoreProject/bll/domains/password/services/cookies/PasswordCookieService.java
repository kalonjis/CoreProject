package be.steby.CoreProject.bll.domains.password.services.cookies;

import be.steby.CoreProject.bll.common.services.cookies.BaseCookieService;
import be.steby.CoreProject.bll.domains.password.exceptions.InvalidPasswordResetTokenException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for managing password reset SMS verification cookies.
 *
 * <p>Handles password reset verification token cookie creation, formatting, and deletion.
 * Delegates low-level cookie operations to BaseCookieService.
 *
 * <p>Manages two types of cookies:
 * <ul>
 *   <li>Verification cookie: Contains JWT with hashed SMS code (10 min TTL)</li>
 *   <li>Permission cookie: Grants access to password reset page (15 min TTL)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordCookieService {

    private final BaseCookieService baseCookieService;

    // Verification cookie configuration (for SMS code verification)
    private static final String PASSWORD_RESET_COOKIE_NAME = "password_reset_sms_token";
    private static final int VERIFICATION_COOKIE_MAX_AGE_SECONDS = 600; // 10 minutes

    // Permission cookie configuration (for password reset page access)
    private static final String PASSWORD_RESET_PERMISSION_COOKIE_NAME = "password_reset_permission";
    private static final int PERMISSION_COOKIE_MAX_AGE_SECONDS = 900; // 15 minutes

    // Common cookie configuration
    private static final String COOKIE_PATH = "/";
    private static final boolean SECURE = true; // Always use HTTPS in production

    // ============================================================================
    // VERIFICATION COOKIE METHODS (SMS code verification)
    // ============================================================================

    /**
     * Sets the password reset SMS verification token cookie.
     * HttpOnly and Secure for protection against XSS and man-in-the-middle attacks.
     *
     * @param response HTTP response
     * @param verificationToken JWT password reset verification token
     */
    public void setVerificationCookie(HttpServletResponse response, String verificationToken) {
        if (verificationToken == null || verificationToken.trim().isEmpty()) {
            throw new InvalidPasswordResetTokenException("Password reset verification token cannot be null or empty");
        }

        baseCookieService.setHttpOnlyCookie(
                response,
                PASSWORD_RESET_COOKIE_NAME,
                verificationToken,
                VERIFICATION_COOKIE_MAX_AGE_SECONDS,
                COOKIE_PATH,
                SECURE
        );

        log.debug("Password reset SMS verification cookie set with expiration: {} seconds", VERIFICATION_COOKIE_MAX_AGE_SECONDS);
    }

    /**
     * Clears the password reset SMS verification token cookie.
     * Used after successful verification or on timeout/failure.
     *
     * @param response HTTP response
     */
    public void clearVerificationCookie(HttpServletResponse response) {
        baseCookieService.deleteHttpOnlyCookie(response, PASSWORD_RESET_COOKIE_NAME);
        log.debug("Password reset SMS verification cookie cleared");
    }

    // ============================================================================
    // PERMISSION COOKIE METHODS (password reset page access)
    // ============================================================================

    /**
     * Sets the password reset permission token cookie.
     *
     * <p>This cookie grants the user permission to access the password reset page
     * after successfully verifying their SMS code. It has a longer TTL than the
     * verification cookie to give users time to reset their password.
     *
     * @param response HTTP response
     * @param permissionToken JWT token granting password reset permission
     */
    public void setPasswordResetPermissionCookie(HttpServletResponse response, String permissionToken) {
        if (permissionToken == null || permissionToken.trim().isEmpty()) {
            throw new InvalidPasswordResetTokenException("Password reset permission token cannot be null or empty");
        }

        baseCookieService.setHttpOnlyCookie(
                response,
                PASSWORD_RESET_PERMISSION_COOKIE_NAME,
                permissionToken,
                PERMISSION_COOKIE_MAX_AGE_SECONDS,
                COOKIE_PATH,
                SECURE
        );

        log.debug("Password reset permission cookie set with expiration: {} seconds", PERMISSION_COOKIE_MAX_AGE_SECONDS);
    }

    /**
     * Clears the password reset permission cookie.
     * Used after successful password reset or on timeout/failure.
     *
     * @param response HTTP response
     */
    public void clearPasswordResetPermissionCookie(HttpServletResponse response) {
        baseCookieService.deleteHttpOnlyCookie(response, PASSWORD_RESET_PERMISSION_COOKIE_NAME);
        log.debug("Password reset permission cookie cleared");
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    /**
     * Extracts and validates the verification token from cookie value.
     * Performs basic validation on the token format.
     *
     * @param cookieValue Raw cookie value from request
     * @return The verification token if valid
     * @throws InvalidPasswordResetTokenException if cookie value is invalid
     */
    public String getVerificationToken(String cookieValue) {
        if (cookieValue == null || cookieValue.trim().isEmpty()) {
            throw new InvalidPasswordResetTokenException("Password reset verification token cookie is missing or empty");
        }

        // Basic validation - JWT tokens typically have 3 parts separated by dots
        String[] parts = cookieValue.split("\\.");
        if (parts.length != 3) {
            log.warn("Invalid password reset verification token format received");
            throw new InvalidPasswordResetTokenException("Invalid password reset verification token format");
        }

        log.debug("Password reset verification token extracted from cookie successfully");
        return cookieValue;
    }

    /**
     * Gets the verification cookie name used for SMS code verification.
     * Useful for @CookieValue annotations in controllers.
     *
     * @return The verification cookie name
     */
    public static String getVerificationCookieName() {
        return PASSWORD_RESET_COOKIE_NAME;
    }

    /**
     * Gets the permission cookie name used for password reset page access.
     * Useful for @CookieValue annotations in controllers.
     *
     * @return The permission cookie name
     */
    public static String getPermissionCookieName() {
        return PASSWORD_RESET_PERMISSION_COOKIE_NAME;
    }
}
package be.steby.CoreProject.bll.domains.profile.services.phone;

import be.steby.CoreProject.bll.common.exceptions.phone.InvalidPhoneVerificationTokenException;
import be.steby.CoreProject.bll.common.services.cookies.BaseCookieService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for managing phone verification-related cookies.
 * Handles phone verification token cookie creation, formatting, and deletion.
 * Delegates low-level cookie operations to BaseCookieService.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationCookieService {

    private final BaseCookieService baseCookieService;

    // Cookie configuration
    private static final String PHONE_VERIFICATION_COOKIE_NAME = "phone_verification_token";
    private static final String COOKIE_PATH = "/";
    private static final boolean SECURE = true; // Always use HTTPS in production
    private static final int COOKIE_MAX_AGE_SECONDS = 900; // 15 minutes

    /**
     * Sets the phone verification token cookie.
     * HttpOnly and Secure for protection against XSS and man-in-the-middle attacks.
     * 
     * @param response HTTP response
     * @param verificationToken JWT phone verification token
     */
    public void setVerificationCookie(HttpServletResponse response, String verificationToken) {
        if (verificationToken == null || verificationToken.trim().isEmpty()) {
            throw new InvalidPhoneVerificationTokenException("Verification token cannot be null or empty");
        }

        baseCookieService.setHttpOnlyCookie(
                response,
                PHONE_VERIFICATION_COOKIE_NAME,
                verificationToken,
                COOKIE_MAX_AGE_SECONDS,
                COOKIE_PATH,
                SECURE
        );

        log.debug("Phone verification cookie set with expiration: {} seconds", COOKIE_MAX_AGE_SECONDS);
    }

    /**
     * Clears the phone verification token cookie.
     * Used after successful verification or on timeout/failure.
     * 
     * @param response HTTP response
     */
    public void clearVerificationCookie(HttpServletResponse response) {
        baseCookieService.deleteHttpOnlyCookie(response, PHONE_VERIFICATION_COOKIE_NAME);
        log.debug("Phone verification cookie cleared");
    }

    /**
     * Extracts and validates the verification token from cookie value.
     * Performs basic validation on the token format.
     * 
     * @param cookieValue Raw cookie value from request
     * @return The verification token if valid
     * @throws InvalidPhoneVerificationTokenException if cookie value is invalid
     */
    public String getVerificationToken(String cookieValue) {
        if (cookieValue == null || cookieValue.trim().isEmpty()) {
            throw new InvalidPhoneVerificationTokenException("Phone verification token cookie is missing or empty");
        }

        // Basic validation - JWT tokens typically have 3 parts separated by dots
        String[] parts = cookieValue.split("\\.");
        if (parts.length != 3) {
            log.warn("Invalid phone verification token format received");
            throw new InvalidPhoneVerificationTokenException("Invalid phone verification token format");
        }

        log.debug("Phone verification token extracted from cookie successfully");
        return cookieValue;
    }

    /**
     * Gets the cookie name used for phone verification.
     * Useful for @CookieValue annotations in controllers.
     * 
     * @return The cookie name
     */
    public static String getCookieName() {
        return PHONE_VERIFICATION_COOKIE_NAME;
    }
}
package be.steby.CoreProject.pl.domains.profile.controller;

import be.steby.CoreProject.bll.common.services.phone.PhoneNumberVerificationService;
import be.steby.CoreProject.bll.domains.profile.services.phone.PhoneVerificationCookieService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.profile.models.requests.PhoneVerificationRequest;
import be.steby.CoreProject.pl.domains.profile.models.responses.PhoneVerificationResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for phone number verification operations.
 *
 * Handles the complete phone verification flow:
 * 1. Send verification code to user's phone number
 * 2. Verify the code and mark phone as verified
 *
 * Security:
 * - All endpoints require authentication
 * - Uses JWT tokens in cookies for temporary code storage
 * - Updates user.phoneNumberVerified flag upon successful verification
 *
 * @author Steby Team
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/profile/phone")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class PhoneProfileController {

    private final PhoneNumberVerificationService phoneNumberVerificationService;
    private final PhoneVerificationCookieService phoneVerificationCookieService;
    private final UserService userService;

    /**
     * Request phone number verification.
     * Sends a 6-digit verification code to the user's current phone number.
     *
     * Prerequisites:
     * - User must have a phone number configured
     * - User must be authenticated
     *
     * Process:
     * 1. Validates user has a phone number
     * 2. Generates and sends verification code via SMS
     * 3. Creates JWT token with hashed code
     * 4. Sets secure cookie with verification token
     *
     * Response: 200 OK with masked phone number
     *
     * Possible errors:
     * - 400 Bad Request: User has no phone number configured
     * - 500 Internal Error: SMS sending failed
     *
     * @param user Authenticated user from security context
     * @param httpResponse HTTP response for setting cookies
     * @return ResponseEntity with verification initiation confirmation
     */
    @PostMapping("/request-verification")
    public ResponseEntity<PhoneVerificationResponse> requestVerification(
            @AuthenticationPrincipal User user,
            HttpServletResponse httpResponse) {

        log.info("Phone verification request from user: {}", user.getUsername());

        // 1. Check if user has a phone number
        if (user.getPhoneNumber() == null || user.getPhoneNumber().trim().isEmpty()) {
            log.warn("User {} attempted phone verification without phone number", user.getUsername());
            throw new IllegalArgumentException("No phone number configured for verification");
        }

        // 2. Generate verification code and send SMS
        String verificationToken = phoneNumberVerificationService.generateAndSendVerificationCode(
                user, user.getPhoneNumber());

        // 3. Set secure cookie with verification token
        phoneVerificationCookieService.setVerificationCookie(httpResponse, verificationToken);

        // 4. Return response with masked phone number
        String maskedPhone = maskPhoneNumber(user.getPhoneNumber());

        log.info("Phone verification initiated for user: {}", user.getUsername());
        return ResponseEntity.ok(PhoneVerificationResponse.verificationSent(maskedPhone));
    }


    /**
     * Verify phone number with provided code.
     * Validates the 6-digit code against the stored verification token.
     *
     * Prerequisites:
     * - User must have initiated verification (have valid cookie)
     * - Code must be provided and valid
     *
     * Process:
     * 1. Extracts verification token from cookie
     * 2. Validates the provided code against stored hash
     * 3. Updates user.phoneNumberVerified = true if successful
     * 4. Clears verification cookie
     *
     * Response: 200 OK with verification result
     *
     * Possible errors:
     * - 400 Bad Request: Missing or invalid verification token
     * - 400 Bad Request: Invalid verification code
     * - 404 Not Found: No pending verification found
     *
     * @param request Verification request containing the 6-digit code
     * @param user Authenticated user from security context
     * @param verificationTokenCookie JWT token from verification cookie
     * @param httpResponse HTTP response for clearing cookies
     * @return ResponseEntity with verification result
     */
    @PostMapping("/verify")
    public ResponseEntity<PhoneVerificationResponse> verifyCode(
            @Valid @RequestBody PhoneVerificationRequest request,
            @AuthenticationPrincipal User user,
            @CookieValue(name = "phone_verification_token", required = false) String verificationTokenCookie,
            HttpServletResponse httpResponse) {

        log.info("Phone verification attempt from user: {}", user.getUsername());

        // 1. Check if verification token exists
        if (verificationTokenCookie == null) {
            log.warn("User {} attempted verification without token", user.getUsername());
            throw new IllegalArgumentException("No pending phone verification found");
        }

        // 2. Extract and validate token from cookie
        String verificationToken = phoneVerificationCookieService.getVerificationToken(verificationTokenCookie);

        // 3. Verify the provided code
        boolean isValid = phoneNumberVerificationService.verifyCode(
                verificationToken, request.verificationCode());

        // 4. Clear verification cookie
        phoneVerificationCookieService.clearVerificationCookie(httpResponse);

        if (isValid) {
            // 5. Update user's phone verification status
            user.setPhoneNumberVerified(true);
            userService.saveUser(user);

            log.info("Phone verification successful for user: {}", user.getUsername());
            return ResponseEntity.ok(PhoneVerificationResponse.verificationSuccessful());
        } else {
            log.warn("Phone verification failed for user: {} - invalid code", user.getUsername());
            return ResponseEntity.badRequest()
                    .body(PhoneVerificationResponse.verificationFailed("Invalid verification code"));
        }
    }

    /**
     * Masks phone number for display.
     * Example: +32498567890 -> +3249856XXXX
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "XXXX";
    }


}
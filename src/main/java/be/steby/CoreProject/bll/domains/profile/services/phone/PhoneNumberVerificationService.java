package be.steby.CoreProject.bll.domains.profile.services.phone;

import be.steby.CoreProject.bll.common.exceptions.phone.InvalidPhoneNumberFormatException;
import be.steby.CoreProject.bll.common.exceptions.phone.InvalidPhoneVerificationTokenException;
import be.steby.CoreProject.bll.common.exceptions.phone.PhoneException;
import be.steby.CoreProject.bll.common.services.validation.phone.PhoneNumberFormatValidationService;
import be.steby.CoreProject.bll.domains.profile.events.PhoneVerificationInitiatedEvent;
import be.steby.CoreProject.bll.domains.profile.exceptions.phone.PhoneVerificationFailedException;
import be.steby.CoreProject.bll.domains.profile.models.phone.PhoneVerificationRequestBLL;
import be.steby.CoreProject.bll.domains.profile.models.phone.PhoneVerificationTokenResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Service for phone number verification via SMS.
 * 
 * Handles the complete SMS verification flow:
 * 1. Generate verification code
 * 2. Send SMS with code
 * 3. Store code securely in JWT token
 * 4. Verify provided code against stored code
 * 
 * Uses JWT tokens (like 2FA flow) for temporary storage instead of database/Redis.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneNumberVerificationService {

    private final PhoneNumberFormatValidationService phoneNumberFormatValidationService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserService userService;
    private final ApplicationEventPublisher publisher;


    /**
     * Generates and sends a verification code to the specified phone number.
     * Returns a JWT token containing the hashed code for later verification.
     * 
     * @param phoneNumber The phone number to verify
     * @return JWT token containing verification data
     * @throws InvalidPhoneNumberFormatException if phone number format is invalid
     * @throws PhoneException if SMS sending fails
     */
    public PhoneVerificationTokenResult generateVerificationCode(String phoneNumber) {
        User user = userService.getAuthenticatedUser();

        log.info("Starting SMS verification for user: {} - SMS: {}",
                 user.getUsername(), maskPhoneNumber(phoneNumber));

        // 1. Validate and format phone number
        String formattedPhoneNumber = phoneNumberFormatValidationService.validateAndFormat(phoneNumber);

        // 2. Generate 6-digit verification code
        String verificationCode = generateVerificationCode();
        String hashedCode = passwordEncoder.encode(verificationCode);

        String verificationToken  = jwtUtil.generatePhoneVerificationToken(
                user, formattedPhoneNumber,hashedCode
        );

        PhoneVerificationTokenResult result = new PhoneVerificationTokenResult(verificationToken);

        publisher.publishEvent(
                new PhoneVerificationInitiatedEvent(
                    formattedPhoneNumber,verificationCode
                )
        );

        log.info("Phone verification process initiated for user: {}", user.getUsername());
        return result;
    }

    /**
     * Verifies the provided code against the code stored in the JWT token.
     *
     * @return true if verification successful, false otherwise
     * @throws InvalidPhoneVerificationTokenException if token is invalid or expired
     */
    public void verifyCode(PhoneVerificationRequestBLL request) {
        String token = request.jwtToken();
        String providedCode = request.verificationCode();
        log.debug("Verifying SMS verification code");

        // 1. Validate and extract claims from token using specialized method
        Claims claims = jwtUtil.validatePhoneVerificationToken(token);

        // 2. Extract verification data (already validated by jwtUtil method)
        String userPublicId = claims.get("userPublicId", String.class);
        String phoneNumber = claims.get("phoneNumber", String.class);
        String hashedCode = claims.get("verificationCodeHash", String.class);

        User user = userService.getAuthenticatedUser();

        if(!user.getPublicId().equals(userPublicId)){
            throw new InvalidPhoneVerificationTokenException("this verification isn't related to the authenticated user ");
        }

        if( !passwordEncoder.matches(providedCode, hashedCode) ){
            throw new PhoneVerificationFailedException("Phone verification failed. Please check your code and try again.");
        }

        user.setPhoneNumber(phoneNumber);
        user.setPhoneNumberVerified(true);
        userService.saveUser(user);

    }



    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    /**
     * Generates a secure 6-digit verification code.
     */
    private String generateVerificationCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Masks phone number for security logging.
     * Example: +32498567890 -> +3249856XXXX
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, phoneNumber.length() - 4) + "XXXX";
    }
}
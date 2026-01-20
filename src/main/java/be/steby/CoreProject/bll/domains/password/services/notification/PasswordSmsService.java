package be.steby.CoreProject.bll.domains.password.services.notification;

import be.steby.CoreProject.bll.common.services.notification.sms.BaseSmsService;
import be.steby.CoreProject.bll.domains.password.exceptions.PasswordRequestValidationException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.sms.TwilioSmsSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Password domain-specific SMS service.
 *
 * <p>Extends BaseSmsService to inherit common SMS utilities.
 * Handles all SMS notifications related to password operations:
 * <ul>
 *   <li>Password reset verification codes via SMS</li>
 *   <li>Password reset confirmation notifications</li>
 *   <li>Password change security alerts</li>
 * </ul>
 *
 * <p>All password-related SMS operations are designed to be non-blocking and failure-tolerant.
 * SMS delivery failures should not affect the core password management flow.
 */
@Service
@Slf4j
public class PasswordSmsService extends BaseSmsService {

    private final SecureRandom secureRandom;

    public PasswordSmsService(TwilioSmsSender twilioSmsSender) {
        super(twilioSmsSender);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Sends a pre-generated password reset verification code via SMS.
     *
     * <p>This method sends a provided verification code to the user's
     * verified phone number. Used when the code is already generated
     * and stored elsewhere (e.g., in JWT token).
     *
     * @param user             the user requesting password reset
     * @param verificationCode the pre-generated verification code to send
     * @throws PasswordRequestValidationException if user cannot receive SMS
     */
    @Async("smsExecutor")
    public void sendPasswordResetCode(User user, String verificationCode) {
        log.info("Sending pre-generated password reset SMS code to user: {}", user.getUsername());

        // Validate SMS requirements
        validateSmsRequirements(user);

        // Format and send SMS with provided code
        String message = formatPasswordResetMessage(verificationCode);
        sendSms(message, user.getPhoneNumber());

        log.info("Password reset SMS code sent successfully to user: {} (phone: {}) CODE: {}",
                user.getUsername(), maskPhoneNumber(user.getPhoneNumber()), verificationCode);
    }

    /**
     * Sends password reset success notification via SMS.
     *
     * <p>This security notification is sent when a user successfully resets
     * their password using an SMS code to confirm the action was authorized.
     *
     * @param user the user whose password was reset
     */
    @Async("smsExecutor")
    public void sendPasswordResetSuccessNotification(User user) {
        log.info("Sending password reset success notification SMS to user: {}", user.getUsername());

        if (!hasVerifiedPhoneNumber(user)) {
            log.debug("User {} has no verified phone number - skipping SMS notification", user.getUsername());
            return;
        }

        String message = "Password reset successful. If you did not request this change, contact support immediately.";
        sendSms(message, user.getPhoneNumber());

        log.debug("Password reset success notification SMS sent to user: {}", user.getUsername());
    }

    /**
     * Sends password change alert via SMS.
     *
     * <p>This security notification is sent when a user changes their password
     * to alert them of the change via their registered phone number.
     *
     * @param user the user whose password was changed
     */
    @Async("smsExecutor")
    public void sendPasswordChangeAlert(User user) {
        log.info("Sending password change alert SMS to user: {}", user.getUsername());

        if (!hasVerifiedPhoneNumber(user)) {
            log.debug("User {} has no verified phone number - skipping SMS alert", user.getUsername());
            return;
        }

        String message = "Your account password was changed. If you did not make this change, contact support immediately.";
        sendSms(message, user.getPhoneNumber());

        log.debug("Password change alert SMS sent to user: {}", user.getUsername());
    }

    /**
     * Validates that a user meets SMS requirements for password operations.
     *
     * @param user the user to validate
     * @throws PasswordRequestValidationException if SMS requirements not met
     */
    private void validateSmsRequirements(User user) {
        if (!hasValidPhoneNumber(user)) {
            throw new PasswordRequestValidationException("User has no valid phone number configured");
        }

        if (!hasVerifiedPhoneNumber(user)) {
            throw new PasswordRequestValidationException("User's phone number is not verified");
        }
    }

    /**
     * Generates a secure 6-digit numeric verification code.
     *
     * <p>Uses SecureRandom to generate cryptographically secure codes
     * suitable for SMS verification and password reset operations.
     *
     * <p>The code range is 100000-999999 (inclusive) to ensure
     * all codes are exactly 6 digits with no leading zeros.
     *
     * @return a 6-digit verification code as String
     */
    public String generateVerificationCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Formats the password reset SMS message.
     *
     * @param verificationCode the verification code to include
     * @return formatted SMS message
     */
    private String formatPasswordResetMessage(String verificationCode) {
        return String.format("Your password reset code: %s. This code expires in 10 minutes.", verificationCode);
    }
}
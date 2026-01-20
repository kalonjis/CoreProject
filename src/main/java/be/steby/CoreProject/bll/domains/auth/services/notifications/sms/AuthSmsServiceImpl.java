package be.steby.CoreProject.bll.domains.auth.services.notifications.sms;

import be.steby.CoreProject.bll.common.exceptions.phone.SmsSendingException;
import be.steby.CoreProject.bll.common.services.notification.sms.BaseSmsService;
import be.steby.CoreProject.bll.domains.auth.exceptions.phone.InvalidPhoneNumberException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.utils.SmsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Auth domain-specific SMS service.
 *
 * Handles all SMS notifications related to authentication operations:
 * - 2FA verification codes
 * - Security notifications
 * - 2FA enablement confirmations
 *
 * Provides both async and sync methods:
 * - Async methods: For non-critical notifications (fire-and-forget)
 * - Sync methods: For time-sensitive codes (throws on failure)
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@Slf4j
public class AuthSmsServiceImpl extends BaseSmsService implements AuthSmsService {

    public AuthSmsServiceImpl(SmsUtil smsUtil) {
        super(smsUtil);
    }

    // =========================================================================
    // ASYNC METHODS (fire-and-forget)
    // =========================================================================

    /**
     * Sends 2FA verification code via SMS asynchronously.
     *
     * @param user The user receiving the verification code
     * @param verificationCode 6-digit verification code
     */
    @Override
    @Async("smsExecutor")
    public void sendTwoFactorVerificationCode(User user, String verificationCode) {
        log.info("Sending 2FA verification code via SMS to user: {} (async)", user.getUsername());

        try {
            validatePhoneNumber(user);

            String message = formatVerificationMessage(verificationCode);
            sendSms(message, user.getPhoneNumber());

            log.info("2FA verification SMS queued successfully for user: {}", user.getUsername());

        } catch (Exception e) {
            log.error("Failed to queue 2FA verification SMS for user: {} - Error: {}",
                    user.getUsername(), e.getMessage(), e);
            // Don't rethrow - async method should not fail the main flow
        }
    }

    // =========================================================================
    // SYNC METHODS (blocking, for critical messages)
    // =========================================================================

    /**
     * Sends 2FA verification code via SMS synchronously (blocking).
     *
     * Use this for login 2FA where immediate feedback is required.
     * If delivery fails, throws exception so caller can offer alternatives.
     *
     * @param user The user receiving the verification code
     * @param verificationCode 6-digit verification code
     * @throws SmsSendingException if SMS delivery fails
     * @throws InvalidPhoneNumberException if phone number is not verified
     */
    @Override
    public void sendTwoFactorCodeSync(User user, String verificationCode) throws SmsSendingException {
        log.info("Sending 2FA verification code via SMS to user: {} (sync)", user.getUsername());

        validatePhoneNumber(user);

        String message = formatVerificationMessage(verificationCode);

        // This will throw SmsSendingException if delivery fails
        sendSmsSync(message, user.getPhoneNumber());

        log.info("2FA verification SMS sent successfully to user: {} (sync)", user.getUsername());
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Validates that user has a verified phone number for SMS 2FA.
     *
     * @param user The user to validate
     * @throws InvalidPhoneNumberException if phone number is not valid or not verified
     */
    private void validatePhoneNumber(User user) {
        if (!hasVerifiedPhoneNumber(user)) {
            throw new InvalidPhoneNumberException("Phone number must be verified for SMS 2FA");
        }
    }

    /**
     * Formats the verification code message.
     *
     * @param verificationCode The 6-digit code
     * @return Formatted SMS message
     */
    private String formatVerificationMessage(String verificationCode) {
        return "Your verification code: " + verificationCode;
    }

    /**
     * Sends SMS synchronously using the SMS utility.
     * Delegates to SmsUtil's sync method.
     *
     * @param message The SMS content
     * @param phoneNumber The recipient's phone number
     * @throws SmsSendingException if sending fails
     */
    private void sendSmsSync(String message, String phoneNumber) throws SmsSendingException {
        smsUtil.sendSmsSync(message, phoneNumber);
    }
}
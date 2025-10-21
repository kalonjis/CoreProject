package be.steby.CoreProject.bll.domains.auth.services.phone;

import be.steby.CoreProject.bll.common.services.phone.BasePhoneService;
import be.steby.CoreProject.bll.domains.auth.exceptions.phone.InvalidPhoneNumberException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.il.utils.PhoneUtil;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Auth domain-specific phone service.
 * Handles all SMS notifications related to authentication operations:
 * - 2FA verification codes
 * - Security notifications
 * - 2FA enablement confirmations
 */
@Service
@Slf4j
public class AuthPhoneServiceImpl extends BasePhoneService implements AuthPhoneService {

    public AuthPhoneServiceImpl(PhoneUtil phoneUtil) {
        super(phoneUtil);
    }

    /**
     * Sends 2FA verification code via SMS.
     *
     * @param user The user receiving the verification code
     * @param verificationCode 6-digit verification code
     * @param httpRequest HTTP request for context
     */
    @Override
    @Async("smsExecutor")
    public void sendTwoFactorVerificationCode(User user, String verificationCode, HttpServletRequest httpRequest) {
        log.info("Sending 2FA verification code via SMS to user: {}", user.getUsername());

        if (!hasValidPhoneNumber(user)) {
            log.error("Cannot send 2FA SMS to user {} - no valid phone number", user.getUsername());
            throw new InvalidPhoneNumberException("User has no valid phone number configured for SMS 2FA");
        }

        try {
            // Use the configured template for 2FA codes
            String message = formatMessage(twoFactorCodeTemplate, "code", verificationCode);
            phoneUtil.sendSms(message, user.getPhoneNumber());

            log.info("2FA verification SMS sent successfully to user: {}", user.getUsername());

        } catch (Exception e) {
            log.error("Failed to send 2FA verification SMS to user {}: {}", user.getUsername(), e.getMessage());
            throw e;
        }
    }

    /**
     * Sends confirmation SMS when 2FA is enabled.
     *
     * @param user User who enabled 2FA
     * @param twoFactorType Type of 2FA that was enabled
     */
    @Override
    @Async("smsExecutor")
    public void sendTwoFactorEnabledConfirmation(User user, TwoFactorType twoFactorType) {
        log.info("Sending 2FA enabled confirmation SMS to user: {} for type: {}", user.getUsername(), twoFactorType);

        if (!hasValidPhoneNumber(user)) {
            log.warn("Cannot send 2FA enabled confirmation SMS to user {} - no valid phone number", user.getUsername());
            return; // Don't throw exception for confirmation messages
        }

        try {
            String username = defineUsername(user);
            String message = String.format(
                "Bonjour %s, l'authentification à deux facteurs (%s) a été activée sur votre compte. " +
                "Si vous n'êtes pas à l'origine de cette action, contactez-nous immédiatement.",
                username,
                twoFactorType.name()
            );

            phoneUtil.sendSms(message, user.getPhoneNumber());

            log.info("2FA enabled confirmation SMS sent successfully to user: {}", user.getUsername());

        } catch (Exception e) {
            log.error("Failed to send 2FA enabled confirmation SMS to user {}: {}", user.getUsername(), e.getMessage());
            // Don't rethrow for confirmation messages - they're not critical
        }
    }
}
package be.steby.CoreProject.bll.domains.auth.services.notification;

import be.steby.CoreProject.bll.common.services.notification.sms.BaseSmsService;
import be.steby.CoreProject.bll.domains.auth.exceptions.phone.InvalidPhoneNumberException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.utils.SmsUtil;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Auth domain-specific SMS service.
 * Handles all SMS notifications related to authentication operations:
 * - 2FA verification codes
 * - Security notifications
 * - 2FA enablement confirmations
 */
@Service
@Slf4j
public class AuthSmsServiceImpl extends BaseSmsService implements AuthSmsService {

    public AuthSmsServiceImpl(SmsUtil smsUtil) {
        super(smsUtil);
    }

    /**
     * Sends 2FA verification code via SMS.
     *
     * @param user The user receiving the verification code
     * @param verificationCode 6-digit verification code
     */
    @Override
    @Async("smsExecutor")
    public void sendTwoFactorVerificationCode(User user, String verificationCode) {
        log.info("Sending 2FA verification code via SMS to user: {}", user.getUsername());

        if (!hasVerifiedPhoneNumber(user)) { // Plus strict pour 2FA
            throw new InvalidPhoneNumberException("Phone number must be verified for SMS 2FA");
        }

        String message = "Your verification code: " + verificationCode;
        sendSms(message, user.getPhoneNumber());

        log.info("2FA verification SMS sent successfully to user: {}", user.getUsername());
    }
}
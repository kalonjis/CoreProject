 package be.steby.CoreProject.il.sms;

import be.steby.CoreProject.bll.common.exceptions.phone.SmsSendingException;
import be.steby.CoreProject.bll.common.services.validation.phone.PhoneNumberFormatValidationService;
import be.steby.CoreProject.il.sms.fallback.FailedSmsHandler;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Twilio Gateway with Circuit Breaker and Retry protection.
 *
 * This component is responsible for the actual Twilio API communication.
 * It uses Resilience4j annotations to provide:
 * - Circuit Breaker: Fails fast when Twilio is down
 * - Retry: Automatic retry with exponential backoff
 * - Fallback: Delegates to FailedSmsHandler when all else fails
 *
 * Provides two sending modes:
 * - {@link #send}: With fallback (queues for retry if delivery fails)
 * - {@link #sendWithoutFallback}: Without fallback (throws immediately on failure)
 *
 * Execution order:
 * 1. Method is called
 * 2. If it fails, CircuitBreaker records the failure
 * 3. Retry decides whether to retry
 * 4. After all retries exhausted (or circuit open), fallback is called
 *
 * @see FailedSmsHandler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioSmsSender {

    private static final String BACKEND_NAME = "twilioBackend";

    private final FailedSmsHandler failedSmsHandler;
    private final PhoneNumberFormatValidationService phoneNumberFormatValidationService;

    @Value("${phone.twilio.account-sid}")
    private String accountSid;

    @Value("${phone.twilio.auth-token}")
    private String authToken;

    @Value("${phone.twilio.from-number}")
    private String fromPhoneNumber;

    @Value("${phone.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${phone.sms.simulation-mode:false}")
    private boolean simulationMode;

    /**
     * Initialize Twilio SDK with credentials after bean construction.
     */
    @PostConstruct
    public void initTwilio() {
        if (smsEnabled && !simulationMode) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SDK initialized successfully with from number: {}", fromPhoneNumber);
        } else if (simulationMode) {
            log.info("Twilio SMS in SIMULATION mode - no actual SMS will be sent");
        } else {
            log.info("Twilio SMS is DISABLED");
        }
    }

    /**
     * Sends an SMS with Circuit Breaker and Retry protection.
     * If all retries fail or circuit is open, SMS is queued for later retry.
     *
     * Use this for non-critical SMS where delayed delivery is acceptable:
     * - Security notifications
     * - Marketing SMS
     * - General alerts
     *
     * @param message the SMS message to send
     */
    @CircuitBreaker(name = BACKEND_NAME, fallbackMethod = "sendFallback")
    @Retry(name = BACKEND_NAME)
    public void send(SmsMessage message) {
        doSend(message);
    }

    /**
     * Sends an SMS with Circuit Breaker and Retry protection but WITHOUT fallback.
     * If delivery fails, throws immediately instead of queuing for retry.
     *
     * Use this for time-sensitive SMS where delayed delivery is useless:
     * - 2FA verification codes (expire in minutes)
     * - Password reset codes
     * - Any OTP-based authentication
     *
     * @param message the SMS message to send
     * @throws SmsSendingException if sending fails after all retries or circuit is open
     */
    @CircuitBreaker(name = BACKEND_NAME, fallbackMethod = "sendWithoutFallbackFallback")
    @Retry(name = BACKEND_NAME)
    public void sendWithoutFallback(SmsMessage message) throws SmsSendingException {
        doSend(message);
    }

    /**
     * Core SMS sending logic shared by both send methods.
     *
     * @param message the SMS message to send
     * @throws SmsSendingException if sending fails
     */
    private void doSend(SmsMessage message) {
        log.debug("Attempting to send SMS: recipient='{}', contentLength={}",
                message.toString(), message.content().length());

        if (!smsEnabled) {
            log.info("SMS disabled - would send to: {}", message.toString());
            return;
        }

        if (simulationMode) {
            simulateSend(message);
            return;
        }

        sendViaTwilio(message);
    }

    /**
     * Simulates SMS sending for development/testing.
     *
     * @param message the SMS to simulate
     */
    private void simulateSend(SmsMessage message) {
        log.info("SMS SIMULATION - TO: {} - CONTENT: '{}'",
                message.toString(),
                message.content());

        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("SMS simulation sleep interrupted");
        }
    }

    /**
     * Sends SMS via Twilio API.
     *
     * @param message the SMS to send
     * @throws SmsSendingException if Twilio API call fails
     */
    private void sendViaTwilio(SmsMessage message) {
        String formattedNumber = phoneNumberFormatValidationService.validateAndFormat(message.recipient());

        try {
            Message twilioMessage = Message.creator(
                    new PhoneNumber(formattedNumber),
                    new PhoneNumber(fromPhoneNumber),
                    message.content()
            ).create();

            log.info("SMS sent successfully - TO: {} - SID: {}",
                    message.toString(),
                    twilioMessage.getSid());

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", message.toString(), e.getMessage());
            throw new SmsSendingException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method for {@link #send} - queues SMS for later retry.
     *
     * Called when Circuit Breaker is OPEN or all retries exhausted.
     * Delegates to the configured FailedSmsHandler strategy.
     *
     * @param message   the SMS that failed to send
     * @param throwable the exception that triggered the fallback
     */
    private void sendFallback(SmsMessage message, Throwable throwable) {
        log.warn("Twilio fallback triggered. Strategy: '{}', Recipient: {}, Error: {}",
                failedSmsHandler.getStrategyName(),
                message.toString(),
                throwable.getMessage());

        // Delegate to the configured handler (DB or RabbitMQ)
        failedSmsHandler.handle(
                message,
                throwable.getMessage(),
                throwable
        );

        log.info("SMS queued for retry via '{}' strategy. Recipient: {}",
                failedSmsHandler.getStrategyName(),
                message.toString());
    }

    /**
     * Fallback method for {@link #sendWithoutFallback} - throws immediately.
     *
     * Called when Circuit Breaker is OPEN or all retries exhausted.
     * Does NOT queue for retry - simply wraps and rethrows the exception.
     *
     * @param message   the SMS that failed to send
     * @param throwable the exception that triggered the fallback
     * @throws SmsSendingException always thrown with details about the failure
     */
    private void sendWithoutFallbackFallback(SmsMessage message, Throwable throwable)
            throws SmsSendingException {

        String errorMessage;

        if (throwable instanceof CallNotPermittedException) {
            errorMessage = "SMS service temporarily unavailable (circuit breaker open)";
            log.warn("Circuit breaker OPEN - cannot send critical SMS. Recipient: {}",
                    message.toString());
        } else {
            errorMessage = "Failed to send SMS after all retries: " + throwable.getMessage();
            log.error("Critical SMS delivery failed. Recipient: {}, Error: {}",
                    message.toString(), throwable.getMessage());
        }

        throw new SmsSendingException(errorMessage, throwable);
    }
}
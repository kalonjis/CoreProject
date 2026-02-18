// src/main/java/be/steby/CoreProject/bll/domains/monitoring/services/TwilioHealthService.java

package be.steby.CoreProject.bll.domains.monitoring.services;

import be.steby.CoreProject.bll.domains.monitoring.models.TwilioHealthResult;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.Account;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for testing Twilio API connectivity.
 *
 * <p>Provides health check capabilities for the SMS subsystem by testing
 * the connection to Twilio without actually sending SMS.</p>
 *
 * <h4>How it works:</h4>
 * <ul>
 *   <li>Fetches account information from Twilio API</li>
 *   <li>Validates credentials are correct</li>
 *   <li>Returns account status (active, suspended, closed)</li>
 * </ul>
 *
 * <p>No SMS is sent during this process.</p>
 *
 * @see TwilioHealthResult
 */
@Service
@Slf4j
public class TwilioHealthService {

    @Value("${phone.twilio.account-sid}")
    private String accountSid;

    @Value("${phone.twilio.auth-token}")
    private String authToken;

    @Value("${phone.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${phone.sms.simulation-mode:false}")
    private boolean simulationMode;

    private boolean twilioInitialized = false;

    /**
     * Ensures Twilio SDK is initialized before testing.
     */
    @PostConstruct
    public void init() {
        if (smsEnabled && !simulationMode) {
            try {
                Twilio.init(accountSid, authToken);
                twilioInitialized = true;
                log.debug("TwilioHealthService: SDK initialized");
            } catch (Exception e) {
                log.warn("TwilioHealthService: Failed to initialize SDK - {}", e.getMessage());
                twilioInitialized = false;
            }
        }
    }

    /**
     * Tests Twilio API connectivity.
     *
     * <p>Attempts to fetch account information from Twilio API.
     * This validates both connectivity and credentials without sending SMS.</p>
     *
     * @return TwilioHealthResult containing connection status, response time, and account info
     */
    public TwilioHealthResult testConnection() {
        long startTime = System.currentTimeMillis();

        // Handle disabled/simulation modes
        if (!smsEnabled) {
            return TwilioHealthResult.failure(0, "SMS service is disabled");
        }

        if (simulationMode) {
            log.debug("Twilio health check in simulation mode - returning simulated success");
            return TwilioHealthResult.success(50, "simulation");
        }

        if (!twilioInitialized) {
            return TwilioHealthResult.failure(0, "Twilio SDK not initialized");
        }

        try {
            Account account = Account.fetcher(accountSid).fetch();
            long responseTime = System.currentTimeMillis() - startTime;

            String status = account.getStatus() != null 
                    ? account.getStatus().toString().toLowerCase() 
                    : "unknown";

            log.debug("Twilio connection test successful in {}ms - account status: {}", 
                    responseTime, status);

            return TwilioHealthResult.success(responseTime, status);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            String errorMessage = extractErrorMessage(e);

            log.warn("Twilio connection test failed after {}ms: {}", responseTime, errorMessage);
            return TwilioHealthResult.failure(responseTime, errorMessage);
        }
    }

    /**
     * Extracts a clean error message from the exception.
     *
     * @param e the exception thrown during connection test
     * @return user-friendly error message
     */
    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();

        if (e.getCause() != null) {
            message = e.getCause().getMessage();
        }

        return message != null ? message : "Unknown Twilio error";
    }
}
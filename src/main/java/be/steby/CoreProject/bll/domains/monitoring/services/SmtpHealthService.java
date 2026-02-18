// src/main/java/be/steby/CoreProject/bll/domains/monitoring/services/SmtpHealthService.java

package be.steby.CoreProject.bll.domains.monitoring.services;

import be.steby.CoreProject.bll.domains.monitoring.models.SmtpHealthResult;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

/**
 * Service for testing SMTP server connectivity.
 *
 * <p>Provides health check capabilities for the mail subsystem by testing
 * the connection to the configured SMTP server without actually sending emails.</p>
 *
 * <h4>How it works:</h4>
 * <ul>
 *   <li>Opens a TCP connection to the SMTP server</li>
 *   <li>Performs EHLO/HELO handshake</li>
 *   <li>Authenticates if credentials are configured</li>
 *   <li>Closes the connection cleanly</li>
 * </ul>
 *
 * <p>No email is sent during this process.</p>
 *
 * @see SmtpHealthResult
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpHealthService {

    private final JavaMailSender mailSender;

    /**
     * Tests SMTP server connectivity.
     *
     * <p>Attempts to establish a connection and authenticate with the SMTP server.
     * Measures the response time and captures any errors.</p>
     *
     * @return SmtpHealthResult containing connection status, response time, and error if any
     */
    public SmtpHealthResult testConnection() {
        long startTime = System.currentTimeMillis();

        try {
            ((JavaMailSenderImpl) mailSender).testConnection();
            long responseTime = System.currentTimeMillis() - startTime;

            log.debug("SMTP connection test successful in {}ms", responseTime);
            return SmtpHealthResult.success(responseTime);

        } catch (MessagingException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            String errorMessage = extractErrorMessage(e);

            log.warn("SMTP connection test failed after {}ms: {}", responseTime, errorMessage);
            return SmtpHealthResult.failure(responseTime, errorMessage);
        }
    }

    /**
     * Extracts a clean error message from the exception.
     *
     * @param e the MessagingException thrown during connection test
     * @return user-friendly error message
     */
    private String extractErrorMessage(MessagingException e) {
        String message = e.getMessage();
        
        // Get root cause if available
        if (e.getCause() != null) {
            message = e.getCause().getMessage();
        }
        
        return message != null ? message : "Unknown SMTP error";
    }
}
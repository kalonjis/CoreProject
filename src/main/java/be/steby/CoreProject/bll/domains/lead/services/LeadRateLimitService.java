package be.steby.CoreProject.bll.domains.lead.services;

import be.steby.CoreProject.dal.repositories.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for public lead rate limiting.
 *
 * <p>Prevents spam by limiting submissions per IP address and email
 * within configurable time windows.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeadRateLimitService {

    private final LeadRepository leadRepository;

    @Value("${lead.rate-limit.ip.max-requests:5}")
    private int maxRequestsPerIp;

    @Value("${lead.rate-limit.ip.window-minutes:60}")
    private int ipWindowMinutes;

    @Value("${lead.rate-limit.email.max-requests:3}")
    private int maxRequestsPerEmail;

    @Value("${lead.rate-limit.email.window-minutes:60}")
    private int emailWindowMinutes;

    /**
     * Checks if an IP address has exceeded the rate limit.
     *
     * @param ipAddress the IP address to check
     * @return true if rate limit exceeded
     */
    public boolean isIpRateLimited(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("IP address is null or blank, skipping rate limit check");
            return false;
        }

        Instant since = Instant.now().minus(Duration.ofMinutes(ipWindowMinutes));
        long count = leadRepository.countByIpAddressSince(ipAddress, since);

        boolean limited = count >= maxRequestsPerIp;
        if (limited) {
            log.warn("IP rate limit exceeded for {} - {} requests in {} minutes",
                    ipAddress, count, ipWindowMinutes);
        }

        return limited;
    }

    /**
     * Checks if an email address has exceeded the rate limit.
     *
     * @param email the email address to check
     * @return true if rate limit exceeded
     */
    public boolean isEmailRateLimited(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        Instant since = Instant.now().minus(Duration.ofMinutes(emailWindowMinutes));
        long count = leadRepository.countByEmailSince(email.toLowerCase(), since);

        boolean limited = count >= maxRequestsPerEmail;
        if (limited) {
            log.warn("Email rate limit exceeded for {} - {} requests in {} minutes",
                    email, count, emailWindowMinutes);
        }

        return limited;
    }

    /**
     * Checks both IP and email rate limits.
     *
     * @param ipAddress the IP address
     * @param email     the email address
     * @return true if either limit is exceeded
     */
    public boolean isRateLimited(String ipAddress, String email) {
        return isIpRateLimited(ipAddress) || isEmailRateLimited(email);
    }
}
package be.steby.CoreProject.bll.domains.lead.services;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.lead.events.LeadSubmittedEvent;
import be.steby.CoreProject.bll.domains.lead.exceptions.HoneypotDetectedException;
import be.steby.CoreProject.bll.domains.lead.exceptions.LeadRateLimitException;
import be.steby.CoreProject.bll.domains.lead.exceptions.LeadValidationException;
import be.steby.CoreProject.bll.domains.lead.models.LeadRequest;
import be.steby.CoreProject.bll.domains.lead.models.LeadResult;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dl.entities.crm.Lead;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementation of public lead submission handling.
 *
 * <p>Flow:</p>
 * <ol>
 *   <li>Check honeypot (reject bots)</li>
 *   <li>Validate email format and domain</li>
 *   <li>Check rate limits (IP + email)</li>
 *   <li>Build and save lead</li>
 *   <li>Publish event for email notification</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadRateLimitService rateLimitService;
    private final EmailPolicyService emailPolicyService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LeadResult submitInquiry(LeadRequest request, HttpServletRequest httpRequest) {
        log.debug("Processing public lead - subject: {}, type: {}",
                request.subject(), request.leadType());

        String ipAddress = extractIpAddress(httpRequest);

        // 1. Honeypot check (silent rejection for bots)
        if (request.isHoneypotFilled()) {
            log.warn("Honeypot triggered from IP: {}", ipAddress);
            throw new HoneypotDetectedException("Bot detected");
        }

        // 2. Validate email is provided
        if (!request.hasEmail()) {
            throw new LeadValidationException("Email is required");
        }

        String email = request.email().toLowerCase().trim();

        // 3. Email validation (format + domain)
        validateEmail(email);

        // 4. Rate limit check
        if (rateLimitService.isRateLimited(ipAddress, email)) {
            throw new LeadRateLimitException("Too many inquiries. Please try again later.");
        }

        // 5. Build and save lead
        Lead lead = buildInquiry(request, email, ipAddress);
        leadRepository.save(lead);

        log.info("Public lead saved - publicId: {}, email: {}, type: {}",
                lead.getPublicId(), email, request.leadType());

        // 6. Publish event for email notification
        eventPublisher.publishEvent(new LeadSubmittedEvent(
                lead,
                request.message()
        ));

        return LeadResult.success(lead.getPublicId());
    }

    // ========================================
    // region Private Methods
    // ========================================

    /**
     * Validates email format and domain.
     */
    private void validateEmail(String email) {
        EmailValidationResult result = emailPolicyService.validateEmailFormatAndDomain(email);
        if (!result.isValid()) {
            throw new LeadValidationException(
                    "Invalid email: " + String.join(", ", result.errors())
            );
        }
    }

    /**
     * Builds the public lead entity.
     */
    private Lead buildInquiry(LeadRequest request, String email, String ipAddress) {
        return Lead.builder()
                .email(email)
                .name(request.hasName() ? request.name().trim() : null)
                .subject(request.subject().trim())
                .leadType(request.leadType())
                .ipAddress(ipAddress)
                .submittedAt(Instant.now())
                .build();
    }

    /**
     * Extracts client IP address from HTTP request.
     * Handles proxies via X-Forwarded-For header.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
package be.steby.CoreProject.bll.domains.lead.services;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.lead.events.*;
import be.steby.CoreProject.bll.domains.lead.exceptions.*;
import be.steby.CoreProject.bll.domains.lead.models.*;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dal.specifications.crm.LeadSpecification;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of {@link LeadService}.
 *
 * <h3>Public flow</h3>
 * <p>Handles anonymous visitor submissions with honeypot, email validation,
 * and rate limiting before persisting the lead and notifying the team.</p>
 *
 * <h3>CRM flow</h3>
 * <p>Handles the full commercial lifecycle: assignment, review, conversion,
 * and rejection. All state transitions publish domain events — no direct
 * cross-domain calls are made.</p>
 *
 * <h3>Terminal state guard</h3>
 * <p>{@code CONVERTED} and {@code REJECTED} leads are terminal.
 * Any mutation attempt on a terminal lead throws the appropriate exception.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadRateLimitService rateLimitService;
    private final EmailPolicyService emailPolicyService;
    private final UserService userService;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Public submission
    // =========================================================================

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
        eventPublisher.publishEvent(new LeadSubmittedEvent(lead, request.message()));

        return LeadResult.success(lead.getPublicId());
    }

    // =========================================================================
    // Lookup
    // =========================================================================

    @Override
    public Lead getByPublicId(String publicId) {
        return leadRepository.findByPublicId(publicId)
                .orElseThrow(() -> new LeadNotFoundException(publicId));
    }

    @Override
    public Page<Lead> findAll(LeadFilterRequest filter, Pageable pageable) {
        Long assignedToId = resolveAssignedToId(filter);

        Specification<Lead> spec = Specification.allOf(
                LeadSpecification.hasStatus(filter.status()),
                LeadSpecification.hasLeadType(filter.leadType()),
                Boolean.TRUE.equals(filter.unassignedOnly())
                        ? LeadSpecification.unassigned()
                        : LeadSpecification.assignedTo(assignedToId),
                LeadSpecification.emailOrNameContains(filter.keyword()),
                LeadSpecification.submittedBetween(filter.submittedFrom(), filter.submittedTo())
        );

        return leadRepository.findAll(spec, pageable);
    }

    // =========================================================================
    // CRM lifecycle
    // =========================================================================

    @Override
    @Transactional
    public Lead assign(String publicId, LeadAssignRequest request, User actor) {
        Lead lead = getByPublicId(publicId);
        guardTerminal(lead);

        Device device = deviceService.detectAndRegisterDevice(actor);
        User previousAssignee = lead.getAssignedTo();
        User commercial = userService.getUserByPublicId(request.commercialPublicId());

        lead.setAssignedTo(commercial);
        leadRepository.save(lead);

        log.info("Lead {} assigned to {} by {}",
                publicId, commercial.getUsername(), actor.getUsername());

        eventPublisher.publishEvent(new LeadAssignedEvent(lead, commercial, previousAssignee, device));

        return lead;
    }

    @Override
    @Transactional
    public Lead markInReview(String publicId, User actor) {
        Lead lead = getByPublicId(publicId);
        guardTerminal(lead);

        Device device = deviceService.detectAndRegisterDevice(actor);

        lead.setStatus(LeadStatus.IN_REVIEW);
        leadRepository.save(lead);

        log.info("Lead {} marked IN_REVIEW by {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new LeadInReviewEvent(lead, actor, device));

        return lead;
    }

    @Override
    @Transactional
    public Lead convert(String publicId, LeadConvertRequest request, User actor) {
        Lead lead = getByPublicId(publicId);
        guardTerminal(lead);

        // Pre-generate the contact publicId so the contact domain can use it
        // as a correlation handle without requiring a direct service call.
        String contactPublicId = UUID.randomUUID().toString();

        lead.setStatus(LeadStatus.CONVERTED);
        lead.setConvertedAt(Instant.now());
        leadRepository.save(lead);

        log.info("Lead {} converted by {} → contact {}", publicId, actor.getUsername(), contactPublicId);

        Device device = deviceService.detectAndRegisterDevice(actor);

        eventPublisher.publishEvent(new LeadConvertedEvent(lead, actor, contactPublicId, device));

        return lead;
    }

    @Override
    @Transactional
    public Lead reject(String publicId, LeadRejectRequest request, User actor) {
        Lead lead = getByPublicId(publicId);
        guardTerminal(lead);

        Device device = deviceService.detectAndRegisterDevice(actor);

        lead.setStatus(LeadStatus.REJECTED);
        lead.setRejectionReason(request.rejectionReason());
        leadRepository.save(lead);

        log.info("Lead {} rejected by {} — reason: {}", publicId, actor.getUsername(), request.rejectionReason());

        eventPublisher.publishEvent(new LeadRejectedEvent(lead, actor, request.rejectionReason(), device));

        return lead;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Guards against mutations on terminal leads.
     *
     * @param lead the lead to check
     * @throws LeadAlreadyConvertedException if the lead is already converted
     * @throws LeadAlreadyRejectedException  if the lead is already rejected
     */
    private void guardTerminal(Lead lead) {
        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new LeadAlreadyConvertedException(lead.getPublicId());
        }
        if (lead.getStatus() == LeadStatus.REJECTED) {
            throw new LeadAlreadyRejectedException(lead.getPublicId());
        }
    }

    /**
     * Resolves the internal ID of the commercial from the filter's public UUID.
     * Returns {@code null} if no assignedToPublicId is provided.
     */
    private Long resolveAssignedToId(LeadFilterRequest filter) {
        if (filter.assignedToPublicId() == null || filter.assignedToPublicId().isBlank()) {
            return null;
        }
        return userService.getUserByPublicId(filter.assignedToPublicId()).getId();
    }

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
     * Builds the public lead entity from the submission request.
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
     * Extracts the client IP address from the HTTP request.
     * Handles reverse proxies via the {@code X-Forwarded-For} header.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
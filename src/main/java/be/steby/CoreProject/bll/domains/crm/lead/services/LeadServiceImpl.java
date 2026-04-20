package be.steby.CoreProject.bll.domains.crm.lead.services;

import be.steby.CoreProject.bll.common.services.validation.email.EmailPolicyService;
import be.steby.CoreProject.bll.domains.crm.lead.events.*;
import be.steby.CoreProject.bll.domains.crm.lead.exceptions.*;
import be.steby.CoreProject.bll.domains.crm.lead.models.*;
import be.steby.CoreProject.bll.domains.emailaddress.models.EmailValidationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dal.specifications.crm.LeadSpecification;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
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
import java.util.List;
import java.util.Optional;
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
    private final ContactRepository contactRepository;
    private final LeadRateLimitService rateLimitService;
    private final EmailPolicyService emailPolicyService;
    private final UserService userService;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    /** Active statuses — a lead in one of these states can be deduplicated. */
    private static final List<LeadStatus> ACTIVE_STATUSES = List.of(LeadStatus.NEW, LeadStatus.IN_REVIEW);

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

        // 5. Deduplication — return existing active lead, notify interaction domain
        Optional<Lead> existing = leadRepository
                .findFirstByEmailIgnoreCaseAndStatusInOrderBySubmittedAtDesc(email, ACTIVE_STATUSES);
        if (existing.isPresent()) {
            log.info("Deduplication (public) — returning existing active lead {} for email: {}",
                    existing.get().getPublicId(), email);
            eventPublisher.publishEvent(new LeadResubmittedEvent(
                    existing.get(), request.subject(), request.message()));
            return LeadResult.success(existing.get().getPublicId());
        }

        // 6. Build and save lead
        Lead lead = buildInquiry(request, email, ipAddress);
        leadRepository.save(lead);

        log.info("Public lead saved - publicId: {}, email: {}, type: {}",
                lead.getPublicId(), email, request.leadType());

        // 7. Publish event for email notification
        eventPublisher.publishEvent(new LeadSubmittedEvent(lead, request.message()));

        return LeadResult.success(lead.getPublicId());
    }

    // =========================================================================
    // Manual creation
    // =========================================================================

    @Override
    @Transactional
    public Lead createManual(LeadManualCreateRequest request, User actor) {
        String email = request.email().toLowerCase().trim();

        // Deduplication — if an active lead exists for this email, return it
        Optional<Lead> existing = leadRepository
                .findFirstByEmailIgnoreCaseAndStatusInOrderBySubmittedAtDesc(email, ACTIVE_STATUSES);
        if (existing.isPresent()) {
            log.info("Deduplication (manual) — returning existing active lead {} for email: {}, by: {}",
                    existing.get().getPublicId(), email, actor.getUsername());
            return existing.get();
        }

        log.info("Manual lead creation — email: {}, by: {}", email, actor.getUsername());

        Lead lead = Lead.builder()
                .email(email)
                .civility(request.civility())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .organisationName(request.organisationName())
                .subject(request.subject().trim())
                .message(request.message() != null ? request.message().trim() : null)
                .leadType(request.leadType())
                .leadSource(LeadSource.MANUAL)
                .submittedAt(Instant.now())
                .build();

        leadRepository.save(lead);

        eventPublisher.publishEvent(new LeadSubmittedEvent(lead, lead.getMessage()));

        return lead;
    }

    // =========================================================================
    // Webhook creation
    // =========================================================================

    @Override
    @Transactional
    public Lead createFromWebhook(LeadManualCreateRequest request, LeadSource source) {
        log.info("Webhook lead creation — source: {}, email: {}", source, request.email());

        Lead lead = Lead.builder()
                .email(request.email().toLowerCase().trim())
                .civility(request.civility())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .organisationName(request.organisationName())
                .subject(request.subject() != null ? request.subject().trim() : "")
                .message(request.message() != null ? request.message().trim() : null)
                .leadType(request.leadType())
                .leadSource(source)
                .submittedAt(Instant.now())
                .build();

        leadRepository.save(lead);

        eventPublisher.publishEvent(new LeadSubmittedEvent(lead, lead.getMessage()));

        return lead;
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
    public LeadDetailModel getDetail(String publicId) {
        Lead lead = getByPublicId(publicId);
        String existingContactPublicId = contactRepository
                .findByEmailIgnoreCase(lead.getEmail())
                .map(Contact::getPublicId)
                .orElse(null);
        return new LeadDetailModel(lead, existingContactPublicId);
    }

    @Override
    public Page<Lead> findAll(LeadFilterRequest filter, Pageable pageable) {
        Long assignedToId = resolveAssignedToId(filter);

        Specification<Lead> spec = Specification.allOf(
                LeadSpecification.hasStatus(filter.status()),
                LeadSpecification.isActive(filter.activeOnly()),
                LeadSpecification.hasLeadType(filter.leadType()),
                LeadSpecification.hasLeadSource(filter.leadSource()),
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
    public Lead enrich(String publicId, LeadEnrichRequest request) {
        Lead lead = getByPublicId(publicId);
        guardTerminal(lead);

        if (request.civility()         != null) lead.setCivility(request.civility());
        if (request.firstName()        != null) lead.setFirstName(request.firstName().isBlank()        ? null : request.firstName().trim());
        if (request.lastName()         != null) lead.setLastName(request.lastName().isBlank()          ? null : request.lastName().trim());
        if (request.phone()            != null) lead.setPhone(request.phone().isBlank()                ? null : request.phone().trim());
        if (request.jobTitle()         != null) lead.setJobTitle(request.jobTitle().isBlank()          ? null : request.jobTitle().trim());
        if (request.organisationName() != null) lead.setOrganisationName(request.organisationName().isBlank() ? null : request.organisationName().trim());
        if (request.leadType()         != null) lead.setLeadType(request.leadType());
        if (request.leadSource()       != null) lead.setLeadSource(request.leadSource());

        leadRepository.save(lead);

        log.info("Lead {} enriched — firstName: {}, lastName: {}, phone: {}, org: {}, source: {}",
                publicId, request.firstName(), request.lastName(), request.phone(), request.organisationName(), request.leadSource());

        return lead;
    }

    @Override
    @Transactional
    public Lead assign(String publicId, LeadAssignRequest request, User actor) {
        Lead lead = getByPublicId(publicId);
        guardTerminal(lead);

        if (!actor.hasAdminPrivileges() && !actor.getPublicId().equals(request.commercialPublicId())) {
            throw new LeadAssignNotAuthorizedException();
        }

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

        // Only the assigned commercial or an admin can mark in review
        if (!actor.hasAdminPrivileges()) {
            if (lead.getAssignedTo() == null || !lead.getAssignedTo().getId().equals(actor.getId())) {
                throw new LeadDomainException("Only the assigned commercial can mark this lead in review", 403);
            }
        }

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

        eventPublisher.publishEvent(new LeadConvertedEvent(
                lead, actor, contactPublicId, device,
                request.organisationPublicId(), request.organisationName(),
                request.email()));

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
                .civility(request.civility())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .jobTitle(request.jobTitle())
                .organisationName(request.organisationName())
                .subject(request.subject().trim())
                .message(request.message() != null ? request.message().trim() : null)
                .leadType(request.leadType())
                .leadSource(request.leadSource())
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
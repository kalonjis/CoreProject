package be.steby.CoreProject.bll.domains.crm.supportticket.services;

import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.crm.supportticket.events.SupportTicketAssignedEvent;
import be.steby.CoreProject.bll.domains.crm.supportticket.events.SupportTicketClosedEvent;
import be.steby.CoreProject.bll.domains.crm.supportticket.events.SupportTicketCreatedEvent;
import be.steby.CoreProject.bll.domains.crm.supportticket.events.SupportTicketDeletedEvent;
import be.steby.CoreProject.bll.domains.crm.supportticket.events.SupportTicketStatusChangedEvent;
import be.steby.CoreProject.bll.domains.crm.supportticket.events.SupportTicketUpdatedEvent;
import be.steby.CoreProject.bll.domains.crm.supportticket.exceptions.SupportTicketAlreadyClosedException;
import be.steby.CoreProject.bll.domains.crm.supportticket.exceptions.SupportTicketAssignNotAuthorizedException;
import be.steby.CoreProject.bll.domains.crm.supportticket.exceptions.SupportTicketNotFoundException;
import be.steby.CoreProject.bll.domains.crm.supportticket.exceptions.SupportTicketRateLimitException;
import be.steby.CoreProject.bll.domains.crm.supportticket.exceptions.SupportTicketStatusTransitionException;
import be.steby.CoreProject.bll.domains.crm.supportticket.models.PublicSupportTicketRequest;
import be.steby.CoreProject.bll.domains.crm.supportticket.models.SupportTicketAssignRequest;
import be.steby.CoreProject.bll.domains.crm.supportticket.models.SupportTicketChangeStatusRequest;
import be.steby.CoreProject.bll.domains.crm.supportticket.models.SupportTicketCreateRequest;
import be.steby.CoreProject.bll.domains.crm.supportticket.models.SupportTicketFilterRequest;
import be.steby.CoreProject.bll.domains.crm.supportticket.models.SupportTicketUpdateRequest;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.SupportTicketRepository;
import be.steby.CoreProject.dal.specifications.crm.SupportTicketSpecification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.dl.enums.crm.SupportTicketSource;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${support.rate-limit.ip.max-requests:5}")
    private int maxRequestsPerIp;

    @Value("${support.rate-limit.ip.window-minutes:60}")
    private int ipWindowMinutes;

    // =========================================================================
    // Lookup
    // =========================================================================

    @Override
    public SupportTicket getById(Long id) {
        return supportTicketRepository.findById(id)
                .orElseThrow(() -> SupportTicketNotFoundException.byId(id));
    }

    @Override
    public SupportTicket getByPublicId(String publicId) {
        return supportTicketRepository.findByPublicId(publicId)
                .orElseThrow(() -> SupportTicketNotFoundException.byPublicId(publicId));
    }

    @Override
    public Page<SupportTicket> findAll(SupportTicketFilterRequest filter, Pageable pageable) {

        Long contactId = null;
        if (filter.contactPublicId() != null) {
            contactId = contactRepository.findByPublicId(filter.contactPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Contact not found with publicId: " + filter.contactPublicId()))
                    .getId();
        }

        Long assignedToId = null;
        if (filter.assignedToPublicId() != null) {
            assignedToId = userRepository.findByPublicId(filter.assignedToPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found with publicId: " + filter.assignedToPublicId()))
                    .getId();
        }

        Specification<SupportTicket> spec = Specification.allOf(
                SupportTicketSpecification.subjectContains(filter.keyword()),
                SupportTicketSpecification.hasStatus(filter.status()),
                SupportTicketSpecification.hasSource(filter.source()),
                SupportTicketSpecification.submittedBy(contactId),
                SupportTicketSpecification.assignedTo(assignedToId),
                SupportTicketSpecification.isUnassigned(filter.unassignedOnly()),
                SupportTicketSpecification.submittedByOrganisation(filter.organisationPublicId())
        );

        return supportTicketRepository.findAll(spec, pageable);
    }

    @Override
    public List<SupportTicket> findByContact(String contactPublicId) {
        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + contactPublicId));
        return supportTicketRepository.findBySubmittedByIdOrderByCreatedAtDesc(contact.getId());
    }

    // =========================================================================
    // Public form submission
    // =========================================================================

    @Override
    @Transactional
    public SupportTicket submitFromPublicForm(PublicSupportTicketRequest request, HttpServletRequest httpRequest) {
        log.debug("Public support ticket submission — email: {}", request.email());

        // 1. Honeypot check (silent rejection for bots)
        if (request.isHoneypotFilled()) {
            log.warn("Honeypot triggered on public support form from email: {}", request.email());
            // Return fake-success: don't reveal bot detection to the submitter
            return buildFakeTicket();
        }

        String ipAddress = extractIpAddress(httpRequest);

        // 2. Rate limit by IP
        Instant since = Instant.now().minus(Duration.ofMinutes(ipWindowMinutes));
        long count = supportTicketRepository.countByIpAddressAndCreatedAtAfter(ipAddress, since);
        if (count >= maxRequestsPerIp) {
            log.warn("IP rate limit exceeded for public support form — IP: {}, count: {}", ipAddress, count);
            throw new SupportTicketRateLimitException("Too many submissions. Please try again later.");
        }

        // 3. Link to existing contact if email is known — no auto-create
        String email = request.email().toLowerCase().trim();
        Contact contact = contactRepository.findByEmailIgnoreCase(email).orElse(null);

        // 4. Create the ticket
        SupportTicket.SupportTicketBuilder builder = SupportTicket.builder()
                .subject(request.subject().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .status(SupportTicketStatus.OPEN)
                .source(SupportTicketSource.PUBLIC_FORM)
                .ipAddress(ipAddress);

        if (contact != null) {
            builder.submittedBy(contact);
            log.info("Public support ticket linked to existing contact — email: {}", email);
        } else {
            builder.reporterName((request.firstName().trim() + " " + request.lastName().trim()).trim())
                   .reporterEmail(email);
            log.info("Public support ticket from unknown email '{}' — stored as reporter", email);
        }

        SupportTicket saved = supportTicketRepository.save(builder.build());
        log.info("Public support ticket created — publicId: {}", saved.getPublicId());

        // actor=null / device=null: public flow, no authenticated user
        eventPublisher.publishEvent(new SupportTicketCreatedEvent(saved, null, null));
        return saved;
    }

    // =========================================================================
    // Creation & update
    // =========================================================================

    @Override
    @Transactional
    public SupportTicket create(SupportTicketCreateRequest request, User actor) {
        log.debug("Creating support ticket — subject: '{}', by: {}", request.subject(), actor.getUsername());

        Contact submittedBy = contactRepository.findByPublicId(request.submittedByPublicId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Contact not found with publicId: " + request.submittedByPublicId()));

        User assignedTo = null;
        if (request.assignedToPublicId() != null) {
            assignedTo = userRepository.findByPublicId(request.assignedToPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found with publicId: " + request.assignedToPublicId()));
        }

        SupportTicket ticket = SupportTicket.builder()
                .subject(request.subject())
                .description(request.description())
                .status(SupportTicketStatus.OPEN)
                .submittedBy(submittedBy)
                .assignedTo(assignedTo)
                .build();

        SupportTicket saved = supportTicketRepository.save(ticket);
        log.info("Support ticket created — publicId: {}, by: {}", saved.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new SupportTicketCreatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    @Override
    @Transactional
    public SupportTicket update(String publicId, SupportTicketUpdateRequest request, User actor) {
        log.debug("Updating support ticket — publicId: {}, by: {}", publicId, actor.getUsername());

        SupportTicket ticket = getByPublicId(publicId);

        if (ticket.isClosed()) {
            throw SupportTicketAlreadyClosedException.forTicket(publicId);
        }

        if (request.subject() != null) ticket.setSubject(request.subject());
        ticket.setDescription(request.description());

        SupportTicket saved = supportTicketRepository.save(ticket);
        log.info("Support ticket updated — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new SupportTicketUpdatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Deletion
    // =========================================================================

    @Override
    @Transactional
    public void delete(String publicId, User actor) {
        log.debug("Deleting support ticket — publicId: {}, by: {}", publicId, actor.getUsername());
        SupportTicket ticket = getByPublicId(publicId);
        supportTicketRepository.delete(ticket);
        log.info("Support ticket deleted — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new SupportTicketDeletedEvent(
                ticket, actor, deviceService.detectAndRegisterDevice(actor)));
    }

    // =========================================================================
    // Status
    // =========================================================================

    @Override
    @Transactional
    public SupportTicket changeStatus(String publicId, SupportTicketChangeStatusRequest request, User actor) {
        log.debug("Changing support ticket status — publicId: {}, target: {}, by: {}",
                publicId, request.status(), actor.getUsername());

        SupportTicket ticket = getByPublicId(publicId);

        if (ticket.isClosed()) {
            throw SupportTicketAlreadyClosedException.forTicket(publicId);
        }

        validateTransition(ticket.getStatus(), request.status());

        SupportTicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(request.status());
        SupportTicket saved = supportTicketRepository.save(ticket);

        log.info("Support ticket {} status changed: {} → {}, by: {}",
                publicId, previousStatus, request.status(), actor.getUsername());

        var device = deviceService.detectAndRegisterDevice(actor);
        eventPublisher.publishEvent(new SupportTicketStatusChangedEvent(
                saved, previousStatus, request.status(), actor, device));

        if (request.status() == SupportTicketStatus.CLOSED) {
            eventPublisher.publishEvent(new SupportTicketClosedEvent(saved, actor, device));
        }

        return saved;
    }

    // =========================================================================
    // Assignment
    // =========================================================================

    @Override
    @Transactional
    public SupportTicket assign(String publicId, SupportTicketAssignRequest request, User actor) {
        log.debug("Assigning support ticket — publicId: {}, assignee: {}, by: {}",
                publicId, request.assignedToPublicId(), actor.getUsername());

        SupportTicket ticket = getByPublicId(publicId);

        if (ticket.isClosed()) {
            throw SupportTicketAlreadyClosedException.forTicket(publicId);
        }

        if (!actor.hasAdminPrivileges() && !actor.getPublicId().equals(request.assignedToPublicId())) {
            throw new SupportTicketAssignNotAuthorizedException();
        }

        User previousAssignee = ticket.getAssignedTo();

        User newAssignee = null;
        if (request.assignedToPublicId() != null) {
            newAssignee = userRepository.findByPublicId(request.assignedToPublicId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User not found with publicId: " + request.assignedToPublicId()));
        }

        ticket.setAssignedTo(newAssignee);
        SupportTicket saved = supportTicketRepository.save(ticket);

        log.info("Support ticket {} assigned to {}, by: {}",
                publicId,
                newAssignee != null ? newAssignee.getPublicId() : "null (unassigned)",
                actor.getUsername());

        eventPublisher.publishEvent(new SupportTicketAssignedEvent(
                saved, newAssignee, previousAssignee, actor,
                deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Extracts the real client IP address, respecting reverse-proxy headers.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Returns a transient (unsaved) fake ticket for silent honeypot rejection.
     *
     * <p>The caller returns this to the client as if it were real — bots see success.</p>
     */
    private SupportTicket buildFakeTicket() {
        SupportTicket fake = new SupportTicket();
        fake.setSubject("honeypot");
        fake.setStatus(SupportTicketStatus.OPEN);
        fake.setSource(SupportTicketSource.PUBLIC_FORM);
        return fake;
    }

    /**
     * Validates that the requested status transition is allowed.
     *
     * <p>Allowed transitions:</p>
     * <ul>
     *   <li>OPEN → IN_PROGRESS, CLOSED</li>
     *   <li>IN_PROGRESS → RESOLVED, CLOSED</li>
     *   <li>RESOLVED → CLOSED</li>
     * </ul>
     *
     * @param from current status
     * @param to   target status
     * @throws SupportTicketStatusTransitionException if the transition is not allowed
     */
    private void validateTransition(SupportTicketStatus from, SupportTicketStatus to) {
        boolean allowed = switch (from) {
            case OPEN        -> to == SupportTicketStatus.IN_PROGRESS || to == SupportTicketStatus.CLOSED;
            case IN_PROGRESS -> to == SupportTicketStatus.RESOLVED    || to == SupportTicketStatus.CLOSED;
            case RESOLVED    -> to == SupportTicketStatus.CLOSED;
            case CLOSED      -> false;
        };

        if (!allowed) {
            throw SupportTicketStatusTransitionException.invalid(from, to);
        }
    }
}

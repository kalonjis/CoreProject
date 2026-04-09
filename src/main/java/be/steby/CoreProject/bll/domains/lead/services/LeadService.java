package be.steby.CoreProject.bll.domains.lead.services;

import be.steby.CoreProject.bll.domains.lead.models.*;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for managing leads throughout their full lifecycle.
 *
 * <h3>Public flow</h3>
 * <pre>
 * Anonymous visitor → {@link #submitInquiry}
 * </pre>
 *
 * <h3>CRM flow</h3>
 * <pre>
 * NEW ──► {@link #markInReview} ──► {@link #convert}
 *                              └──► {@link #reject}
 * </pre>
 *
 * <p>Assignment via {@link #assign} can occur at any non-terminal status.</p>
 */
public interface LeadService {

    // =========================================================================
    // Public submission
    // =========================================================================

    /**
     * Processes a public inquiry submission from an anonymous visitor.
     *
     * @param request     the inquiry form data
     * @param httpRequest the HTTP request (for IP extraction)
     * @return the result of the submission
     */
    LeadResult submitInquiry(LeadRequest request, HttpServletRequest httpRequest);

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Finds a lead by its public UUID.
     *
     * @param publicId the public UUID
     * @return the lead
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadNotFoundException if not found
     */
    Lead getByPublicId(String publicId);

    /**
     * Returns the full detail context for a lead, including deduplication info.
     *
     * <p>In addition to the lead entity, checks whether a Contact with the same
     * email address already exists in the CRM. If so, the contact's public UUID
     * is included so the frontend can disable the "Convert" action and link
     * directly to the existing contact.</p>
     *
     * @param publicId the public UUID of the lead
     * @return the lead wrapped with its optional existing-contact reference
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadNotFoundException if not found
     */
    LeadDetailModel getDetail(String publicId);

    /**
     * Returns a paginated, filtered list of leads for the admin queue.
     *
     * @param filter   the filter criteria (all fields optional)
     * @param pageable pagination and sorting parameters
     * @return a page of matching leads
     */
    Page<Lead> findAll(LeadFilterRequest filter, Pageable pageable);

    // =========================================================================
    // Manual creation
    // =========================================================================

    /**
     * Creates a lead manually from the CRM, bypassing public submission guards.
     *
     * <p>No honeypot check, no rate limiting, no email domain validation.
     * Source is always {@code MANUAL}. Status starts at {@code NEW}.</p>
     *
     * @param request the lead data entered by the commercial
     * @param actor   the commercial creating the lead
     * @return the newly created lead
     */
    Lead createManual(LeadManualCreateRequest request, User actor);

    /**
     * Creates a lead from an external platform webhook (Facebook, Typeform, etc.).
     *
     * <p>No honeypot check, no rate limiting, no actor required.
     * The {@code source} parameter identifies the originating platform.</p>
     *
     * @param request the normalized lead data produced by the platform mapper
     * @param source  the originating platform source
     * @return the newly created lead
     */
    Lead createFromWebhook(LeadManualCreateRequest request, LeadSource source);

    // =========================================================================
    // CRM lifecycle
    // =========================================================================

    /**
     * Enriches a lead with contact details provided or completed by the commercial.
     *
     * <p>Only updates fields that are non-null in the request.
     * Can be called at any non-terminal status.</p>
     *
     * @param publicId the public UUID of the lead
     * @param request  the enrichment data (all fields optional)
     * @return the updated lead
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadNotFoundException if not found
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyConvertedException if terminal
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyRejectedException if terminal
     */
    Lead enrich(String publicId, LeadEnrichRequest request);

    /**
     * Assigns or reassigns a lead to a commercial.
     *
     * <p>Can be called at any non-terminal status.
     * Publishes a {@link be.steby.CoreProject.bll.domains.lead.events.LeadAssignedEvent}.</p>
     *
     * @param publicId the public UUID of the lead
     * @param request  the assignment request
     * @param actor    the user performing the action
     * @return the updated lead
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadNotFoundException if not found
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyConvertedException if terminal
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyRejectedException if terminal
     */
    Lead assign(String publicId, LeadAssignRequest request, User actor);

    /**
     * Transitions a lead to {@code IN_REVIEW} status.
     *
     * <p>Publishes a {@link be.steby.CoreProject.bll.domains.lead.events.LeadInReviewEvent}.</p>
     *
     * @param publicId the public UUID of the lead
     * @param actor    the commercial opening the lead
     * @return the updated lead
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadNotFoundException if not found
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyConvertedException if terminal
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyRejectedException if terminal
     */
    Lead markInReview(String publicId, User actor);

    /**
     * Converts a lead into a Contact.
     *
     * <p>Transitions the lead to {@code CONVERTED}, records {@code convertedAt},
     * and publishes a {@link be.steby.CoreProject.bll.domains.lead.events.LeadConvertedEvent}
     * for the {@code contact} domain to consume.</p>
     *
     * @param publicId the public UUID of the lead
     * @param request  the conversion data used to create the Contact
     * @param actor    the commercial performing the conversion
     * @return the updated lead
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadNotFoundException if not found
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyConvertedException if already converted
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyRejectedException if already rejected
     */
    Lead convert(String publicId, LeadConvertRequest request, User actor);

    /**
     * Rejects a lead with a mandatory reason.
     *
     * <p>Transitions the lead to {@code REJECTED}, records {@code rejectionReason},
     * and publishes a {@link be.steby.CoreProject.bll.domains.lead.events.LeadRejectedEvent}.</p>
     *
     * @param publicId the public UUID of the lead
     * @param request  the rejection request containing the reason
     * @param actor    the commercial performing the rejection
     * @return the updated lead
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadNotFoundException if not found
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyConvertedException if already converted
     * @throws be.steby.CoreProject.bll.domains.lead.exceptions.LeadAlreadyRejectedException if already rejected
     */
    Lead reject(String publicId, LeadRejectRequest request, User actor);
}
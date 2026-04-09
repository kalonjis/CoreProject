package be.steby.CoreProject.pl.domains.contact.controllers;

import be.steby.CoreProject.bll.domains.crm.contact.services.ContactService;
import be.steby.CoreProject.bll.domains.crm.deal.services.DealService;
import be.steby.CoreProject.bll.domains.outreach.services.CrmOutreachService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.pl.domains.contact.models.requests.*;
import be.steby.CoreProject.pl.domains.contact.models.requests.SendContactEmailRequest;
import be.steby.CoreProject.pl.domains.contact.models.responses.ContactDetailResponse;
import be.steby.CoreProject.pl.domains.contact.models.responses.ContactSummaryResponse;
import be.steby.CoreProject.pl.domains.deal.models.responses.DealSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for CRM contact management operations.
 *
 * <p>Exposes the full commercial lifecycle of a contact to authenticated
 * users with {@code COMMERCIAL} or {@code ADMIN} authority:</p>
 * <ul>
 *   <li>Paginated list with filtering</li>
 *   <li>Contact detail view</li>
 *   <li>Manual creation, partial update</li>
 *   <li>CRM status transition</li>
 *   <li>Organisation linking/unlinking</li>
 *   <li>Commercial assignment</li>
 *   <li>Duplicate merge</li>
 * </ul>
 *
 * <p>All business logic is delegated to {@link ContactService}.
 * This controller only handles HTTP concerns.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/contacts</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 */
@RestController
@RequestMapping("/api/crm/contacts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Contacts", description = "Contact management and CRM lifecycle")
public class CrmContactController {

    private final ContactService      contactService;
    private final DealService         dealService;
    private final CrmOutreachService  outreachService;

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Returns a paginated, filtered list of contacts.
     *
     * <p>All filter parameters are optional — omitting them returns all contacts.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/contacts</p>
     *
     * @param filter   optional filter criteria as query parameters
     * @param pageable pagination and sorting (default: 20 per page, by last name ascending)
     * @return paginated list of contact summaries
     */
    @GetMapping
    @Operation(summary = "List contacts", description = "Returns a paginated, filtered list of contacts")
    public ResponseEntity<Page<ContactSummaryResponse>> findAll(
            @ModelAttribute ContactListFilterRequest filter,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.debug("CRM contact list requested — filter: {}", filter);

        Page<ContactSummaryResponse> page = contactService
                .findAll(filter.toBllModel(), pageable)
                .map(ContactSummaryResponse::fromEntity);

        return ResponseEntity.ok(page);
    }

    /**
     * Returns the full detail of a single contact.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/contacts/{publicId}</p>
     *
     * @param publicId the public UUID of the contact
     * @return the contact detail
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get contact", description = "Returns the full detail of a contact")
    public ResponseEntity<ContactDetailResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("CRM contact detail requested — publicId: {}", publicId);

        Contact contact = contactService.getByPublicId(publicId);
        return ResponseEntity.ok(ContactDetailResponse.fromEntity(contact));
    }

    /**
     * Returns the contact that was converted from a given lead.
     *
     * <p>Called immediately after lead conversion so the frontend can
     * pre-fill a Deal creation form with the new contact's publicId.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/contacts/from-lead/{leadPublicId}</p>
     *
     * @param leadPublicId the public UUID of the origin lead
     * @return the contact detail
     */
    @GetMapping("/from-lead/{leadPublicId}")
    @Operation(summary = "Get contact from lead", description = "Returns the contact created from a given lead conversion")
    public ResponseEntity<ContactDetailResponse> getFromLead(@PathVariable String leadPublicId) {
        log.debug("CRM contact by origin lead requested — leadPublicId: {}", leadPublicId);

        Contact contact = contactService.getByOriginLeadPublicId(leadPublicId);
        return ResponseEntity.ok(ContactDetailResponse.fromEntity(contact));
    }

    /**
     * Returns all deals involving a given contact (in any role).
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/contacts/{publicId}/deals</p>
     *
     * @param publicId the public UUID of the contact
     * @return list of deal summaries for that contact, newest first
     */
    @GetMapping("/{publicId}/deals")
    @Operation(summary = "Get contact deals", description = "Returns all deals involving a contact in any role")
    public ResponseEntity<List<DealSummaryResponse>> getDeals(@PathVariable String publicId) {
        log.debug("CRM contact deals requested — publicId: {}", publicId);

        List<DealSummaryResponse> deals = dealService.findByContact(publicId)
                .stream()
                .map(DealSummaryResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(deals);
    }

    // =========================================================================
    // Creation & update
    // =========================================================================

    /**
     * Creates a contact manually, without going through the lead flow.
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/contacts</p>
     *
     * @param request the contact creation data
     * @param actor   the authenticated user performing the action
     * @return 201 Created with the new contact detail
     */
    @PostMapping
    @Operation(summary = "Create contact", description = "Creates a contact manually")
    public ResponseEntity<ContactDetailResponse> create(
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Contact creation requested — email: {}, by: {}", request.email(), actor.getUsername());

        Contact contact = contactService.create(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/contacts/" + contact.getPublicId());
        return ResponseEntity.created(location).body(ContactDetailResponse.fromEntity(contact));
    }

    /**
     * Partially updates an existing contact's editable fields.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/contacts/{publicId}</p>
     *
     * @param publicId the public UUID of the contact to update
     * @param request  the partial update request
     * @param actor    the authenticated user performing the action
     * @return the updated contact detail
     */
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update contact", description = "Partially updates an existing contact")
    public ResponseEntity<ContactDetailResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateContactRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Contact update requested — publicId: {}, by: {}", publicId, actor.getUsername());

        Contact contact = contactService.update(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(ContactDetailResponse.fromEntity(contact));
    }

    // =========================================================================
    // Lifecycle & relations
    // =========================================================================

    /**
     * Transitions a contact to a new CRM lifecycle status.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/contacts/{publicId}/status</p>
     *
     * @param publicId the public UUID of the contact
     * @param request  the status transition request
     * @param actor    the authenticated user performing the action
     * @return the updated contact detail
     */
    @PatchMapping("/{publicId}/status")
    @Operation(summary = "Update contact status", description = "Transitions a contact to a new CRM lifecycle status")
    public ResponseEntity<ContactDetailResponse> updateStatus(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateContactStatusRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Contact status update requested — publicId: {}, status: {}, by: {}",
                publicId, request.status(), actor.getUsername());

        Contact contact = contactService.updateStatus(publicId, request.status(), actor);
        return ResponseEntity.ok(ContactDetailResponse.fromEntity(contact));
    }

    /**
     * Links or unlinks an organisation to a contact.
     *
     * <p>Passing {@code null} as {@code organisationPublicId} removes the current link.</p>
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/contacts/{publicId}/organisation</p>
     *
     * @param publicId the public UUID of the contact
     * @param request  the link request (organisationPublicId may be null to unlink)
     * @param actor    the authenticated user performing the action
     * @return the updated contact detail
     */
    @PatchMapping("/{publicId}/organisation")
    @Operation(summary = "Link organisation", description = "Links or unlinks an organisation to a contact")
    public ResponseEntity<ContactDetailResponse> linkOrganisation(
            @PathVariable String publicId,
            @RequestBody LinkContactOrganisationRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Contact organisation link requested — publicId: {}, org: {}, by: {}",
                publicId, request.organisationPublicId(), actor.getUsername());

        Contact contact = contactService.linkOrganisation(publicId, request.organisationPublicId(), actor);
        return ResponseEntity.ok(ContactDetailResponse.fromEntity(contact));
    }

    /**
     * Assigns or unassigns a commercial to a contact.
     *
     * <p>Passing {@code null} as {@code commercialPublicId} removes the current assignee.</p>
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/contacts/{publicId}/assign</p>
     *
     * @param publicId the public UUID of the contact
     * @param request  the assignment request (commercialPublicId may be null to unassign)
     * @param actor    the authenticated user performing the action
     * @return the updated contact detail
     */
    @PatchMapping("/{publicId}/assign")
    @Operation(summary = "Assign contact", description = "Assigns or unassigns a commercial to a contact")
    public ResponseEntity<ContactDetailResponse> assign(
            @PathVariable String publicId,
            @RequestBody AssignContactRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Contact assignment requested — publicId: {}, commercial: {}, by: {}",
                publicId, request.commercialPublicId(), actor.getUsername());

        Contact contact = contactService.assign(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(ContactDetailResponse.fromEntity(contact));
    }

    // =========================================================================
    // Outreach
    // =========================================================================

    /**
     * Sends a CRM outreach email from the authenticated commercial to a contact,
     * and automatically logs it as an {@code EMAIL} interaction in the timeline.
     *
     * <p>The email is sent with the commercial's email address as the {@code Reply-To}
     * header, so the contact's reply goes directly to the commercial's inbox.</p>
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/contacts/{publicId}/email</p>
     *
     * @param publicId the public UUID of the target contact
     * @param request  the email subject and body
     * @param actor    the authenticated commercial sending the email
     * @return 204 No Content on success
     */
    @PostMapping("/{publicId}/email")
    @Operation(summary = "Send email to contact",
               description = "Sends an outreach email to a contact and logs it as an interaction")
    public ResponseEntity<Void> sendEmail(
            @PathVariable String publicId,
            @Valid @RequestBody SendContactEmailRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("CRM outreach requested — contact: {}, subject: '{}', by: {}",
                publicId, request.subject(), actor.getUsername());

        outreachService.send(request.toBllModel(publicId), actor);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Merge
    // =========================================================================

    /**
     * Merges two duplicate contacts into one surviving record.
     *
     * <p>All deals and interactions linked to the source are reassigned to the target.
     * The source contact is archived (status set to {@code INACTIVE}) after the merge.</p>
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/contacts/merge</p>
     *
     * @param request the merge request containing source and target public UUIDs
     * @param actor   the authenticated user performing the action
     * @return the surviving target contact detail
     */
    @PostMapping("/merge")
    @Operation(summary = "Merge contacts", description = "Merges two duplicate contacts into one surviving record")
    public ResponseEntity<ContactDetailResponse> merge(
            @Valid @RequestBody MergeContactRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Contact merge requested — source: {}, target: {}, by: {}",
                request.sourcePublicId(), request.targetPublicId(), actor.getUsername());

        Contact contact = contactService.merge(request.toBllModel(), actor);
        return ResponseEntity.ok(ContactDetailResponse.fromEntity(contact));
    }
}

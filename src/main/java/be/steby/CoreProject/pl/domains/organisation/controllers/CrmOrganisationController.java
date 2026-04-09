package be.steby.CoreProject.pl.domains.organisation.controllers;

import be.steby.CoreProject.bll.domains.crm.contact.services.ContactService;
import be.steby.CoreProject.bll.domains.crm.deal.services.DealService;
import be.steby.CoreProject.bll.domains.organisation.services.OrganisationService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.pl.domains.contact.models.responses.ContactSummaryResponse;
import be.steby.CoreProject.pl.domains.deal.models.responses.DealSummaryResponse;
import be.steby.CoreProject.pl.domains.organisation.models.requests.CreateOrganisationRequest;
import be.steby.CoreProject.pl.domains.organisation.models.requests.MergeOrganisationRequest;
import be.steby.CoreProject.pl.domains.organisation.models.requests.OrganisationListFilterRequest;
import be.steby.CoreProject.pl.domains.organisation.models.requests.UpdateOrganisationRequest;
import be.steby.CoreProject.pl.domains.organisation.models.requests.UpdateOrganisationStatusRequest;
import be.steby.CoreProject.pl.domains.organisation.models.responses.OrganisationDetailResponse;
import be.steby.CoreProject.pl.domains.organisation.models.responses.OrganisationSummaryResponse;
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
 * REST controller for CRM organisation management operations.
 *
 * <p>Exposes CRUD and merge operations on organisations to authenticated
 * users with {@code COMMERCIAL} or {@code ADMIN} authority:</p>
 * <ul>
 *   <li>Paginated list with filtering</li>
 *   <li>Organisation detail view</li>
 *   <li>Creation, partial update</li>
 *   <li>Duplicate merge</li>
 * </ul>
 *
 * <p>All business logic is delegated to {@link OrganisationService}.
 * This controller only handles HTTP concerns.</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/organisations</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 */
@RestController
@RequestMapping("/api/crm/organisations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Organisations", description = "Organisation management")
public class CrmOrganisationController {

    private final OrganisationService organisationService;
    private final ContactService      contactService;
    private final DealService         dealService;

    // =========================================================================
    // Lookup
    // =========================================================================

    /**
     * Returns a paginated, filtered list of organisations.
     *
     * <p>All filter parameters are optional — omitting them returns all organisations.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/organisations</p>
     *
     * @param filter   optional filter criteria as query parameters
     * @param pageable pagination and sorting (default: 20 per page, by name ascending)
     * @return paginated list of organisation summaries
     */
    @GetMapping
    @Operation(summary = "List organisations", description = "Returns a paginated, filtered list of organisations")
    public ResponseEntity<Page<OrganisationSummaryResponse>> findAll(
            @ModelAttribute OrganisationListFilterRequest filter,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.debug("CRM organisation list requested — filter: {}", filter);

        Page<OrganisationSummaryResponse> page = organisationService
                .findAll(filter.toBllModel(), pageable)
                .map(OrganisationSummaryResponse::fromEntity);

        return ResponseEntity.ok(page);
    }

    /**
     * Returns the full detail of a single organisation.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/organisations/{publicId}</p>
     *
     * @param publicId the public UUID of the organisation
     * @return the organisation detail
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get organisation", description = "Returns the full detail of an organisation")
    public ResponseEntity<OrganisationDetailResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("CRM organisation detail requested — publicId: {}", publicId);

        Organisation organisation = organisationService.getByPublicId(publicId);
        return ResponseEntity.ok(OrganisationDetailResponse.fromEntity(organisation));
    }

    /**
     * Returns all contacts linked to a given organisation.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/organisations/{publicId}/contacts</p>
     *
     * @param publicId the public UUID of the organisation
     * @return list of contact summaries for that organisation
     */
    @GetMapping("/{publicId}/contacts")
    @Operation(summary = "Get organisation contacts", description = "Returns all contacts linked to an organisation")
    public ResponseEntity<List<ContactSummaryResponse>> getContacts(@PathVariable String publicId) {
        log.debug("CRM organisation contacts requested — publicId: {}", publicId);

        List<ContactSummaryResponse> contacts = contactService.findByOrganisation(publicId)
                .stream()
                .map(ContactSummaryResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(contacts);
    }

    /**
     * Returns all deals linked to a given organisation.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/organisations/{publicId}/deals</p>
     *
     * @param publicId the public UUID of the organisation
     * @return list of deal summaries for that organisation, newest first
     */
    @GetMapping("/{publicId}/deals")
    @Operation(summary = "Get organisation deals", description = "Returns all deals linked to an organisation")
    public ResponseEntity<List<DealSummaryResponse>> getDeals(@PathVariable String publicId) {
        log.debug("CRM organisation deals requested — publicId: {}", publicId);

        List<DealSummaryResponse> deals = dealService.findByOrganisation(publicId)
                .stream()
                .map(DealSummaryResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(deals);
    }

    // =========================================================================
    // Creation & update
    // =========================================================================

    /**
     * Creates a new organisation.
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/organisations</p>
     *
     * @param request the organisation creation data
     * @param actor   the authenticated user performing the action
     * @return 201 Created with the new organisation detail
     */
    @PostMapping
    @Operation(summary = "Create organisation", description = "Creates a new organisation")
    public ResponseEntity<OrganisationDetailResponse> create(
            @Valid @RequestBody CreateOrganisationRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Organisation creation requested — name: {}, by: {}", request.name(), actor.getUsername());

        Organisation organisation = organisationService.create(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/organisations/" + organisation.getPublicId());
        return ResponseEntity.created(location).body(OrganisationDetailResponse.fromEntity(organisation));
    }

    /**
     * Partially updates an existing organisation.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/organisations/{publicId}</p>
     *
     * @param publicId the public UUID of the organisation to update
     * @param request  the partial update request
     * @param actor    the authenticated user performing the action
     * @return the updated organisation detail
     */
    @PatchMapping("/{publicId}")
    @Operation(summary = "Update organisation", description = "Partially updates an existing organisation")
    public ResponseEntity<OrganisationDetailResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateOrganisationRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Organisation update requested — publicId: {}, by: {}", publicId, actor.getUsername());

        Organisation organisation = organisationService.update(publicId, request.toBllModel(), actor);
        return ResponseEntity.ok(OrganisationDetailResponse.fromEntity(organisation));
    }

    /**
     * Manually updates the lifecycle status of an organisation.
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/organisations/{publicId}/status</p>
     *
     * @param publicId the public UUID of the organisation
     * @param request  the new status
     * @param actor    the authenticated user performing the action
     * @return the updated organisation detail
     */
    @PatchMapping("/{publicId}/status")
    @Operation(summary = "Update organisation status", description = "Manually sets the lifecycle status of an organisation")
    public ResponseEntity<OrganisationDetailResponse> updateStatus(
            @PathVariable String publicId,
            @Valid @RequestBody UpdateOrganisationStatusRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Organisation status update requested — publicId: {}, status: {}, by: {}",
                publicId, request.status(), actor.getUsername());

        Organisation organisation = organisationService.updateStatus(publicId, request.status(), actor);
        return ResponseEntity.ok(OrganisationDetailResponse.fromEntity(organisation));
    }

    // =========================================================================
    // Merge
    // =========================================================================

    /**
     * Merges two duplicate organisations into one surviving record.
     *
     * <p>All contacts linked to the source are reassigned to the target.
     * The source organisation is removed after the merge.</p>
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/organisations/merge</p>
     *
     * @param request the merge request containing source and target public UUIDs
     * @param actor   the authenticated user performing the action
     * @return the surviving target organisation detail
     */
    @PostMapping("/merge")
    @Operation(summary = "Merge organisations", description = "Merges two duplicate organisations into one surviving record")
    public ResponseEntity<OrganisationDetailResponse> merge(
            @Valid @RequestBody MergeOrganisationRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Organisation merge requested — source: {}, target: {}, by: {}",
                request.sourcePublicId(), request.targetPublicId(), actor.getUsername());

        Organisation organisation = organisationService.merge(request.toBllModel(), actor);
        return ResponseEntity.ok(OrganisationDetailResponse.fromEntity(organisation));
    }
}

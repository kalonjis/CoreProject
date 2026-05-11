package be.steby.CoreProject.pl.domains.timeline.controllers;

import be.steby.CoreProject.bll.domains.crm.timeline.services.TimelineService;
import be.steby.CoreProject.pl.domains.timeline.models.responses.TimelineEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the unified CRM activity timeline.
 *
 * <p>Exposes timeline views that merge spontaneous {@link be.steby.CoreProject.dl.entities.crm.Interaction}
 * entries and completed {@link be.steby.CoreProject.dl.entities.crm.CommercialAction} entries
 * into a single chronological feed — one endpoint per CRM entity context
 * (deal, contact, lead).</p>
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/timeline</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 */
@RestController
@RequestMapping("/api/crm/timeline")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Timeline", description = "Unified activity timeline (interactions + completed actions)")
public class CrmTimelineController {

    private final TimelineService timelineService;

    /**
     * Returns the unified activity timeline for a deal, most recent first.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/timeline/deal/{dealPublicId}</p>
     *
     * @param dealPublicId the public UUID of the deal
     * @return merged list of interactions and completed commercial actions
     */
    @GetMapping("/deal/{dealPublicId}")
    @Operation(
            summary = "Deal timeline",
            description = "Unified timeline for a deal: interactions + completed commercial actions, most recent first"
    )
    public ResponseEntity<List<TimelineEntryResponse>> getTimelineByDeal(@PathVariable String dealPublicId) {
        log.debug("CRM timeline requested for deal: {}", dealPublicId);

        List<TimelineEntryResponse> timeline = timelineService.getTimelineByDeal(dealPublicId)
                .stream()
                .map(TimelineEntryResponse::fromEntry)
                .toList();

        return ResponseEntity.ok(timeline);
    }

    /**
     * Returns the unified activity timeline for a contact, most recent first.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/timeline/contact/{contactPublicId}</p>
     *
     * @param contactPublicId the public UUID of the contact
     * @return merged list of interactions and completed commercial actions
     */
    @GetMapping("/contact/{contactPublicId}")
    @Operation(
            summary = "Contact timeline",
            description = "Unified timeline for a contact: interactions + completed commercial actions, most recent first"
    )
    public ResponseEntity<List<TimelineEntryResponse>> getTimelineByContact(@PathVariable String contactPublicId) {
        log.debug("CRM timeline requested for contact: {}", contactPublicId);

        List<TimelineEntryResponse> timeline = timelineService.getTimelineByContact(contactPublicId)
                .stream()
                .map(TimelineEntryResponse::fromEntry)
                .toList();

        return ResponseEntity.ok(timeline);
    }

    /**
     * Returns the unified activity timeline for a lead, most recent first.
     *
     * <p>Used during lead qualification — before conversion to a Contact.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/timeline/lead/{leadPublicId}</p>
     *
     * @param leadPublicId the public UUID of the lead
     * @return merged list of interactions and completed commercial actions
     */
    @GetMapping("/lead/{leadPublicId}")
    @Operation(
            summary = "Lead timeline",
            description = "Unified timeline for a lead: interactions + completed commercial actions, most recent first"
    )
    public ResponseEntity<List<TimelineEntryResponse>> getTimelineByLead(@PathVariable String leadPublicId) {
        log.debug("CRM timeline requested for lead: {}", leadPublicId);

        List<TimelineEntryResponse> timeline = timelineService.getTimelineByLead(leadPublicId)
                .stream()
                .map(TimelineEntryResponse::fromEntry)
                .toList();

        return ResponseEntity.ok(timeline);
    }

    /**
     * Returns the aggregated activity timeline for all contacts of an organisation, most recent first.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/timeline/organisation/{organisationPublicId}</p>
     *
     * @param organisationPublicId the public UUID of the organisation
     * @return merged timeline entries across all org contacts
     */
    @GetMapping("/organisation/{organisationPublicId}")
    @Operation(
            summary = "Organisation timeline",
            description = "Aggregated timeline across all contacts of an organisation: interactions + completed commercial actions, most recent first"
    )
    public ResponseEntity<List<TimelineEntryResponse>> getTimelineByOrganisation(
            @PathVariable String organisationPublicId) {
        log.debug("CRM timeline requested for organisation: {}", organisationPublicId);

        List<TimelineEntryResponse> timeline = timelineService.getTimelineByOrganisation(organisationPublicId)
                .stream()
                .map(TimelineEntryResponse::fromEntry)
                .toList();

        return ResponseEntity.ok(timeline);
    }
}

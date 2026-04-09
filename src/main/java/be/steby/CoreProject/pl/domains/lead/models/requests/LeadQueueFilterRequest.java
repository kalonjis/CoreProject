package be.steby.CoreProject.pl.domains.lead.models.requests;

import be.steby.CoreProject.bll.domains.crm.lead.models.LeadFilterRequest;
import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * PL request model for filtering the admin lead queue.
 *
 * <p>All fields are optional — passed as query parameters on
 * {@code GET /api/crm/leads}. {@code null} means no restriction
 * on that criterion.</p>
 *
 * @param status             filter by CRM processing status
 * @param leadType           filter by inquiry type
 * @param leadSource         filter by acquisition source
 * @param assignedToPublicId filter by assigned commercial (public UUID)
 * @param unassignedOnly     if {@code true}, returns only unassigned leads
 * @param activeOnly         if {@code true}, returns only NEW and IN_REVIEW leads
 * @param keyword            search term matched against email and name
 * @param submittedFrom      start of submission date range (inclusive)
 * @param submittedTo        end of submission date range (inclusive)
 */
public record LeadQueueFilterRequest(

        LeadStatus status,
        LeadType leadType,
        LeadSource leadSource,
        String assignedToPublicId,
        Boolean unassignedOnly,
        Boolean activeOnly,
        String keyword,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant submittedFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant submittedTo

) {

    /**
     * Converts this PL request to the BLL filter model.
     *
     * @return {@link LeadFilterRequest} for the service layer
     */
    public LeadFilterRequest toBllModel() {
        return new LeadFilterRequest(
                status,
                leadType,
                leadSource,
                assignedToPublicId,
                unassignedOnly,
                activeOnly,
                keyword,
                submittedFrom,
                submittedTo
        );
    }
}
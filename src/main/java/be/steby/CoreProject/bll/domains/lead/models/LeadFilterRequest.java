package be.steby.CoreProject.bll.domains.lead.models;

import be.steby.CoreProject.dl.enums.LeadType;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;

import java.time.Instant;

/**
 * Filter criteria for querying leads in the admin queue.
 *
 * <p>All fields are optional — {@code null} means "no restriction" on that criterion.
 * Criteria are combined with AND logic via {@code LeadSpecification}.</p>
 *
 * <p>Mirrors the available predicates in
 * {@link be.steby.CoreProject.dal.specifications.crm.LeadSpecification}.</p>
 *
 * @param status             filter by CRM processing status
 * @param leadType           filter by inquiry type
 * @param assignedToPublicId filter by assigned commercial (public UUID)
 * @param unassignedOnly     if {@code true}, returns only unassigned leads
 * @param keyword            search term matched against email and name (case-insensitive)
 * @param submittedFrom      start of submission date range (inclusive)
 * @param submittedTo        end of submission date range (inclusive)
 */
public record LeadFilterRequest(

        LeadStatus status,
        LeadType leadType,
        String assignedToPublicId,
        Boolean unassignedOnly,
        String keyword,
        Instant submittedFrom,
        Instant submittedTo

) {}
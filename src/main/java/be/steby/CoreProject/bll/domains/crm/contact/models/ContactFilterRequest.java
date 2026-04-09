package be.steby.CoreProject.bll.domains.crm.contact.models;

import be.steby.CoreProject.dl.enums.crm.ContactStatus;

/**
 * BLL request model for filtering {@link be.steby.CoreProject.dl.entities.crm.Contact}
 * entities in paginated list queries.
 *
 * <p>All fields are optional. The service layer passes this request to
 * {@link be.steby.CoreProject.dal.specifications.crm.ContactSpecification}
 * to build a dynamic {@code Specification} — {@code null} fields are ignored.</p>
 *
 * <h3>Mapping to specifications</h3>
 * <ul>
 *   <li>{@code keyword}               → {@code ContactSpecification.nameOrEmailContains}</li>
 *   <li>{@code status}                → {@code ContactSpecification.hasStatus}</li>
 *   <li>{@code organisationPublicId}  → {@code ContactSpecification.belongsToOrganisation}</li>
 *   <li>{@code withoutOrganisation}   → {@code ContactSpecification.withoutOrganisation}</li>
 *   <li>{@code assignedToPublicId}    → resolved to internal ID by the service layer</li>
 *   <li>{@code hasLinkedUser}         → {@code ContactSpecification.hasLinkedUser}</li>
 *   <li>{@code convertedFromLead}     → {@code ContactSpecification.convertedFromLead}</li>
 *   <li>{@code tagPublicId}           → resolved to internal ID → {@code ContactSpecification.hasTag}</li>
 * </ul>
 *
 * @param keyword              search term matched against first name, last name, and email (optional)
 * @param status               filter by CRM lifecycle status (optional)
 * @param organisationPublicId filter contacts belonging to a specific organisation (optional)
 * @param withoutOrganisation  if {@code true}, returns only independent contacts (optional)
 * @param assignedToPublicId   filter contacts assigned to a specific commercial (optional)
 * @param hasLinkedUser        if {@code true}, returns only contacts linked to a platform account (optional)
 * @param convertedFromLead    if {@code true}, returns only contacts converted from a lead (optional)
 * @param tagPublicId          filter contacts that have a specific tag applied (optional)
 */
public record ContactFilterRequest(
        String keyword,
        ContactStatus status,
        String organisationPublicId,
        Boolean withoutOrganisation,
        String assignedToPublicId,
        Boolean hasLinkedUser,
        Boolean convertedFromLead,
        String tagPublicId
) {}
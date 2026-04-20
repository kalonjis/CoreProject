package be.steby.CoreProject.bll.domains.crm.contact.models;

/**
 * BLL request model for linking or unlinking a
 * {@link be.steby.CoreProject.dl.entities.crm.Contact} to an
 * {@link be.steby.CoreProject.dl.entities.crm.Organisation}.
 *
 * <p>The {@link be.steby.CoreProject.dl.entities.crm.Contact} entity owns the
 * relationship — this operation updates {@code Contact.organisation} directly.
 * The {@code OrganisationService} is not involved.</p>
 *
 * <h3>Unlinking</h3>
 * <p>Passing {@code null} as {@code organisationPublicId} removes the current
 * organisation link, making the contact independent (e.g., sole trader,
 * freelancer). The service layer publishes a distinct event in this case.</p>
 *
 * @param organisationPublicId public UUID of the organisation to link,
 *                             or {@code null} to unlink
 */
public record ContactLinkOrganisationRequest(
        String organisationPublicId
) {}
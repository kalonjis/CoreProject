package be.steby.CoreProject.dal.specifications.crm;

import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.dl.enums.crm.SupportTicketSource;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for dynamic filtering of {@link SupportTicket} entities.
 *
 * <p>Each static method returns a {@link Specification} that can be combined
 * with others to build complex ticket list filters at runtime.</p>
 *
 * <h3>Null safety</h3>
 * <p>All methods return {@code null} when the filter value is {@code null},
 * which JPA treats as "no restriction" — safe to chain unconditionally.</p>
 */
public class SupportTicketSpecification {

    private SupportTicketSpecification() {}

    /**
     * Filters tickets whose subject contains the given keyword (case-insensitive).
     *
     * @param keyword the search term, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<SupportTicket> subjectContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.like(cb.lower(root.get("subject")), pattern);
        };
    }

    /**
     * Filters tickets by their lifecycle status.
     *
     * @param status the status to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<SupportTicket> hasStatus(SupportTicketStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filters tickets by their creation source.
     *
     * @param source the source to filter by, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<SupportTicket> hasSource(SupportTicketSource source) {
        if (source == null) return null;
        return (root, query, cb) -> cb.equal(root.get("source"), source);
    }

    /**
     * Filters tickets submitted by a specific contact.
     *
     * @param contactId the internal ID of the contact, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<SupportTicket> submittedBy(Long contactId) {
        if (contactId == null) return null;
        return (root, query, cb) ->
            cb.equal(root.get("submittedBy").get("id"), contactId);
    }

    /**
     * Filters tickets assigned to a specific team member.
     *
     * @param assignedToId the internal ID of the user, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<SupportTicket> assignedTo(Long assignedToId) {
        if (assignedToId == null) return null;
        return (root, query, cb) ->
            cb.equal(root.get("assignedTo").get("id"), assignedToId);
    }

    /**
     * Filters tickets that are not yet assigned to anyone.
     *
     * @param unassignedOnly if {@code true}, returns only unassigned tickets
     * @return the specification, or {@code null}
     */
    public static Specification<SupportTicket> isUnassigned(Boolean unassignedOnly) {
        if (!Boolean.TRUE.equals(unassignedOnly)) return null;
        return (root, query, cb) -> cb.isNull(root.get("assignedTo"));
    }

    /**
     * Filters tickets submitted by contacts belonging to a given organisation.
     *
     * <p>Uses INNER JOINs through {@code submittedBy → organisation}, so tickets
     * with no contact or with a contact unlinked from any organisation are excluded.</p>
     *
     * @param organisationPublicId the public UUID of the organisation, or {@code null} to skip
     * @return the specification, or {@code null}
     */
    public static Specification<SupportTicket> submittedByOrganisation(String organisationPublicId) {
        if (organisationPublicId == null || organisationPublicId.isBlank()) return null;
        return (root, query, cb) -> {
            var contact = root.join("submittedBy", JoinType.INNER);
            var org     = contact.join("organisation", JoinType.INNER);
            return cb.equal(org.get("publicId"), organisationPublicId);
        };
    }
}

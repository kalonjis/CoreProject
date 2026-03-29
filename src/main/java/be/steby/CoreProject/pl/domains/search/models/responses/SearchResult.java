package be.steby.CoreProject.pl.domains.search.models.responses;

/**
 * A single CRM search hit, normalised across all entity types.
 *
 * @param type     Entity type: CONTACT, ORGANISATION, DEAL, LEAD
 * @param publicId Public UUID — used to build the navigation URL on the client
 * @param label    Primary display string (full name, title, etc.)
 * @param subtitle Secondary information (email, stage, status, etc.)
 */
public record SearchResult(
        String type,
        String publicId,
        String label,
        String subtitle
) {}

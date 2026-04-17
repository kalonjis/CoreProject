package be.steby.CoreProject.bll.domains.crm.search.services;

import be.steby.CoreProject.pl.domains.search.models.responses.GlobalSearchResponse;

/**
 * Service contract for cross-entity CRM global search.
 *
 * <p>Searches across contacts, organisations, deals, and leads simultaneously,
 * returning a unified list of ranked results.</p>
 */
public interface GlobalSearchService {

    /**
     * Searches all major CRM entities for the given keyword.
     *
     * <p>Returns an empty result set if {@code query} is null or blank.
     * Results are limited to {@code MAX_PER_CATEGORY} entries per entity type.</p>
     *
     * @param query the search keyword (case-insensitive, partial match)
     * @return a {@link GlobalSearchResponse} aggregating results from all entity types
     */
    GlobalSearchResponse search(String query);
}

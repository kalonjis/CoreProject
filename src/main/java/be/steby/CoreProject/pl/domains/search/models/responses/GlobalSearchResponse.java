package be.steby.CoreProject.pl.domains.search.models.responses;

import java.util.List;

/** Response returned by {@code GET /api/crm/search?q=}. */
public record GlobalSearchResponse(List<SearchResult> results) {}

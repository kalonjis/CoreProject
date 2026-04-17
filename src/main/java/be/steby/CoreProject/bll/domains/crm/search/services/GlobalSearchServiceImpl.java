package be.steby.CoreProject.bll.domains.crm.search.services;

import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.Lead;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.pl.domains.search.models.responses.GlobalSearchResponse;
import be.steby.CoreProject.pl.domains.search.models.responses.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link GlobalSearchService}.
 *
 * <p>Executes four parallel LIKE queries (contacts, organisations, deals, leads)
 * each capped at {@link #MAX_PER_CATEGORY} results, then merges them into a single flat list.
 * The keyword is lowercased and wrapped in SQL wildcards before querying.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalSearchServiceImpl implements GlobalSearchService {

    /** Maximum number of results returned per entity category. */
    private static final int MAX_PER_CATEGORY = 5;

    private final ContactRepository      contactRepository;
    private final OrganisationRepository organisationRepository;
    private final DealRepository         dealRepository;
    private final LeadRepository         leadRepository;

    /** {@inheritDoc} */
    @Override
    public GlobalSearchResponse search(String query) {
        if (query == null || query.isBlank()) return new GlobalSearchResponse(List.of());

        String kw = "%" + query.toLowerCase().trim() + "%";
        var page  = PageRequest.of(0, MAX_PER_CATEGORY);

        List<SearchResult> results = new ArrayList<>();

        contactRepository.searchByKeyword(kw, page).stream()
                .map(this::toContactResult).forEach(results::add);

        organisationRepository.searchByKeyword(kw, page).stream()
                .map(this::toOrganisationResult).forEach(results::add);

        dealRepository.searchByKeyword(kw, page).stream()
                .map(this::toDealResult).forEach(results::add);

        leadRepository.searchByKeyword(kw, page).stream()
                .map(this::toLeadResult).forEach(results::add);

        return new GlobalSearchResponse(results);
    }

    /**
     * Maps a {@link Contact} entity to a {@link SearchResult} of type {@code CONTACT}.
     *
     * @param c the contact entity
     * @return the corresponding search result
     */
    private SearchResult toContactResult(Contact c) {
        return new SearchResult(
                "CONTACT",
                c.getPublicId(),
                c.getFirstName() + " " + c.getLastName(),
                c.getEmail() != null ? c.getEmail() : ""
        );
    }

    /**
     * Maps an {@link Organisation} entity to a {@link SearchResult} of type {@code ORGANISATION}.
     *
     * @param o the organisation entity
     * @return the corresponding search result
     */
    private SearchResult toOrganisationResult(Organisation o) {
        return new SearchResult(
                "ORGANISATION",
                o.getPublicId(),
                o.getName(),
                o.getIndustry() != null ? o.getIndustry() : ""
        );
    }

    /**
     * Maps a {@link Deal} entity to a {@link SearchResult} of type {@code DEAL}.
     *
     * @param d the deal entity
     * @return the corresponding search result
     */
    private SearchResult toDealResult(Deal d) {
        return new SearchResult(
                "DEAL",
                d.getPublicId(),
                d.getTitle(),
                d.getPipelineStep() != null ? d.getPipelineStep().getName() : ""
        );
    }

    /**
     * Maps a {@link Lead} entity to a {@link SearchResult} of type {@code LEAD}.
     *
     * @param l the lead entity
     * @return the corresponding search result
     */
    private SearchResult toLeadResult(Lead l) {
        return new SearchResult(
                "LEAD",
                l.getPublicId(),
                l.getDisplayName().orElse(l.getEmail()),
                l.getEmail()
        );
    }
}

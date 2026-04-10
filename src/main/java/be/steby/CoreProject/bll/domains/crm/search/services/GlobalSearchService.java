package be.steby.CoreProject.bll.domains.crm.search.services;

import be.steby.CoreProject.pl.domains.search.models.responses.GlobalSearchResponse;

public interface GlobalSearchService {
    GlobalSearchResponse search(String query);
}

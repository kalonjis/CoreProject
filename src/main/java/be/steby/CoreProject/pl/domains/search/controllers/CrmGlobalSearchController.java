package be.steby.CoreProject.pl.domains.search.controllers;

import be.steby.CoreProject.bll.domains.search.services.GlobalSearchService;
import be.steby.CoreProject.pl.domains.search.models.responses.GlobalSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for cross-domain CRM search.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/search</pre>
 */
@RestController
@RequestMapping("/api/crm/search")
@RequiredArgsConstructor
@Tag(name = "CRM - Search", description = "Global cross-domain search across contacts, organisations, deals and leads")
public class CrmGlobalSearchController {

    private final GlobalSearchService searchService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Global CRM search",
               description = "Returns up to 5 results per category matching the query string")
    public ResponseEntity<GlobalSearchResponse> search(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(searchService.search(q));
    }
}

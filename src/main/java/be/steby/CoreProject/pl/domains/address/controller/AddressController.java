package be.steby.CoreProject.pl.domains.address.controller;

import be.steby.CoreProject.bll.domains.address.services.AddressService;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.address.models.responses.AddressSuggestionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for address-related operations.
 *
 * <p>Provides endpoints for address autocomplete/suggestion functionality,
 * enabling users to search for existing addresses while creating events
 * or other entities that require address information.</p>
 *
 * <h4>Endpoints:</h4>
 * <ul>
 *   <li>{@code GET /api/addresses/suggestions} - Autocomplete suggestions</li>
 * </ul>
 *
 * <h4>Security:</h4>
 * <p>All endpoints require authentication. Users can only access the
 * suggestion feature when logged in.</p>
 *
 * <h4>Relationship to Other Controllers:</h4>
 * <ul>
 *   <li>{@code UserAddressController} - Manages user-specific address links</li>
 *   <li>{@code AdminAddressController} - Administrative address management</li>
 *   <li>This controller - General address operations (suggestions, lookup)</li>
 * </ul>
 *
 * @see AddressService
 * @see AddressSuggestionDTO
 */
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressService addressService;

    private static final int DEFAULT_SUGGESTION_LIMIT = 10;
    private static final int MAX_SUGGESTION_LIMIT = 20;
    private static final int MIN_QUERY_LENGTH = 2;

    // ========================================
    // region Suggestions / Autocomplete
    // ========================================

    /**
     * Retrieves address suggestions for autocomplete functionality.
     *
     * <p>Searches addresses based on a specific field and returns matching
     * results suitable for populating autocomplete dropdowns.</p>
     *
     * <h4>Request:</h4>
     * <p>{@code GET /api/addresses/suggestions?field=streetName&q=rue+de+la&limit=10}</p>
     *
     * <h4>Parameters:</h4>
     * <ul>
     *   <li>{@code field} (required) - The field to search in:
     *     <ul>
     *       <li>{@code streetName} - Search by street name</li>
     *       <li>{@code city} - Search by city name</li>
     *       <li>{@code postalCode} - Search by postal code</li>
     *     </ul>
     *   </li>
     *   <li>{@code q} (required) - The search query (minimum 2 characters)</li>
     *   <li>{@code limit} (optional) - Maximum results (1-20, default 10)</li>
     * </ul>
     *
     * <h4>Response:</h4>
     * <p>Returns a list of {@link AddressSuggestionDTO} objects containing
     * complete address information for each match.</p>
     *
     * <h4>Example Response:</h4>
     * <pre>{@code
     * [
     *   {
     *     "publicId": "550e8400-e29b-41d4-a716-446655440000",
     *     "displayText": "Rue de la Loi 16, 1000 Bruxelles, BE",
     *     "streetNumber": "16",
     *     "streetName": "Rue de la Loi",
     *     "complement": null,
     *     "postalCode": "1000",
     *     "city": "Bruxelles",
     *     "countryCode": "BE"
     *   },
     *   ...
     * ]
     * }</pre>
     *
     * <h4>Frontend Usage:</h4>
     * <ol>
     *   <li>User types in a form field (e.g., street name)</li>
     *   <li>Frontend sends request with field name and typed text</li>
     *   <li>Suggestions displayed in dropdown</li>
     *   <li>On selection, all form fields populated from suggestion data</li>
     *   <li>User can edit any field before submitting</li>
     * </ol>
     *
     * @param field the field to search in (streetName, city, or postalCode)
     * @param query the search query (minimum 2 characters)
     * @param limit maximum number of results (optional, default 10, max 20)
     * @param user  the authenticated user
     * @return list of matching address suggestions
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<AddressSuggestionDTO>> getSuggestions(
            @RequestParam String field,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal User user) {

        log.debug("Address suggestion request - field: {}, query: '{}', limit: {}, user: {}",
                  field, query, limit, user.getUsername());

        // Validate query length
        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            log.debug("Query too short, returning empty list");
            return ResponseEntity.ok(List.of());
        }

        // Enforce limit bounds
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_SUGGESTION_LIMIT);

        // Search for suggestions
        List<Address> addresses = addressService.searchSuggestions(field, query, effectiveLimit);

        // Convert to DTOs
        List<AddressSuggestionDTO> suggestions = addresses.stream()
                .map(AddressSuggestionDTO::fromEntity)
                .toList();

        log.debug("Returning {} address suggestions", suggestions.size());

        return ResponseEntity.ok(suggestions);
    }

    // endregion
}
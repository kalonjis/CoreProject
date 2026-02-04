package be.steby.CoreProject.bll.domains.address.services;

import be.steby.CoreProject.bll.domains.address.events.AddressGeocodingRequestedEvent;
import be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException;

import be.steby.CoreProject.bll.domains.address.specifications.AddressSpecification;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dl.entities.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link AddressService}.
 *
 * @see AddressService
 * @see Address
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ========================================
    // region Lookup
    // ========================================

    @Override
    public Address getById(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new AddressNotFoundException("Address not found with id: " + id));
    }

    @Override
    public Address getByPublicId(String publicId) {
        return addressRepository.findByPublicId(publicId)
                .orElseThrow(() -> AddressNotFoundException.forPublicId(publicId));
    }

    @Override
    public Optional<Address> findByPublicId(String publicId) {
        return addressRepository.findByPublicId(publicId);
    }

    // endregion

    // ========================================
    // region Creation
    // ========================================

    /**
     * Creates a new address with optional duplicate detection.
     * Triggers asynchronous geocoding for newly created addresses.
     *
     * Workflow:
     * 1. Check for duplicates if enabled
     * 2. Save address to database (synchronous)
     * 3. Publish AddressGeocodingRequestedEvent for NEW addresses only
     * 4. Return saved address immediately (non-blocking)
     *
     * The geocoding process runs in background:
     * - AddressGeocodingListener picks up the event
     * - GeocodingService calls Nominatim API
     * - Address is updated with coordinates asynchronously
     *
     * @param address the address to create
     * @param detectDuplicates if true, checks for existing duplicates before creating
     * @return the saved address (new or existing duplicate)
     */
    @Override
    @Transactional
    public Address create(Address address, boolean detectDuplicates) {
        if (detectDuplicates) {
            Optional<Address> existing = findDuplicate(address);
            if (existing.isPresent()) {
                log.debug("Duplicate address found, returning existing: {}", existing.get().getPublicId());
                return existing.get();
            }
        }

        // Save new address
        Address saved = addressRepository.save(address);
        log.info("Created new address: {}", saved.getPublicId());

        // Publish event for asynchronous geocoding (only for NEW addresses)
        // This does NOT block the current thread - geocoding happens in background
        eventPublisher.publishEvent(new AddressGeocodingRequestedEvent(saved.getId()));
        log.debug("Geocoding event published for address ID: {}", saved.getId());

        return saved;
    }

    /**
     * Creates a new address with duplicate detection enabled by default.
     *
     * @param address the address to create
     * @return the saved address (new or existing duplicate)
     */
    @Override
    @Transactional
    public Address create(Address address) {
        log.debug("Creating new address with duplicate detection");
        return create(address, true);
    }


    @Override
    @Transactional
    public Address createForced(Address address) {
        return create(address, false);
    }

    // endregion

    // ========================================
    // region Update
    // ========================================

    @Override
    @Transactional
    public Address update(String publicId, Address updated) {
        Address existing = getByPublicId(publicId);

        existing.setStreetNumber(updated.getStreetNumber());
        existing.setStreetName(updated.getStreetName());
        existing.setComplement(updated.getComplement());
        existing.setPostalCode(updated.getPostalCode());
        existing.setCity(updated.getCity());
        existing.setStateProvince(updated.getStateProvince());
        existing.setCountryCode(updated.getCountryCode());

        // Reset validation status on address change
        existing.setValidated(false);
        existing.setValidationSource(null);
        existing.setFormattedAddress(null);

        // Preserve coordinates only if address didn't change significantly
        if (hasLocationChanged(existing, updated)) {
            existing.setLatitude(null);
            existing.setLongitude(null);
        }

        log.info("Updated address: {}", publicId);
        return addressRepository.save(existing);
    }

    // endregion

    // ========================================
    // region Duplicate Detection
    // ========================================

    @Override
    public Optional<Address> findDuplicate(Address address) {
        List<Address> duplicates = addressRepository.findDuplicates(
                address.getStreetNumber(),
                address.getStreetName(),
                address.getPostalCode(),
                address.getCity(),
                address.getCountryCode()
        );

        return duplicates.stream().findFirst();
    }

    @Override
    public boolean hasDuplicate(Address address) {
        return addressRepository.existsDuplicate(
                address.getStreetNumber(),
                address.getStreetName(),
                address.getPostalCode(),
                address.getCity(),
                address.getCountryCode()
        );
    }

    @Override
    @Transactional
    public Address findOrCreate(Address address) {
        return findDuplicate(address).orElseGet(() -> {
            Address saved = addressRepository.save(address);
            log.info("Created new address (findOrCreate): {}", saved.getPublicId());

            return create(address, false);
        });
    }

    // endregion

    // ========================================
    // region Geographical Queries
    // ========================================

    @Override
    public List<Address> findByCity(String city, String countryCode) {
        return addressRepository.findByCity(city, countryCode);
    }

    @Override
    public List<Address> findByPostalCode(String postalCode, String countryCode) {
        return addressRepository.findByPostalCodeAndCountryCodeIgnoreCase(postalCode, countryCode);
    }

    @Override
    public List<Address> findByCountry(String countryCode) {
        return addressRepository.findByCountryCodeIgnoreCase(countryCode);
    }

    // endregion


    // ========================================
    // region Suggestions/Autocomplete
    // ========================================


    /**
     * Supported fields for autocomplete search.
     */
    private static final String FIELD_STREET_NAME = "streetName";
    private static final String FIELD_CITY = "city";
    private static final String FIELD_POSTAL_CODE = "postalCode";

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_LIMIT = 20;
    private static final int DEFAULT_LIMIT = 10;

    /**
     * {@inheritDoc}
     *
     * <p>Implementation uses JPA Specifications for dynamic query building.
     * The search uses prefix matching (LIKE 'query%') which can leverage
     * database indexes efficiently.</p>
     */
    @Override
    public List<Address> searchSuggestions(String field, String query, int limit) {
        // Validate inputs
        validateSuggestionParams(field, query, limit);

        // Sanitize query
        String sanitizedQuery = sanitizeQuery(query);
        if (sanitizedQuery.length() < MIN_QUERY_LENGTH) {
            log.debug("Query too short for suggestions: '{}' (min {} chars)",
                    sanitizedQuery, MIN_QUERY_LENGTH);
            return List.of();
        }

        // Build specification based on field
        Specification<Address> spec = buildSuggestionSpecification(field, sanitizedQuery);

        // Determine sort order based on field
        Sort sort = buildSuggestionSort(field);

        // Execute query with pagination
        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        PageRequest pageRequest = PageRequest.of(0, effectiveLimit, sort);

        List<Address> results = addressRepository.findAll(spec, pageRequest).getContent();

        log.debug("Suggestion search for field '{}' with query '{}' returned {} results",
                field, sanitizedQuery, results.size());

        return results;
    }

    /**
     * Validates the suggestion search parameters.
     *
     * @param field the field to search in
     * @param query the search query
     * @param limit the maximum results
     * @throws IllegalArgumentException if parameters are invalid
     */
    private void validateSuggestionParams(String field, String query, int limit) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("Search field cannot be null or blank");
        }

        if (!isValidSuggestionField(field)) {
            throw new IllegalArgumentException(
                    "Invalid search field: '" + field + "'. " +
                            "Supported fields: " + FIELD_STREET_NAME + ", " + FIELD_CITY + ", " + FIELD_POSTAL_CODE
            );
        }

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be null or blank");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be at least 1");
        }
    }

    /**
     * Checks if the given field is valid for suggestion search.
     *
     * @param field the field name to validate
     * @return true if the field is supported
     */
    private boolean isValidSuggestionField(String field) {
        return FIELD_STREET_NAME.equals(field)
                || FIELD_CITY.equals(field)
                || FIELD_POSTAL_CODE.equals(field);
    }

    /**
     * Sanitizes the search query by trimming whitespace and converting to lowercase.
     *
     * @param query the raw query string
     * @return sanitized query string
     */
    private String sanitizeQuery(String query) {
        return query.trim().toLowerCase();
    }

    /**
     * Builds the JPA Specification for the suggestion search based on the field.
     *
     * @param field the field to search in
     * @param query the sanitized search query
     * @return the specification for the search
     */
    private Specification<Address> buildSuggestionSpecification(String field, String query) {
        return switch (field) {
            case FIELD_STREET_NAME -> AddressSpecification.streetNameStartsWith(query);
            case FIELD_CITY -> AddressSpecification.cityStartsWith(query);
            case FIELD_POSTAL_CODE -> AddressSpecification.postalCodeStartsWith(query);
            default -> throw new IllegalArgumentException("Unsupported field: " + field);
        };
    }

    /**
     * Builds the sort order for suggestion results based on the searched field.
     *
     * <p>Results are sorted alphabetically by the searched field first,
     * then by secondary fields for consistent ordering.</p>
     *
     * @param field the field being searched
     * @return the sort order
     */
    private Sort buildSuggestionSort(String field) {
        return switch (field) {
            case FIELD_STREET_NAME -> Sort.by(
                    Sort.Order.asc("streetName"),
                    Sort.Order.asc("streetNumber"),
                    Sort.Order.asc("city")
            );
            case FIELD_CITY -> Sort.by(
                    Sort.Order.asc("city"),
                    Sort.Order.asc("postalCode"),
                    Sort.Order.asc("streetName")
            );
            case FIELD_POSTAL_CODE -> Sort.by(
                    Sort.Order.asc("postalCode"),
                    Sort.Order.asc("city"),
                    Sort.Order.asc("streetName")
            );
            default -> Sort.by(Sort.Order.asc("streetName"));
        };
    }

    // endregion


    // ========================================
    // region Validation
    // ========================================

    @Override
    @Transactional
    public Address markAsValidated(Address address, String validationSource) {
        address.setValidated(true);
        address.setValidationSource(validationSource);
        log.info("Marked address {} as validated by {}", address.getId(), validationSource);
        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address setCoordinates(Address address, Double latitude, Double longitude) {
        address.setLatitude(latitude);
        address.setLongitude(longitude);
        log.debug("Set coordinates for address {}: ({}, {})", address.getId(), latitude, longitude);
        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address setFormattedAddress(Address address, String formattedAddress) {
        address.setFormattedAddress(formattedAddress);
        return addressRepository.save(address);
    }

    @Override
    public List<Address> getUnvalidatedAddresses() {
        return addressRepository.findByValidatedFalse();
    }

    @Override
    public List<Address> getAddressesWithoutCoordinates() {
        return addressRepository.findWithoutCoordinates();
    }

    // endregion

    // ========================================
    // region Cleanup
    // ========================================

    @Override
    public List<Address> findOrphanedAddresses() {
        return addressRepository.findOrphanedAddresses();
    }

    @Override
    public long countOrphanedAddresses() {
        return addressRepository.countOrphanedAddresses();
    }

    @Override
    @Transactional
    public int deleteOrphanedAddresses() {
        List<Address> orphaned = findOrphanedAddresses();
        int count = orphaned.size();

        if (count > 0) {
            addressRepository.deleteAll(orphaned);
            log.info("Deleted {} orphaned addresses", count);
        }

        return count;
    }

    @Override
    @Transactional
    public void delete(String publicId) {
        Address address = getByPublicId(publicId);
        addressRepository.delete(address);
        log.info("Deleted address: {}", publicId);
    }

    // endregion

    // ========================================
    // region Helpers
    // ========================================

    private boolean hasLocationChanged(Address existing, Address updated) {
        return !equalsIgnoreCase(existing.getStreetName(), updated.getStreetName())
                || !equalsIgnoreCase(existing.getStreetNumber(), updated.getStreetNumber())
                || !equalsIgnoreCase(existing.getPostalCode(), updated.getPostalCode())
                || !equalsIgnoreCase(existing.getCity(), updated.getCity())
                || !equalsIgnoreCase(existing.getCountryCode(), updated.getCountryCode());
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    // endregion
}
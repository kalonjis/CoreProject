package be.steby.CoreProject.bll.domains.address.services;

import be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException;

import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dl.entities.Address;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        Address saved = addressRepository.save(address);
        log.info("Created new address: {}", saved.getPublicId());
        return saved;
    }

    @Override
    @Transactional
    public Address create(Address address) {
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
            return saved;
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
    // region Validation
    // ========================================

    @Override
    @Transactional
    public Address markAsValidated(String publicId, String validationSource) {
        Address address = getByPublicId(publicId);
        address.setValidated(true);
        address.setValidationSource(validationSource);
        log.info("Marked address {} as validated by {}", publicId, validationSource);
        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address setCoordinates(String publicId, Double latitude, Double longitude) {
        Address address = getByPublicId(publicId);
        address.setLatitude(latitude);
        address.setLongitude(longitude);
        log.debug("Set coordinates for address {}: ({}, {})", publicId, latitude, longitude);
        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address setFormattedAddress(String publicId, String formattedAddress) {
        Address address = getByPublicId(publicId);
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
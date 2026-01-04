package be.steby.CoreProject.bll.domains.profile.services.address;

import be.steby.CoreProject.bll.domains.address.exceptions.AddressAlreadyLinkedException;
import be.steby.CoreProject.bll.domains.address.exceptions.AddressModificationNotAllowedException;
import be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException;
import be.steby.CoreProject.bll.domains.address.exceptions.AddressValidationException;
import be.steby.CoreProject.bll.domains.address.services.AddressService;
import be.steby.CoreProject.bll.specifications.UserAddressSpecification;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dal.repositories.UserAddressRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressModificationStrategy;
import be.steby.CoreProject.dl.enums.AddressType;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UpdateUserAddressRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UserAddressSearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implementation of {@link UserAddressService}.
 *
 * @see UserAddressService
 * @see UserAddress
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final AddressRepository addressRepository;
    private final AddressService addressService;

    @Value("${app.address.default-modification-strategy:COPY_ON_WRITE}")
    private AddressModificationStrategy defaultStrategy;

    // ========================================
    // region Search
    // ========================================

    @Override
    public List<UserAddress> search(User user, UserAddressSearchCriteria criteria) {
        Specification<UserAddress> spec = UserAddressSpecification.belongsToUser(user);

        if (criteria == null || !criteria.hasFilters()) {
            // Default: active and currently valid
            spec = spec.and(UserAddressSpecification.isActive())
                    .and(UserAddressSpecification.isCurrentlyValid());
            return userAddressRepository.findAll(spec);
        }

        // Apply type filter
        if (criteria.type() != null) {
            spec = spec.and(UserAddressSpecification.hasType(criteria.type()));
        }

        // Apply boolean filters using helper method
        spec = applyBooleanFilter(spec, criteria.active(),
                UserAddressSpecification::isActive,
                UserAddressSpecification::isInactive);

        spec = applyBooleanFilter(spec, criteria.isDefault(),
                UserAddressSpecification::isDefault,
                UserAddressSpecification::isNotDefault);

        spec = applyBooleanFilter(spec, criteria.isPrimary(),
                UserAddressSpecification::isPrimary,
                UserAddressSpecification::isNotPrimary);

        spec = applyBooleanFilter(spec, criteria.billingEligible(),
                UserAddressSpecification::isBillingEligible,
                UserAddressSpecification::isNotBillingEligible);

        spec = applyBooleanFilter(spec, criteria.shippingEligible(),
                UserAddressSpecification::isShippingEligible,
                UserAddressSpecification::isNotShippingEligible);

        spec = applyBooleanFilter(spec, criteria.verified(),
                UserAddressSpecification::isVerifiedByOwner,
                UserAddressSpecification::isNotVerifiedByOwner);

        // Apply string filters
        if (criteria.countryCode() != null && !criteria.countryCode().isBlank()) {
            spec = spec.and(UserAddressSpecification.hasCountry(criteria.countryCode()));
        }

        if (criteria.city() != null && !criteria.city().isBlank()) {
            spec = spec.and(UserAddressSpecification.hasCity(criteria.city()));
        }

        if (criteria.postalCode() != null && !criteria.postalCode().isBlank()) {
            spec = spec.and(UserAddressSpecification.hasPostalCode(criteria.postalCode()));
        }

        if (criteria.label() != null && !criteria.label().isBlank()) {
            spec = spec.and(UserAddressSpecification.labelContains(criteria.label()));
        }

        // Apply validity filter (non-boolean, specific logic)
        if (criteria.validOnly() != null && criteria.validOnly()) {
            spec = spec.and(UserAddressSpecification.isCurrentlyValid());
        }

        // Apply coordinates filter (non-boolean, specific logic)
        if (criteria.hasCoordinates() != null && criteria.hasCoordinates()) {
            spec = spec.and(UserAddressSpecification.hasCoordinates());
        }

        return userAddressRepository.findAll(spec);
    }

    /**
     * Helper method to apply boolean filters consistently.
     *
     * <p>Handles three states:</p>
     * <ul>
     *   <li>null → no filter applied</li>
     *   <li>true → applies positive specification</li>
     *   <li>false → applies negative specification</li>
     * </ul>
     *
     * @param spec current specification
     * @param criterion the boolean criterion (null, true, or false)
     * @param positiveSpec supplier for the positive specification
     * @param negativeSpec supplier for the negative specification
     * @return updated specification
     */
    private Specification<UserAddress> applyBooleanFilter(
            Specification<UserAddress> spec,
            Boolean criterion,
            Supplier<Specification<UserAddress>> positiveSpec,
            Supplier<Specification<UserAddress>> negativeSpec) {

        if (criterion == null) {
            return spec;
        }

        return criterion
                ? spec.and(positiveSpec.get())
                : spec.and(negativeSpec.get());
    }

    // endregion

    // ========================================
    // region Lookup
    // ========================================

    @Override
    public UserAddress getByPublicId(String publicId) {
        return userAddressRepository.findByPublicId(publicId)
                .orElseThrow(() -> AddressNotFoundException.forUserAddressPublicId(publicId));
    }

    @Override
    public UserAddress getByPublicIdAndUser(String publicId, User user) {
        return userAddressRepository.findByPublicIdAndUser(publicId, user)
                .orElseThrow(() -> AddressNotFoundException.forUserAddressNotOwned(publicId, user.getUsername()));
    }

    @Override
    public List<UserAddress> getAllForUser(User user) {
        return userAddressRepository.findByUser(user);
    }

    @Override
    public List<UserAddress> getActiveForUser(User user) {
        return userAddressRepository.findActiveByUser(user);
    }

    @Override
    public List<UserAddress> getByType(User user, AddressType addressType) {
        return userAddressRepository.findActiveByUserAndType(user, addressType);
    }

    // endregion

    // ========================================
    // region Default & Primary Address
    // ========================================

    @Override
    public Optional<UserAddress> getPrimaryAddress(User user) {
        return userAddressRepository.findByUserAndIsPrimaryTrue(user);
    }

    @Override
    public Optional<UserAddress> getDefaultForType(User user, AddressType addressType) {
        return userAddressRepository.findByUserAndAddressTypeAndIsDefaultTrue(user, addressType);
    }

    @Override
    public Optional<UserAddress> getDefaultBillingAddress(User user) {
        return userAddressRepository.findDefaultBillingAddress(user);
    }

    @Override
    public Optional<UserAddress> getDefaultShippingAddress(User user) {
        return userAddressRepository.findDefaultShippingAddress(user);
    }

    @Override
    @Transactional
    public UserAddress setAsDefault(String publicId, User user) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);

        userAddressRepository.clearDefaultForType(user, userAddress.getAddressType());

        userAddress.setDefault(true);
        log.info("Set address {} as default {} for user {}",
                publicId, userAddress.getAddressType(), user.getUsername());

        return userAddressRepository.save(userAddress);
    }

    @Override
    @Transactional
    public UserAddress setAsPrimary(String publicId, User user) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);

        userAddressRepository.clearPrimaryForUser(user);

        userAddress.setPrimary(true);
        log.info("Set address {} as primary for user {}", publicId, user.getUsername());

        return userAddressRepository.save(userAddress);
    }

    // endregion

    // ========================================
    // region Link Management
    // ========================================

    @Override
    @Transactional
    public UserAddress linkAddress(User user, Address address, AddressType addressType,
                                   String label, boolean isDefault, boolean isPrimary) {

        if (userAddressRepository.existsByUserAndAddressAndAddressType(user, address, addressType)) {
            throw AddressAlreadyLinkedException.forUserAndType(
                    user.getUsername(), address.getPublicId(), addressType);
        }

        if (isDefault) {
            userAddressRepository.clearDefaultForType(user, addressType);
        }
        if (isPrimary) {
            userAddressRepository.clearPrimaryForUser(user);
        }

        UserAddress userAddress = new UserAddress(user, address, addressType, label, isDefault, isPrimary);
        UserAddress saved = userAddressRepository.save(userAddress);

        log.info("Linked address {} to user {} as {}", address.getPublicId(), user.getUsername(), addressType);
        return saved;
    }

    @Override
    @Transactional
    public UserAddress linkAddress(User user, Address address, AddressType addressType) {
        return linkAddress(user, address, addressType, null, false, false);
    }

    @Override
    @Transactional
    public UserAddress createAndLink(User user, Address address, AddressType addressType,
                                     String label, boolean isDefault, boolean isPrimary) {
        return createAndLink(user, address, addressType, label, isDefault, isPrimary, true, true);
    }

    @Override
    @Transactional
    public UserAddress createAndLink(User user, Address address, AddressType addressType,
                                     String label, boolean isDefault, boolean isPrimary,
                                     boolean billingEligible, boolean shippingEligible) {

        Address savedAddress = addressService.findOrCreate(address);

        UserAddress userAddress = linkAddress(user, savedAddress, addressType, label, isDefault, isPrimary);

        // Apply eligibility settings
        if (!billingEligible || !shippingEligible) {
            userAddress.setBillingEligible(billingEligible);
            userAddress.setShippingEligible(shippingEligible);
            userAddress = userAddressRepository.save(userAddress);
        }

        return userAddress;
    }

    @Override
    @Transactional
    public UserAddress createAndLink(User user, Address address, AddressType addressType) {
        return createAndLink(user, address, addressType, null, false, false);
    }

    @Override
    @Transactional
    public UserAddress unlinkAddress(String publicId, User user) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);
        userAddress.deactivate();
        log.info("Deactivated address link {} for user {}", publicId, user.getUsername());
        return userAddressRepository.save(userAddress);
    }

    @Override
    @Transactional
    public void removeAddressLink(String publicId, User user) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);
        userAddressRepository.delete(userAddress);
        log.info("Permanently removed address link {} for user {}", publicId, user.getUsername());
    }

    // endregion

    // ========================================
    // region Address Update
    // ========================================

    @Override
    @Transactional
    public UserAddress updateAddress(String publicId, User user, Address updatedAddress) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);

        AddressModificationStrategy strategy = userAddress.getModificationStrategy() != null
                ? userAddress.getModificationStrategy()
                : defaultStrategy;

        return switch (strategy) {
            case SHARED -> updateShared(userAddress, updatedAddress);
            case COPY_ON_WRITE -> updateCopyOnWrite(userAddress, user, updatedAddress);
            case OWNER_ONLY -> updateOwnerOnly(userAddress, user, updatedAddress);
            case IMMUTABLE -> throw AddressModificationNotAllowedException.immutableAddress(publicId);
            case VERSIONED -> updateVersioned(userAddress, user, updatedAddress);
        };
    }

    private UserAddress updateShared(UserAddress userAddress, Address updatedAddress) {
        Address address = userAddress.getAddress();

        address.setStreetNumber(updatedAddress.getStreetNumber());
        address.setStreetName(updatedAddress.getStreetName());
        address.setComplement(updatedAddress.getComplement());
        address.setPostalCode(updatedAddress.getPostalCode());
        address.setCity(updatedAddress.getCity());
        address.setStateProvince(updatedAddress.getStateProvince());
        address.setCountryCode(updatedAddress.getCountryCode());
        address.setValidated(false);

        addressRepository.save(address);
        log.info("Updated shared address: {}", address.getPublicId());

        return userAddress;
    }

    private UserAddress updateCopyOnWrite(UserAddress userAddress, User user, Address updatedAddress) {
        Address currentAddress = userAddress.getAddress();

        if (userAddressRepository.isAddressShared(currentAddress)) {
            Address newAddress = Address.builder()
                    .streetNumber(updatedAddress.getStreetNumber())
                    .streetName(updatedAddress.getStreetName())
                    .complement(updatedAddress.getComplement())
                    .postalCode(updatedAddress.getPostalCode())
                    .city(updatedAddress.getCity())
                    .stateProvince(updatedAddress.getStateProvince())
                    .countryCode(updatedAddress.getCountryCode())
                    .validated(false)
                    .build();

            Address savedAddress = addressRepository.save(newAddress);
            userAddress.setAddress(savedAddress);

            log.info("Copy-on-write: Created new address {} for user {} (was shared: {})",
                    savedAddress.getPublicId(), user.getUsername(), currentAddress.getPublicId());

            return userAddressRepository.save(userAddress);
        } else {
            return updateShared(userAddress, updatedAddress);
        }
    }

    private UserAddress updateOwnerOnly(UserAddress userAddress, User user, Address updatedAddress) {
        Address address = userAddress.getAddress();

        if (!address.isCreatedBy(user.getUsername())) {
            throw AddressModificationNotAllowedException.ownerOnly(
                    address.getPublicId(), user.getUsername());
        }

        return updateShared(userAddress, updatedAddress);
    }

    private UserAddress updateVersioned(UserAddress userAddress, User user, Address updatedAddress) {
        // Create new address version
        Address newAddress = Address.builder()
                .streetNumber(updatedAddress.getStreetNumber())
                .streetName(updatedAddress.getStreetName())
                .complement(updatedAddress.getComplement())
                .postalCode(updatedAddress.getPostalCode())
                .city(updatedAddress.getCity())
                .stateProvince(updatedAddress.getStateProvince())
                .countryCode(updatedAddress.getCountryCode())
                .validated(false)
                .build();

        Address savedAddress = addressRepository.save(newAddress);

        // Deactivate old link
        userAddress.deactivate();
        userAddressRepository.save(userAddress);

        // Create new link with new address version
        UserAddress newLink = new UserAddress(
                user,
                savedAddress,
                userAddress.getAddressType(),
                userAddress.getLabel(),
                userAddress.isDefault(),
                userAddress.isPrimary()
        );
        newLink.setBillingEligible(userAddress.isBillingEligible());
        newLink.setShippingEligible(userAddress.isShippingEligible());

        log.info("Versioned update: Created new address {} replacing {} for user {}",
                savedAddress.getPublicId(), userAddress.getAddress().getPublicId(), user.getUsername());

        return userAddressRepository.save(newLink);
    }

    @Override
    @Transactional
    public UserAddress updateLinkMetadata(String publicId, User user, String label, String notes) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);

        if (label != null) {
            userAddress.setLabel(label);
        }
        if (notes != null) {
            userAddress.setNotes(notes);
        }

        return userAddressRepository.save(userAddress);
    }

    @Override
    @Transactional
    public UserAddress updateUserAddress(String publicId, User user, UpdateUserAddressRequest request) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);

        // Update address geographical data if provided
        if (request.hasAddressChanges()) {
            Address updatedAddress = buildUpdatedAddress(userAddress.getAddress(), request);
            userAddress = updateAddress(publicId, user, updatedAddress);
        }

        // Update link metadata with smart empty string handling
        if (request.label() != null) {
            // Empty string = remove label, otherwise update
            userAddress.setLabel(request.label().isBlank() ? null : request.label());
        }

        if (request.notes() != null) {
            // Empty string = remove notes, otherwise update
            userAddress.setNotes(request.notes().isBlank() ? null : request.notes());
        }

        // Update eligibility if provided
        if (request.billingEligible() != null) {
            userAddress.setBillingEligible(request.billingEligible());
        }

        if (request.shippingEligible() != null) {
            userAddress.setShippingEligible(request.shippingEligible());
        }

        return userAddressRepository.save(userAddress);
    }

    /**
     * Builds an updated Address entity with smart validation for required vs optional fields.
     *
     * <p><b>Field handling rules:</b></p>
     * <ul>
     *   <li><b>Required fields</b> (streetName, postalCode, city, countryCode): Cannot be empty, throws exception if blank</li>
     *   <li><b>Optional fields</b> (streetNumber, complement, stateProvince): Empty string sets to null (removal)</li>
     *   <li><b>Null fields</b>: Preserve existing value (no change)</li>
     * </ul>
     *
     * @param existing the current address
     * @param request  the update request
     * @return the address with updated fields
     * @throws AddressValidationException if a required field is set to empty string
     */
    private Address buildUpdatedAddress(Address existing, UpdateUserAddressRequest request) {
        // Validate and update required fields
        String streetName = existing.getStreetName();
        if (request.streetName() != null) {
            if (request.streetName().isBlank()) {
                throw AddressValidationException.missingRequiredField("streetName");
            }
            streetName = request.streetName();
        }

        String postalCode = existing.getPostalCode();
        if (request.postalCode() != null) {
            if (request.postalCode().isBlank()) {
                throw AddressValidationException.missingRequiredField("postalCode");
            }
            postalCode = request.postalCode();
        }

        String city = existing.getCity();
        if (request.city() != null) {
            if (request.city().isBlank()) {
                throw AddressValidationException.missingRequiredField("city");
            }
            city = request.city();
        }

        String countryCode = existing.getCountryCode();
        if (request.countryCode() != null) {
            if (request.countryCode().isBlank()) {
                throw AddressValidationException.missingRequiredField("countryCode");
            }
            countryCode = request.countryCode().toUpperCase();
        }

        // Handle optional fields - empty string = null (removal)
        String streetNumber = existing.getStreetNumber();
        if (request.streetNumber() != null) {
            streetNumber = request.streetNumber().isBlank() ? null : request.streetNumber();
        }

        String complement = existing.getComplement();
        if (request.complement() != null) {
            complement = request.complement().isBlank() ? null : request.complement();
        }

        String stateProvince = existing.getStateProvince();
        if (request.stateProvince() != null) {
            stateProvince = request.stateProvince().isBlank() ? null : request.stateProvince();
        }

        return Address.builder()
                .streetNumber(streetNumber)
                .streetName(streetName)
                .complement(complement)
                .postalCode(postalCode)
                .city(city)
                .stateProvince(stateProvince)
                .countryCode(countryCode)
                .validated(false)  // Mark as not validated after modification
                .build();
    }

    // endregion

    // ========================================
    // region Eligibility Management
    // ========================================

    @Override
    @Transactional
    public UserAddress updateEligibility(String publicId, User user,
                                         boolean billingEligible, boolean shippingEligible) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);
        userAddress.setBillingEligible(billingEligible);
        userAddress.setShippingEligible(shippingEligible);
        return userAddressRepository.save(userAddress);
    }

    @Override
    @Transactional
    public UserAddress changeType(String publicId, User user,
                                  AddressType newType, boolean setAsDefault) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);

        if (userAddress.isDefault()) {
            userAddress.setDefault(false);
        }

        userAddress.setAddressType(newType);

        if (setAsDefault) {
            userAddressRepository.clearDefaultForType(user, newType);
            userAddress.setDefault(true);
        }

        return userAddressRepository.save(userAddress);
    }

    // endregion

    // ========================================
    // region Sharing Info
    // ========================================

    @Override
    public List<UserAddress> getUsersSharingAddress(String addressPublicId) {
        Address address = addressService.getByPublicId(addressPublicId);
        return userAddressRepository.findByAddress(address);
    }

    @Override
    public boolean isAddressShared(String addressPublicId) {
        Address address = addressService.getByPublicId(addressPublicId);
        return userAddressRepository.isAddressShared(address);
    }

    @Override
    public long countUsersForAddress(String addressPublicId) {
        Address address = addressService.getByPublicId(addressPublicId);
        return userAddressRepository.countByAddress(address);
    }

    // endregion

    // ========================================
    // region Eligibility Queries
    // ========================================

    @Override
    public List<UserAddress> getBillingEligibleAddresses(User user) {
        return userAddressRepository.findBillingEligible(user);
    }

    @Override
    public List<UserAddress> getShippingEligibleAddresses(User user) {
        return userAddressRepository.findShippingEligible(user);
    }

    // endregion
}
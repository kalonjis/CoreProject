package be.steby.CoreProject.bll.domains.profile.services.address;

import be.steby.CoreProject.bll.domains.address.exceptions.AddressAlreadyLinkedException;
import be.steby.CoreProject.bll.domains.address.exceptions.AddressModificationNotAllowedException;
import be.steby.CoreProject.bll.domains.address.exceptions.AddressNotFoundException;
import be.steby.CoreProject.bll.domains.address.services.AddressService;

import be.steby.CoreProject.bll.specifications.UserAddressSpecification;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dal.repositories.UserAddressRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.dl.enums.AddressModificationStrategy;
import be.steby.CoreProject.dl.enums.AddressType;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UserAddressSearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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

        // Apply filters
        if (criteria.type() != null) {
            spec = spec.and(UserAddressSpecification.hasType(criteria.type()));
        }

        if (criteria.active() != null) {
            spec = criteria.active()
                    ? spec.and(UserAddressSpecification.isActive())
                    : spec.and(UserAddressSpecification.isInactive());
        }

        if (criteria.isDefault() != null && criteria.isDefault()) {
            spec = spec.and(UserAddressSpecification.isDefault());
        }

        if (criteria.isPrimary() != null && criteria.isPrimary()) {
            spec = spec.and(UserAddressSpecification.isPrimary());
        }

        if (criteria.billingEligible() != null && criteria.billingEligible()) {
            spec = spec.and(UserAddressSpecification.isBillingEligible());
        }

        if (criteria.shippingEligible() != null && criteria.shippingEligible()) {
            spec = spec.and(UserAddressSpecification.isShippingEligible());
        }

        if (criteria.verified() != null && criteria.verified()) {
            spec = spec.and(UserAddressSpecification.isVerifiedByOwner());
        }

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

        if (criteria.validOnly() != null && criteria.validOnly()) {
            spec = spec.and(UserAddressSpecification.isCurrentlyValid());
        }

        if (criteria.hasCoordinates() != null && criteria.hasCoordinates()) {
            spec = spec.and(UserAddressSpecification.hasCoordinates());
        }

        return userAddressRepository.findAll(spec);
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

        Address savedAddress = addressService.findOrCreate(address);
        return linkAddress(user, savedAddress, addressType, label, isDefault, isPrimary);
    }

    @Override
    @Transactional
    public UserAddress createAndLink(User user, Address address, AddressType addressType) {
        return createAndLink(user, address, addressType, null, false, false);
    }

    @Override
    @Transactional
    public void unlinkAddress(String publicId, User user) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);
        userAddress.deactivate();
        userAddressRepository.save(userAddress);

        log.info("Unlinked address {} from user {}", publicId, user.getUsername());
    }

    @Override
    @Transactional
    public void removeAddressLink(String publicId, User user) {
        UserAddress userAddress = getByPublicIdAndUser(publicId, user);
        userAddressRepository.delete(userAddress);

        log.info("Removed address link {} for user {}", publicId, user.getUsername());
    }

    // endregion

    // ========================================
    // region Address Modification (with Strategy)
    // ========================================

    @Override
    @Transactional
    public UserAddress updateAddress(String userAddressPublicId, User user, Address updatedAddress) {
        UserAddress userAddress = getByPublicIdAndUser(userAddressPublicId, user);

        AddressModificationStrategy strategy = userAddress.getEffectiveStrategy(defaultStrategy);

        return switch (strategy) {
            case SHARED -> updateShared(userAddress, updatedAddress);
            case COPY_ON_WRITE -> updateCopyOnWrite(userAddress, user, updatedAddress);
            case OWNER_ONLY -> updateOwnerOnly(userAddress, user, updatedAddress);
            case IMMUTABLE -> throw AddressModificationNotAllowedException.immutableAddress(
                    userAddress.getAddress().getPublicId());
            case VERSIONED -> updateVersioned(userAddress, updatedAddress);
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
                    address.getPublicId(), address.getCreatedBy());
        }

        return updateShared(userAddress, updatedAddress);
    }

    private UserAddress updateVersioned(UserAddress userAddress, Address updatedAddress) {
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

        userAddress.deactivate();
        userAddressRepository.save(userAddress);

        UserAddress newLink = new UserAddress(
                userAddress.getUser(),
                savedAddress,
                userAddress.getAddressType(),
                userAddress.getLabel(),
                userAddress.isDefault(),
                userAddress.isPrimary()
        );

        log.info("Versioned update: Created new address {} replacing {}",
                savedAddress.getPublicId(), userAddress.getAddress().getPublicId());

        return userAddressRepository.save(newLink);
    }

    // endregion

    // ========================================
    // region Link Metadata Updates
    // ========================================

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
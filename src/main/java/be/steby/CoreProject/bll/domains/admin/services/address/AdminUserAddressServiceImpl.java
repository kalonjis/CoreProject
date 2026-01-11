package be.steby.CoreProject.bll.domains.admin.services.address;

import be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator;
import be.steby.CoreProject.bll.domains.profile.services.address.UserAddressService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.UserAddressRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import be.steby.CoreProject.pl.domains.admin.models.requests.UpdateUserAddressMetadataRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.CreateUserAddressRequest;
import be.steby.CoreProject.pl.domains.profile.address.models.requests.UserAddressSearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link AdminUserAddressService}.
 *
 * <p>Orchestrates administrative user address operations by:</p>
 * <ol>
 *   <li>Validating admin permissions</li>
 *   <li>Delegating to specialized services</li>
 *   <li>Logging administrative actions</li>
 * </ol>
 *
 * <h4>Transaction management:</h4>
 * <p>Read operations use {@code @Transactional(readOnly = true)} for optimization.</p>
 *
 * @see AdminUserAddressService
 * @see UserAddressService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserAddressServiceImpl implements AdminUserAddressService {

    private final UserService userService;
    private final UserAddressService userAddressService;
    private final UserAddressRepository userAddressRepository;
    private final AdminPermissionValidator adminPermissionValidator;


    @Override
    public List<UserAddress> getUserAddresses(
            String userPublicId,
            UserAddressSearchCriteria criteria) {

        log.debug("Admin retrieving addresses for user: {} with criteria: {}",
                userPublicId, criteria != null ? "filtered" : "all");

        // 1. Get the target user
        User target = userService.getUserByPublicId(userPublicId);

        // 2. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 3. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        // 4. Delegate to user address service with search criteria
        List<UserAddress> addresses = userAddressService.search(target, criteria);

        log.info("Admin {} viewed {} address(es) of user {} (filtered: {})",
                admin.getUsername(), addresses.size(), target.getUsername(),
                criteria != null && criteria.hasFilters());

        return addresses;
    }

    @Override
    @Transactional
    public UserAddress updateAddressMetadata(
            String userPublicId,
            String linkPublicId,
            UpdateUserAddressMetadataRequest request) {

        log.debug("Admin updating address metadata - user: {}, link: {}", userPublicId, linkPublicId);

        // 1. Validate request has at least one field to update
        if (!request.hasAnyUpdate()) {
            throw new IllegalArgumentException("No fields provided for update");
        }

        // 2. Get the target user
        User target = userService.getUserByPublicId(userPublicId);

        // 3. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 4. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        // 5. Get the user address link (verifies ownership)
        UserAddress userAddress = userAddressService.getByPublicIdAndUser(linkPublicId, target);

        log.debug("Admin {} updating address {} for user {}",
                admin.getUsername(), linkPublicId, target.getUsername());

        // 6. Apply updates in specific order to handle dependencies

        // 6.1. Change type first (affects isDefault behavior)
        if (request.addressType() != null) {
            userAddress = userAddressService.changeType(
                    linkPublicId,
                    target,
                    request.addressType(),
                    request.shouldSetDefaultOnTypeChange()
            );
            log.debug("Changed address type to {}", request.addressType());
        }

        // 6.2. Set as primary (clears other primary flags)
        if (request.isPrimary() != null && request.isPrimary()) {
            userAddress = userAddressService.setAsPrimary(linkPublicId, target);
            log.debug("Set as primary address");
        } else if (request.isPrimary() != null && !request.isPrimary()) {
            // Unset primary if explicitly set to false
            userAddress.setPrimary(false);
            userAddress = userAddressRepository.save(userAddress);
            log.debug("Removed primary flag");
        }

        // 6.3. Set as default (clears other default flags for same type)
        if (request.isDefault() != null && request.isDefault()) {
            userAddress = userAddressService.setAsDefault(linkPublicId, target);
            log.debug("Set as default for type {}", userAddress.getAddressType());
        } else if (request.isDefault() != null && !request.isDefault()) {
            // Unset default if explicitly set to false
            userAddress.setDefault(false);
            userAddress = userAddressRepository.save(userAddress);
            log.debug("Removed default flag");
        }

        // 6.4. Update eligibility flags
        if (request.hasEligibilityUpdate()) {
            boolean billingEligible = request.billingEligible() != null
                    ? request.billingEligible()
                    : userAddress.isBillingEligible();
            boolean shippingEligible = request.shippingEligible() != null
                    ? request.shippingEligible()
                    : userAddress.isShippingEligible();

            userAddress = userAddressService.updateEligibility(
                    linkPublicId,
                    target,
                    billingEligible,
                    shippingEligible
            );
            log.debug("Updated eligibility - billing: {}, shipping: {}",
                    billingEligible, shippingEligible);
        }

        // 6.5. Update label and notes
        if (request.hasMetadataUpdate()) {
            String label = request.label();
            String notes = request.notes();

            // Handle empty string = clear field
            if (label != null && label.isBlank()) {
                label = null;
            }
            if (notes != null && notes.isBlank()) {
                notes = null;
            }

            userAddress = userAddressService.updateLinkMetadata(
                    linkPublicId,
                    target,
                    label,
                    notes
            );
            log.debug("Updated metadata - label: {}, notes: {}",
                    label != null ? "updated" : "unchanged",
                    notes != null ? "updated" : "unchanged");
        }

        // 6.6. Update verification status (no dedicated service method)
        if (request.verifiedByOwner() != null) {
            userAddress.setVerifiedByOwner(request.verifiedByOwner());
            userAddress = userAddressRepository.save(userAddress);
            log.debug("Updated verification status: {}", request.verifiedByOwner());
        }

        log.info("Admin {} successfully updated address {} for user {}",
                admin.getUsername(), linkPublicId, target.getUsername());

        return userAddress;
    }

    @Override
    @Transactional
    public UserAddress createAddress(
            String userPublicId,
            CreateUserAddressRequest request) {

        log.debug("Admin creating new address for user: {}", userPublicId);

        // 1. Get the target user
        User target = userService.getUserByPublicId(userPublicId);

        // 2. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 3. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        log.debug("Admin {} creating address for user {}",
                admin.getUsername(), target.getUsername());

        // 4. Convert request to Address entity
        Address address = request.toAddressEntity();

        // 5. Delegate to UserAddressService (handles duplicate detection, rules, etc.)
        UserAddress userAddress = userAddressService.createAndLink(
                target,
                address,
                request.addressType(),
                request.label(),
                Boolean.TRUE.equals(request.isDefault()),
                Boolean.TRUE.equals(request.isPrimary()),
                request.isBillingEligible(),
                request.isShippingEligible()
        );

        log.info("Admin {} successfully created address {} for user {}",
                admin.getUsername(), userAddress.getPublicId(), target.getUsername());

        return userAddress;
    }


    @Override
    @Transactional
    public UserAddress softDeleteAddress(String userPublicId, String linkPublicId) {
        log.debug("Admin soft deleting address link - user: {}, link: {}", userPublicId, linkPublicId);

        // 1. Get the target user
        User target = userService.getUserByPublicId(userPublicId);

        // 2. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 3. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        log.debug("Admin {} soft deleting address {} for user {}",
                admin.getUsername(), linkPublicId, target.getUsername());

        // 4. Delegate to UserAddressService (sets active=false)
        UserAddress userAddress = userAddressService.unlinkAddress(linkPublicId, target);

        log.info("Admin {} successfully soft deleted address {} for user {}",
                admin.getUsername(), linkPublicId, target.getUsername());

        return userAddress;
    }

    @Override
    @Transactional
    public void hardDeleteAddress(String userPublicId, String linkPublicId) {
        log.debug("Admin hard deleting address link - user: {}, link: {}", userPublicId, linkPublicId);

        // 1. Get the target user
        User target = userService.getUserByPublicId(userPublicId);

        // 2. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 3. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        log.debug("Admin {} hard deleting address {} for user {}",
                admin.getUsername(), linkPublicId, target.getUsername());

        // 4. Delegate to UserAddressService (permanent deletion)
        userAddressService.removeAddressLink(linkPublicId, target);

        log.info("Admin {} successfully hard deleted address {} for user {}",
                admin.getUsername(), linkPublicId, target.getUsername());
    }
}
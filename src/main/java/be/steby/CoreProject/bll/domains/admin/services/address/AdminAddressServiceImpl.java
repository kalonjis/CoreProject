package be.steby.CoreProject.bll.domains.admin.services.address;

import be.steby.CoreProject.bll.domains.address.services.AddressService;
import be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dal.repositories.UserAddressRepository;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.UserAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link AdminAddressService}.
 *
 * <p>Orchestrates administrative address operations by:</p>
 * <ol>
 *   <li>Validating admin permissions</li>
 *   <li>Delegating to specialized services</li>
 *   <li>Logging administrative actions</li>
 * </ol>
 *
 * <h4>Transaction management:</h4>
 * <p>Read operations use {@code @Transactional(readOnly = true)} for optimization.</p>
 *
 * @see AdminAddressService
 * @see AddressService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminAddressServiceImpl implements AdminAddressService {

    private final AddressService addressService;
    private final UserAddressRepository userAddressRepository;
    private final UserService userService;
    private final AdminPermissionValidator adminPermissionValidator;
    private final AddressRepository addressRepository;

    // ========================================
    // region Address Retrieval
    // ========================================

    @Override
    public List<Address> getAllAddresses() {
        log.debug("Admin retrieving all addresses");

        // 1. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 2. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        // 3. Retrieve all addresses
        List<Address> addresses = addressRepository.findAll();

        log.info("Admin {} retrieved {} address(es)",
                admin.getUsername(), addresses.size());

        return addresses;
    }

    @Override
    public Address getAddressById(String publicId) {
        log.debug("Admin retrieving address: {}", publicId);

        // 1. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 2. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        // 3. Retrieve address
        Address address = addressService.getByPublicId(publicId);

        log.info("Admin {} viewed address {}",
                admin.getUsername(), publicId);

        return address;
    }

    @Override
    public List<UserAddress> getAddressUsers(String addressPublicId) {
        log.debug("Admin retrieving users linked to address: {}", addressPublicId);

        // 1. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 2. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        // 3. Verify address exists
        Address address = addressService.getByPublicId(addressPublicId);

        // 4. Retrieve all user links for this address
        List<UserAddress> userAddresses = userAddressRepository.findByAddress(address);

        log.info("Admin {} viewed {} user(s) linked to address {}",
                admin.getUsername(), userAddresses.size(), addressPublicId);

        return userAddresses;
    }

    // endregion


    @Override
    @Transactional
    public void deleteOrphanedAddress(String addressPublicId) {
        log.debug("Admin deleting orphaned address: {}", addressPublicId);

        // 1. Get authenticated admin
        User admin = userService.getAuthenticatedUser();

        // 2. Validate admin permissions
        adminPermissionValidator.validateAdminRole(admin);

        // 3. Verify address exists
        Address address = addressService.getByPublicId(addressPublicId);

        // 4. Check if address has any links (must be orphaned)
        long linkCount = userAddressRepository.countByAddress(address);
        if (linkCount > 0) {
            log.warn("Admin {} attempted to delete address {} which has {} link(s)",
                    admin.getUsername(), addressPublicId, linkCount);
            throw be.steby.CoreProject.bll.domains.address.exceptions.AddressStillInUseException
                    .forAddress(addressPublicId, linkCount);
        }

        // 5. Delete the orphaned address
        addressService.delete(addressPublicId);

        log.info("Admin {} successfully deleted orphaned address {}",
                admin.getUsername(), addressPublicId);
    }
}
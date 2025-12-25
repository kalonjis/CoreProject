package be.steby.CoreProject.bll.domains.admin.services.search;

import be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.DeviceRepository;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of AdminSearchService.
 * Handles administrative user search and query operations with proper permission validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSearchServiceImpl implements AdminSearchService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final UserService userService;
    private final AdminPermissionValidator adminPermissionValidator;

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, Pageable pageable) {
        log.debug("Admin searching users - query: '{}'", query);

        // Validate admin permissions
        User admin = userService.getAuthenticatedUser();
        adminPermissionValidator.validateAdminRole(admin);

        // Perform search
        Page<User> users;
        if (query == null || query.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.searchByMultipleFields(query, pageable);
        }

        log.debug("Search found {} users", users.getTotalElements());

        return users;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                            String email, String phoneNumber, Pageable pageable) {
        log.debug("Admin searching users by criteria");

        // Validate admin permissions
        User admin = userService.getAuthenticatedUser();
        adminPermissionValidator.validateAdminRole(admin);

        // Perform criteria search
        Page<User> users = userRepository.searchByCriteria(
                username, firstname, lastname, email, phoneNumber, pageable);

        log.debug("Criteria search found {} users", users.getTotalElements());

        return users;
    }

//    @Override
//    @Transactional(readOnly = true)
//    public Page<User> getAllUsers(Pageable pageable) {
//        log.debug("Admin retrieving all users");
//
//        // Validate admin permissions
//        User admin = userService.getAuthenticatedUser();
//        adminPermissionValidator.validateAdminRole(admin);
//
//        // Get all users
//        Page<User> users = userRepository.findAll(pageable);
//
//        log.debug("Retrieved {} users", users.getTotalElements());
//
//        return users;
//    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        log.debug("Admin retrieving user by ID: {}", userId);

        // Validate admin permissions
        User admin = userService.getAuthenticatedUser();
        adminPermissionValidator.validateAdminRole(admin);

        // Get user
        User user = userService.getUserById(userId);

        log.debug("Retrieved user: {}", user.getUsername());

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> getUserDevices(Long userId) {
        log.debug("Admin retrieving devices for user: {}", userId);

        // Validate admin permissions
        User admin = userService.getAuthenticatedUser();
        adminPermissionValidator.validateAdminRole(admin);

        // Get user and devices
        User user = userService.getUserById(userId);
        List<Device> devices = deviceRepository.findByUser(user);

        log.debug("Retrieved {} devices for user: {}", devices.size(), userId);

        return devices;
    }

    @Override
    public Long getTotalUsers() {
        return 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics() {
        log.debug("Admin retrieving user statistics");

        // Validate admin permissions
        User admin = userService.getAuthenticatedUser();
        adminPermissionValidator.validateAdminRole(admin);

        // Gather all user statistics atomically
        Map<String, Object> stats = new HashMap<>();

        // Total user count
        Long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);

        // Active vs deactivated users
        Long activeUsers = userRepository.countByEnabled(true);
        Long deactivatedUsers = userRepository.countByEnabled(false);
        stats.put("activeUsers", activeUsers);
        stats.put("deactivatedUsers", deactivatedUsers);

        // Verified vs unverified users
        Long verifiedUsers = userRepository.countByEmailVerified(true);
        Long unverifiedUsers = userRepository.countByEmailVerified(false);
        stats.put("verifiedUsers", verifiedUsers);
        stats.put("unverifiedUsers", unverifiedUsers);

        // Admin vs regular users
        Long adminUsers = userRepository.countUsersWithAdminRoles();
        Long regularUsers = totalUsers - adminUsers;
        stats.put("adminUsers", adminUsers);
        stats.put("regularUsers", regularUsers);

        log.debug("User statistics computed - total: {}, active: {}, admins: {}",
                totalUsers, activeUsers, adminUsers);

        return stats;
    }
}
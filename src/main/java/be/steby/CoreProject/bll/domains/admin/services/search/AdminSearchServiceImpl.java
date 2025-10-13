package be.steby.CoreProject.bll.domains.admin.services.search;

import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service implementation for admin search and query operations on users.
 * Handles user searches, retrieval, and statistics with proper permission validation.
 *
 * Responsibilities:
 * - Validates admin permissions for all operations
 * - Delegates search/query operations to UserService
 * - Delegates device retrieval to DeviceService
 * - Provides read-only access to user data
 *
 * Delegation strategy:
 * - UserService → Core user search and retrieval operations
 * - DeviceService → Device-related queries
 *
 * Note: All methods are read-only and require admin privileges.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSearchServiceImpl implements AdminSearchService {

    private final UserService userService;
    private final DeviceService deviceService;

    // ===============================
    // USER SEARCH OPERATIONS
    // ===============================

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, Pageable pageable) {
        log.debug("Admin search users request - query: '{}', page: {}, size: {}",
                query, pageable.getPageNumber(), pageable.getPageSize());

        // Validate admin permissions
        userService.requireAdminPermissions();

        // Delegate to user service
        Page<User> results = userService.searchUsers(query, pageable);

        log.info("Admin search completed - query: '{}', results: {} users found",
                query, results.getTotalElements());

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsersByCriteria(
            String username,
            String firstname,
            String lastname,
            String email,
            String phoneNumber,
            Pageable pageable) {

        log.debug("Admin search by criteria - username: {}, firstname: {}, lastname: {}, email: {}, phone: {}",
                username, firstname, lastname, email, phoneNumber);

        // Validate admin permissions
        userService.requireAdminPermissions();

        // Delegate to user service
        Page<User> results = userService.searchUsersByCriteria(
                username, firstname, lastname, email, phoneNumber, pageable
        );

        log.info("Admin criteria search completed - results: {} users found", results.getTotalElements());

        return results;
    }

    // ===============================
    // USER RETRIEVAL OPERATIONS
    // ===============================

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        log.debug("Admin get user by ID request - userId: {}", userId);

        // Validate admin permissions
        userService.requireAdminPermissions();

        // Delegate to user service
        User user = userService.getUserById(userId);

        log.debug("Admin retrieved user - ID: {}, username: {}", user.getId(), user.getUsername());

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> getUserDevices(Long userId) {
        log.debug("Admin get user devices request - userId: {}", userId);

        // Validate admin permissions
        userService.requireAdminPermissions();

        // Get user first (validates user exists)
        User user = userService.getUserById(userId);

        // Delegate to device service
        List<Device> devices = deviceService.getUserDevices(user);

        log.info("Admin retrieved devices for user {} - {} device(s) found",
                user.getUsername(), devices.size());

        return devices;
    }

    // ===============================
    // STATISTICS OPERATIONS
    // ===============================

    @Override
    @Transactional(readOnly = true)
    public Long getTotalUsers() {
        log.debug("Admin get total users request");

        // Validate admin permissions
        userService.requireAdminPermissions();

        // Delegate to user service
        Long totalUsers = userService.getTotalUsers();

        log.debug("Admin retrieved total users count: {}", totalUsers);

        return totalUsers;
    }
}
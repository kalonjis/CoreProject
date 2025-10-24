package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.search.AdminSearchService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.profile.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for admin user search and query operations.
 * Handles user searches, retrieval, device queries, and statistics.
 *
 * All endpoints require ADMIN privileges.
 * All operations are read-only.
 *
 * Business logic is delegated to AdminSearchService.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/users")
@Slf4j
public class AdminUserSearchController {

    private final AdminSearchService adminSearchService;

    // ===============================
    // USER SEARCH OPERATIONS
    // ===============================

    /**
     * Searches users with a global query across all fields.
     * GET /api/admin/users/search?query=...
     *
     * Searches in: username, firstname, lastname, email, phone number
     *
     * @param query Search query (optional, null returns all users)
     * @param pageable Pagination settings (default: page 0, size 20)
     * @return Paginated list of users
     */
    @GetMapping("/search")
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Admin user search - query: '{}', page: {}, size: {}",
                query, pageable.getPageNumber(), pageable.getPageSize());

        Page<User> users = adminSearchService.searchUsers(query, pageable);

        log.info("Admin search completed - {} users found", users.getTotalElements());

        return ResponseEntity.ok(users);
    }

    /**
     * Searches users by specific criteria with individual field filters.
     * GET /api/admin/users/searchbycriteria?username=...&email=...
     *
     * All parameters are optional and can be combined.
     *
     * @param username Username filter (partial match, case-insensitive)
     * @param firstname First name filter (partial match, case-insensitive)
     * @param lastname Last name filter (partial match, case-insensitive)
     * @param email Email filter (partial match, case-insensitive)
     * @param phoneNumber Phone number filter (partial match)
     * @param pageable Pagination settings (default: page 0, size 20)
     * @return Paginated list of users matching criteria
     */
    @GetMapping("/searchbycriteria")
    public ResponseEntity<Page<User>> searchUsersByCriteria(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String firstname,
            @RequestParam(required = false) String lastname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Admin criteria search - username: {}, firstname: {}, lastname: {}, email: {}, phone: {}",
                username, firstname, lastname, email, phoneNumber);

        Page<User> users = adminSearchService.searchUsersByCriteria(
                username, firstname, lastname, email, phoneNumber, pageable);

        log.info("Admin criteria search completed - {} users found", users.getTotalElements());

        return ResponseEntity.ok(users);
    }

    /**
     * Lists all users with pagination and sorting.
     * GET /api/admin/users/all?page=0&size=20&sort=id,asc
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sort Sort parameter in format "field,direction" (default: "id,asc")
     * @return Paginated list of all users
     */
    @GetMapping("/all")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        log.info("Admin list all users - page: {}, size: {}, sort: {}", page, size, sort);

        Pageable pageable = buildPageable(page, size, sort);
        Page<User> users = adminSearchService.searchUsers(null, pageable);

        log.info("Admin list completed - {} total users", users.getTotalElements());

        return ResponseEntity.ok(users);
    }

    // ===============================
    // USER RETRIEVAL OPERATIONS
    // ===============================

    /**
     * Gets a specific user by ID.
     * GET /api/admin/users/{id}
     *
     * @param id User ID
     * @return User DTO with user details
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        log.info("Admin get user by ID - userId: {}", id);

        User user = adminSearchService.getUserById(id);

        log.info("Admin retrieved user - userId: {}, username: {}", id, user.getUsername());

        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    /**
     * Gets all devices associated with a user.
     * GET /api/admin/users/{id}/devices
     *
     * @param id User ID
     * @return List of user's devices
     */
    @GetMapping("/{id}/devices")
    public ResponseEntity<List<Device>> getUserDevices(@PathVariable Long id) {
        log.info("Admin get user devices - userId: {}", id);

        List<Device> devices = adminSearchService.getUserDevices(id);

        log.info("Admin retrieved {} devices for userId: {}", devices.size(), id);

        return ResponseEntity.ok(devices);
    }

    // ===============================
    // STATISTICS OPERATIONS
    // ===============================

    /**
     * Gets admin statistics including total user count.
     * GET /api/admin/users/stats
     *
     * @return Statistics map with various metrics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        log.info("Admin get statistics request");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", adminSearchService.getTotalUsers());
        // TODO: Add more statistics as needed (active users, deactivated users, role distribution, etc.)

        log.info("Admin statistics retrieved - totalUsers: {}", stats.get("totalUsers"));

        return ResponseEntity.ok(stats);
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    /**
     * Builds a Pageable object from request parameters.
     *
     * @param page Page number
     * @param size Page size
     * @param sort Sort parameter in format "field,direction"
     * @return Configured Pageable object
     */
    private Pageable buildPageable(int page, int size, String sort) {
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
    }
}
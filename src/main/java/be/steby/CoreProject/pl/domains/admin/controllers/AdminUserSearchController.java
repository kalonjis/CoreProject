package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.search.AdminSearchService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.admin.models.responses.AdminUserDTO;
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

import java.time.Instant;
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
    public ResponseEntity<Page<AdminUserDTO>> searchUsers(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Admin user search - query: '{}', page: {}, size: {}",
                query, pageable.getPageNumber(), pageable.getPageSize());

        Page<AdminUserDTO> users = adminSearchService.searchUsers(query, pageable)
                .map(AdminUserDTO::fromEntity);

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
    public ResponseEntity<Page<AdminUserDTO>> searchUsersByCriteria(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String firstname,
            @RequestParam(required = false) String lastname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Admin criteria search - username: {}, firstname: {}, lastname: {}, email: {}, phone: {}",
                username, firstname, lastname, email, phoneNumber);

        Page<AdminUserDTO> users = adminSearchService.searchUsersByCriteria(
                username, firstname, lastname, email, phoneNumber, pageable)
                .map(AdminUserDTO::fromEntity);

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
    public ResponseEntity<Page<AdminUserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        log.info("Admin list all users - page: {}, size: {}, sort: {}", page, size, sort);

        Pageable pageable = buildPageable(page, size, sort);
        Page<AdminUserDTO> users = adminSearchService.searchUsers(null, pageable)
                .map(AdminUserDTO::fromEntity);

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
     * @param publicId User publicId
     * @return User DTO with user details
     */
    @GetMapping("/{publicId}")
    public ResponseEntity<AdminUserDTO> getUserById(@PathVariable String publicId) {
        log.info("Admin get user by ID - userId: {}", publicId);

        User user = adminSearchService.getUserByPublicId(publicId);

        log.info("Admin retrieved user - userId: {}, username: {}", publicId, user.getUsername());

        return ResponseEntity.ok(AdminUserDTO.fromEntity(user));
    }


    /**
     * Gets all devices associated with a user.
     * GET /api/admin/users/{id}/devices
     *
     * @param publicId User String publicId
     * @return List of user's devices
     */
    @GetMapping("/{publicId}/devices")
    public ResponseEntity<List<Device>> getUserDevices(@PathVariable String publicId) {
        log.info("Admin get user devices - userId: {}", publicId);

        List<Device> devices = adminSearchService.getUserDevices(publicId);

        log.info("Admin retrieved {} devices for userId: {}", devices.size(), publicId);

        return ResponseEntity.ok(devices);
    }

    // ===============================
    // USER STATISTICS
    // ===============================

    /**
     * Retrieves comprehensive user statistics for admin dashboard.
     * Provides all user-related metrics in a single efficient call.
     *
     * GET /api/admin/users/stats
     *
     * Response format:
     * {
     *   "totalUsers": 150,
     *   "activeUsers": 142,
     *   "deactivatedUsers": 8,
     *   "verifiedUsers": 145,
     *   "unverifiedUsers": 5,
     *   "adminUsers": 5,
     *   "regularUsers": 145,
     *   "timestamp": "2024-12-24T10:00:00Z"
     * }
     *
     * All metrics are computed atomically within a single transaction
     * to ensure consistency across all statistics.
     *
     * @return ResponseEntity with comprehensive user statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        log.info("Admin requesting user statistics");

        Map<String, Object> stats = adminSearchService.getUserStatistics();
        stats.put("timestamp", Instant.now());

        log.info("User statistics retrieved - total: {}, active: {}, admins: {}",
                stats.get("totalUsers"), stats.get("activeUsers"), stats.get("adminUsers"));

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
package be.steby.CoreProject.pl.domains.admin.models.responses;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.time.Instant;
import java.util.Set;

/**
 * DTO for admin user management.
 * Contains all necessary information for administrators to manage users.
 */
public record AdminUserDTO(
        long id,
        String publicId,
        String username,
        String firstname,
        String lastname,
        String email,
        String phoneNumber,
        Set<UserRole> userRoles,
        boolean enabled,
        boolean emailVerified,
        boolean phoneNumberVerified,
        boolean mustChangePassword,
        boolean twoFactorEnabled,
        Instant createdAt,
        Instant activatedAt,
        Instant deactivatedAt,
        Integer deviceCount
) {
    /**
     * Creates an AdminUserDTO from a User entity.
     * 
     * @param user User entity
     * @return AdminUserDTO with user information
     */
    public static AdminUserDTO fromEntity(User user) {
        return new AdminUserDTO(
                user.getId(),
                user.getPublicId(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getUserRoles(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.isPhoneNumberVerified(),
                user.isMustChangePassword(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt(),
                user.getActivatedAt(),
                user.getDeactivatedAt(),
                null // Device count will be set separately if needed
        );
    }

    /**
     * Creates an AdminUserDTO from a User entity with device count.
     * 
     * @param user User entity
     * @param deviceCount Number of devices associated with the user
     * @return AdminUserDTO with user information and device count
     */
    public static AdminUserDTO fromEntity(User user, Integer deviceCount) {
        return new AdminUserDTO(
                user.getId(),
                user.getPublicId(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getUserRoles(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.isPhoneNumberVerified(),
                user.isMustChangePassword(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt(),
                user.getActivatedAt(),
                user.getDeactivatedAt(),
                deviceCount
        );
    }
}
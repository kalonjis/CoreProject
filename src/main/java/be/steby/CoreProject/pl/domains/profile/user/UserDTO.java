package be.steby.CoreProject.pl.domains.profile.user;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.time.Instant;
import java.util.Set;

public record UserDTO(
        String publicId,
        String username,
        String firstname,
        String lastname,
        String email,
        String phoneNumber,
        String avatarUrl,
        Instant createdAt,
        Set<UserRole> userRoles,
        boolean mustChangePassword,
        boolean enabled
) {
    public static UserDTO fromEntity(User u){
        return new UserDTO(
                u.getPublicId(),
                u.getUsername(),
                u.getFirstname(),
                u.getLastname(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getAvatarUrl(),
                u.getCreatedAt(),
                u.getUserRoles(),
                u.isMustChangePassword(),
                u.isEnabled()
        );
    }
}

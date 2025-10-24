package be.steby.CoreProject.pl.domains.profile.user;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.time.Instant;
import java.util.Set;

public record UserDTO(
        Long id,
        String username,
        String firstname,
        String lastname,
        String email,
        String phoneNumber,
        Instant createdAt,
        Set<UserRole> userRoles,
        boolean mustChangePassword,
        boolean enabled
) {
    public static UserDTO fromEntity(User u){
        return new UserDTO(
                u.getId(),
                u.getUsername(),
                u.getFirstname(),
                u.getLastname(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getCreatedAt(),
                u.getUserRoles(),
                u.isMustChangePassword(),
                u.isEnabled()
        );
    }
}

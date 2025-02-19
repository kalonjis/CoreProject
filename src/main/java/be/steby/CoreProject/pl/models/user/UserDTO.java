package be.steby.CoreProject.pl.models.user;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.util.Set;

public record UserDTO(
        Long id,
        String username,
        String firstname,
        String lastname,
        String email,
        String phoneNumber,
        Set<UserRole> userRoles
) {
    public static UserDTO fromEntity(User u){
        return new UserDTO(
                u.getId(),
                u.getUsername(),
                u.getFirstname(),
                u.getLastname(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getUserRoles()
        );
    }
}

package be.steby.CoreProject.pl.domains.auth.models.responses;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;

import java.time.Instant;
import java.util.Set;

public record UserSessionResponse(
    String publicId,
    String username,
    String firstname,
    String lastname,
    String email,
    String phoneNumber,
    Set<UserRole> userRoles,
    boolean mustChangePassword,
    boolean twoFactorEnabled,
    boolean emailVerified,
    boolean phoneNumberVerified,
    Instant passwordChangedAt
) {
    public static UserSessionResponse fromEntity(User u){
        return new UserSessionResponse(
            u.getPublicId(),
                u.getUsername(),
                u.getFirstname(),
                u.getLastname(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getUserRoles(),
                u.isMustChangePassword(),
                u.isTwoFactorEnabled(),
                u.isEmailVerified(),
                u.isPhoneNumberVerified(),
                u.getPasswordChangedAt()
        );
    }
}

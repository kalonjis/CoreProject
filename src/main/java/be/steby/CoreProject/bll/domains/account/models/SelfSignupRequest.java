package be.steby.CoreProject.bll.domains.account.models;

import be.steby.CoreProject.dl.entities.User;

/**
 * Business layer DTO for self-signup operation.
 * Contains only the data needed for signup business logic.
 * BLL layer can convert this to User entity internally
 */
public record SelfSignupRequest(
        String email,
        String password
) {
    /**
     * Converts BLL DTO to User entity.
     */
    public User toEntity() {
        User user = new be.steby.CoreProject.dl.entities.User();
        user.setEmail(email);
        user.setPassword(password); // Will be encoded by service
        return user;
    }
}

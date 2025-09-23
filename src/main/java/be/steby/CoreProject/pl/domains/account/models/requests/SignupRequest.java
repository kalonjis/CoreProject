package be.steby.CoreProject.pl.domains.account.models.requests;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.validators.password.StrongPassword;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

/**
 * Request model for account signup operations.
 * Contains validation rules for user registration data.
 */
public record SignupRequest(
        @JsonProperty("username")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscores and hyphens")
        String username,

        @JsonProperty("email")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email cannot exceed 100 characters")
        String email,

        @JsonProperty("password")
        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        @JsonProperty("confirm_password")
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword,

        @JsonProperty("first_name")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        String firstName,

        @JsonProperty("last_name")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        String lastName,

        @JsonProperty("phone_number")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid international format")
        String phoneNumber
) {
    /**
     * Custom validation to ensure passwords match.
     */
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * Converts this request to a User entity for business logic processing.
     *
     * @return User entity with data from this request
     */
    public User toEntity() {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password); // Will be encoded by business logic
        user.setFirstname(firstName);
        user.setLastname(lastName);
        user.setPhoneNumber(phoneNumber);
        return user;
    }
}

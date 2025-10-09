package be.steby.CoreProject.pl.domains.admin.models.requests;

import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationRequest;
import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.validation.constraints.*;

import java.util.Set;

/**
 * Presentation layer request for admin user creation.
 * Contains all necessary validation for HTTP request data.
 *
 * This is the entry point from the controller, validated by Jakarta Validation.
 * Converts to BLL DTO via toBLL() method.
 */
public record AdminUserCreateRequest(

        @NotBlank(message = "First name cannot be blank")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstname,

        @NotBlank(message = "Last name cannot be blank")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastname,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email format")
        String email,

        @Pattern(regexp = "^[0-9]{9,15}$", message = "Phone number must be between 9 and 15 digits")
        String phoneNumber,

        @NotNull(message = "User roles cannot be null")
        @NotEmpty(message = "At least one role must be assigned")
        Set<UserRole> userRoles
) {

    /**
     * Converts PL request to BLL DTO.
     * This method bridges the presentation layer to the business layer.
     *
     * @return AdminUserCreationRequest for BLL processing
     */
    public AdminUserCreationRequest toBLL() {
        return new AdminUserCreationRequest(
                email,
                firstname,
                lastname,
                phoneNumber,
                userRoles
        );
    }
}
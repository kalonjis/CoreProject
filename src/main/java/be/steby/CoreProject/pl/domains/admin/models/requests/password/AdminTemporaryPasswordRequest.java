package be.steby.CoreProject.pl.domains.admin.models.requests.password;

import be.steby.CoreProject.bll.domains.admin.models.password.AdminTemporaryPasswordBLLRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Presentation layer request for sending temporary password.
 * Contains Jakarta validation for HTTP request data.
 */
public record AdminTemporaryPasswordRequest(

        @NotBlank(message = "Reason is required for audit purposes")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason

) {
    /**
     * Converts PL request to BLL DTO.
     * Bridges presentation layer to business layer.
     * Performs data cleaning (trim) before passing to BLL.
     *
     * @return AdminTemporaryPasswordBLLRequest for BLL processing
     */
    public AdminTemporaryPasswordBLLRequest toBLL() {
        return new AdminTemporaryPasswordBLLRequest(
                reason != null ? reason.trim() : null
        );
    }
}
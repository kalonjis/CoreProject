package be.steby.CoreProject.pl.domains.admin.models.requests.password;

import be.steby.CoreProject.bll.domains.admin.models.password.AdminPasswordResetLinkBLLRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordResetLinkRequest(
        @NotBlank(message = "Reason is required for audit purposes")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason // could be replaced by IT ticket number - reference when implemented
) {
    /**
     * Converts this PL request to a BLL DTO.
     * Performs data cleaning (trim) before passing to BLL.
     *
     * @return AdminPasswordResetLinkBLLRequest for BLL processing
     */
    public AdminPasswordResetLinkBLLRequest toBLL() {
        return new AdminPasswordResetLinkBLLRequest(
                reason != null ? reason.trim() : null
        );
    }
}
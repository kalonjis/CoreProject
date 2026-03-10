package be.steby.CoreProject.bll.domains.lead.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model for rejecting a lead.
 *
 * <p>A rejection reason is mandatory — it is persisted on the lead
 * for reporting and quality improvement purposes
 * (e.g. identifying recurring spam patterns or out-of-scope inquiries).</p>
 *
 * @param rejectionReason a short explanation of why the lead was rejected
 */
public record LeadRejectRequest(

        @NotBlank(message = "Rejection reason is required")
        @Size(max = 255, message = "Rejection reason must not exceed 255 characters")
        String rejectionReason

) {}
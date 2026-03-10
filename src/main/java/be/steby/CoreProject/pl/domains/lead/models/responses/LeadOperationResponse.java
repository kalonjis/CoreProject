package be.steby.CoreProject.pl.domains.lead.models.responses;

import be.steby.CoreProject.bll.domains.lead.models.LeadResult;

/**
 * Response model for public inquiry operations.
 *
 * @param success     Whether the operation was successful
 * @param message     User-friendly message
 * @param referenceId Reference ID for tracking (only on success)
 */
public record LeadOperationResponse(
        boolean success,
        String message,
        String referenceId
) {

    /**
     * Creates a response from a BLL result.
     *
     * @param result the business layer result
     * @return the response DTO
     */
    public static LeadOperationResponse from(LeadResult result) {
        return new LeadOperationResponse(
                result.success(),
                result.message(),
                result.publicId()
        );
    }

    /**
     * Creates a successful submission response.
     *
     * @param referenceId the inquiry reference ID
     * @return success response
     */
    public static LeadOperationResponse submitted(String referenceId) {
        return new LeadOperationResponse(
                true,
                "Your inquiry has been sent successfully. We will get back to you soon.",
                referenceId
        );
    }

    /**
     * Creates a fake success response for honeypot detection.
     * Returns success to avoid informing bots they were detected.
     *
     * @return fake success response
     */
    public static LeadOperationResponse honeypotFakeSuccess() {
        return new LeadOperationResponse(
                true,
                "Your inquiry has been sent successfully. We will get back to you soon.",
                null
        );
    }

    /**
     * Creates an error response.
     *
     * @param message error message
     * @return error response
     */
    public static LeadOperationResponse error(String message) {
        return new LeadOperationResponse(false, message, null);
    }
}
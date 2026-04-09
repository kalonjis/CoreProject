package be.steby.CoreProject.bll.domains.crm.lead.models;

/**
 * Result of a public inquiry submission.
 *
 * @param publicId Public identifier of the inquiry (for tracking)
 * @param success  Whether the submission was successful
 * @param message  User-friendly message describing the result
 */
public record LeadResult(
        String publicId,
        boolean success,
        String message
) {

    /**
     * Creates a successful result.
     *
     * @param publicId the inquiry public ID
     * @return successful result
     */
    public static LeadResult success(String publicId) {
        return new LeadResult(
                publicId,
                true,
                "Your inquiry has been sent successfully. We will get back to you soon."
        );
    }

    /**
     * Creates a failed result.
     *
     * @param message the error message
     * @return failed result
     */
    public static LeadResult failure(String message) {
        return new LeadResult(null, false, message);
    }
}
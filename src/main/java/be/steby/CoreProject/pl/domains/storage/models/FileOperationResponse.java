package be.steby.CoreProject.pl.domains.storage.models;

/**
 * Generic response for file operations (delete, archive, etc.).
 *
 * <p>Provides consistent response format for non-query operations.
 *
 * @param message   human-readable message
 * @param operation the operation performed
 * @param publicId  the file's public ID (if applicable)
 */
public record FileOperationResponse(
        String message,
        String operation,
        String publicId
) {
    /**
     * Creates a success response for file deletion.
     */
    public static FileOperationResponse deleted(String publicId) {
        return new FileOperationResponse(
                "File deleted successfully",
                "DELETE",
                publicId
        );
    }

    /**
     * Creates a success response for file archiving.
     */
    public static FileOperationResponse archived(String publicId) {
        return new FileOperationResponse(
                "File archived successfully",
                "ARCHIVE",
                publicId
        );
    }

    /**
     * Creates a success response for file restoration.
     */
    public static FileOperationResponse restored(String publicId) {
        return new FileOperationResponse(
                "File restored successfully",
                "RESTORE",
                publicId
        );
    }
}
package be.steby.CoreProject.pl.domains.profile.avatar.models.responses;

import be.steby.CoreProject.bll.domains.storage.models.FileUploadResult;

/**
 * Response DTO for avatar operations.
 *
 * @param publicId      unique file identifier for future operations
 * @param avatarUrl     URL to access the avatar image
 * @param formattedSize human-readable file size
 */
public record AvatarResponse(
        String publicId,
        String avatarUrl,
        String formattedSize
) {
    /**
     * Creates response from upload result.
     *
     * @param result the file upload result
     * @return avatar response DTO
     */
    public static AvatarResponse from(FileUploadResult result) {
        return new AvatarResponse(
                result.getPublicId(),
                result.getAccessUrl(),
                result.getFormattedSize()
        );
    }
}

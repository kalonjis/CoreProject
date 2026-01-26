package be.steby.CoreProject.pl.domains.storage.models;

import be.steby.CoreProject.bll.domains.storage.models.FileUploadResult;
import be.steby.CoreProject.dl.enums.FileCategory;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for file upload operations.
 *
 * <p>Contains essential information for the client after upload.
 * Uses record for immutability and compact syntax.
 *
 * @param publicId         unique file identifier for future operations
 * @param originalFilename the original filename
 * @param fileSize         size in bytes
 * @param formattedSize    human-readable size (e.g., "2.5 MB")
 * @param contentType      MIME type
 * @param category         file category
 * @param accessUrl        URL to access the file
 * @param imageWidth       image width in pixels (null for non-images)
 * @param imageHeight      image height in pixels (null for non-images)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileUploadResponse(
        String publicId,
        String originalFilename,
        long fileSize,
        String formattedSize,
        String contentType,
        FileCategory category,
        String accessUrl,
        Integer imageWidth,
        Integer imageHeight
) {
    /**
     * Creates response from BLL result.
     *
     * @param result the upload result from service
     * @return response DTO
     */
    public static FileUploadResponse from(FileUploadResult result) {
        return new FileUploadResponse(
                result.getPublicId(),
                result.getOriginalFilename(),
                result.getFileSize(),
                result.getFormattedSize(),
                result.getContentType(),
                result.getCategory(),
                result.getAccessUrl(),
                result.getImageWidth(),
                result.getImageHeight()
        );
    }
}
package be.steby.CoreProject.pl.domains.storage.models;

import be.steby.CoreProject.dl.entities.StoredFile;
import be.steby.CoreProject.dl.enums.FileCategory;
import be.steby.CoreProject.dl.enums.FileStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Response DTO for file metadata queries.
 *
 * <p>Provides detailed file information for display and management.
 * Does NOT expose internal paths or storage details.
 *
 * @param publicId         unique file identifier
 * @param originalFilename the original filename
 * @param fileSize         size in bytes
 * @param formattedSize    human-readable size
 * @param contentType      MIME type
 * @param category         file category
 * @param status           current status
 * @param accessUrl        URL to access the file
 * @param description      optional description/alt text
 * @param imageWidth       image width (null for non-images)
 * @param imageHeight      image height (null for non-images)
 * @param createdAt        upload timestamp
 * @param isPublic         whether file is publicly accessible
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileInfoResponse(
        String publicId,
        String originalFilename,
        long fileSize,
        String formattedSize,
        String contentType,
        FileCategory category,
        FileStatus status,
        String accessUrl,
        String description,
        Integer imageWidth,
        Integer imageHeight,
        Instant createdAt,
        boolean isPublic
) {
    /**
     * Creates response from entity.
     *
     * @param file      the stored file entity
     * @param accessUrl the computed access URL
     * @return response DTO
     */
    public static FileInfoResponse from(StoredFile file, String accessUrl) {
        return new FileInfoResponse(
                file.getPublicId(),
                file.getOriginalFilename(),
                file.getFileSize(),
                file.getFormattedSize(),
                file.getContentType(),
                file.getCategory(),
                file.getStatus(),
                accessUrl,
                file.getDescription(),
                file.getImageWidth(),
                file.getImageHeight(),
                file.getCreatedAt(),
                file.isPublic()
        );
    }
}
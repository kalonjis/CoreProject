package be.steby.CoreProject.bll.domains.storage.services;

import be.steby.CoreProject.bll.domains.storage.exceptions.FileNotFoundException;
import be.steby.CoreProject.bll.domains.storage.exceptions.FileStorageException;
import be.steby.CoreProject.bll.domains.storage.exceptions.FileValidationException;
import be.steby.CoreProject.bll.domains.storage.models.FileUploadResult;
import be.steby.CoreProject.dl.enums.FileCategory;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for file storage operations.
 *
 * <p>Abstracts the underlying storage mechanism (local filesystem, S3, etc.).
 * Implementations handle the physical storage while this interface provides
 * a consistent API for domain services.
 *
 * <p>Typical usage:
 * <pre>
 * {@code
 * // Store a file
 * FileUploadResult result = fileStorageService.store(file, FileCategory.AVATAR, userPublicId);
 *
 * // Retrieve a file
 * Resource resource = fileStorageService.loadAsResource(result.getPublicId());
 *
 * // Delete a file
 * fileStorageService.delete(result.getPublicId());
 * }
 * </pre>
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@code LocalFileStorageService} - Local filesystem storage</li>
 *   <li>{@code S3FileStorageService} - AWS S3 storage (future)</li>
 * </ul>
 *
 * @see LocalFileStorageService
 */
public interface FileStorageService {

    /**
     * Stores a file.
     *
     * <p>The file is validated before storage. If validation fails,
     * a {@link FileValidationException} is thrown.
     *
     * @param file          the file to store
     * @param category      the file category (determines validation rules)
     * @param ownerPublicId public ID of the owner (user, product, etc.)
     * @param ownerType     type of owner (USER, PRODUCT, etc.)
     * @return result containing file metadata and access URL
     * @throws FileValidationException if file validation fails
     * @throws FileStorageException    if storage operation fails
     */
    FileUploadResult store(MultipartFile file, FileCategory category, 
                           String ownerPublicId, String ownerType);

    /**
     * Stores a file with optional description.
     *
     * @param file          the file to store
     * @param category      the file category
     * @param ownerPublicId public ID of the owner
     * @param ownerType     type of owner
     * @param description   optional description/alt text
     * @return result containing file metadata and access URL
     * @throws FileValidationException if file validation fails
     * @throws FileStorageException    if storage operation fails
     */
    FileUploadResult store(MultipartFile file, FileCategory category,
                           String ownerPublicId, String ownerType, String description);

    /**
     * Loads a file as a Spring Resource.
     *
     * <p>Used for serving files via HTTP response.
     *
     * @param publicId the file's public ID
     * @return the file as a Resource
     * @throws FileNotFoundException if file not found
     */
    Resource loadAsResource(String publicId);

    /**
     * Deletes a file (soft delete).
     *
     * <p>Marks the file for deletion. Physical removal happens
     * via scheduled cleanup job.
     *
     * @param publicId the file's public ID
     * @throws FileNotFoundException if file not found
     */
    void delete(String publicId);

    /**
     * Permanently deletes a file.
     *
     * <p>Removes both metadata and physical file immediately.
     * Use with caution - this operation cannot be undone.
     *
     * @param publicId the file's public ID
     * @throws FileNotFoundException if file not found
     */
    void deletePermanently(String publicId);

    /**
     * Checks if a file exists.
     *
     * @param publicId the file's public ID
     * @return true if file exists and is accessible
     */
    boolean exists(String publicId);

    /**
     * Gets the URL for accessing a file.
     *
     * @param publicId the file's public ID
     * @return the access URL
     * @throws FileNotFoundException if file not found
     */
    String getAccessUrl(String publicId);
}
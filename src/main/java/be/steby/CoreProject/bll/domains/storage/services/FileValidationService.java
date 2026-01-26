package be.steby.CoreProject.bll.domains.storage.services;

import be.steby.CoreProject.bll.domains.storage.config.StorageConfiguration;
import be.steby.CoreProject.bll.domains.storage.exceptions.FileValidationException;
import be.steby.CoreProject.bll.domains.storage.validators.FileValidator;
import be.steby.CoreProject.dl.enums.FileCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service orchestrating file validation.
 *
 * <p>Coordinates multiple validators based on file category.
 * Applies common validations (security checks) before
 * delegating to category-specific validators.
 *
 * <p>Validation flow:
 * <ol>
 *   <li>Common security checks (blocked extensions, etc.)</li>
 *   <li>Category-specific validators (in priority order)</li>
 * </ol>
 *
 * @see FileValidator
 */
@Service
@Slf4j
public class FileValidationService {

    private final StorageConfiguration storageConfig;
    private final List<FileValidator> validators;

    public FileValidationService(StorageConfiguration storageConfig, List<FileValidator> validators) {
        this.storageConfig = storageConfig;
        // Sort validators by priority (lower = first)
        this.validators = validators.stream()
                .sorted(Comparator.comparingInt(FileValidator::getPriority))
                .toList();
        
        log.info("FileValidationService initialized with {} validators", validators.size());
    }

    /**
     * Validates a file for the given category.
     *
     * @param file     the file to validate
     * @param category the target category
     * @throws FileValidationException if validation fails
     */
    public void validate(MultipartFile file, FileCategory category) {
        List<String> allErrors = new ArrayList<>();

        // 1. Basic null/empty checks
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("No file provided or file is empty");
        }

        // 2. Common security validations
        allErrors.addAll(validateSecurity(file));

        // 3. Run category-specific validators
        for (FileValidator validator : validators) {
            if (validator.supports(category)) {
                log.debug("Running validator: {} for category: {}",
                        validator.getClass().getSimpleName(), category);
                
                List<String> errors = validator.validate(file, category);
                allErrors.addAll(errors);
            }
        }

        // 4. Throw if any errors
        if (!allErrors.isEmpty()) {
            log.warn("File validation failed for {}: {}", 
                    file.getOriginalFilename(), allErrors);
            throw new FileValidationException(allErrors);
        }

        log.debug("File validation passed: {}, category: {}",
                file.getOriginalFilename(), category);
    }

    /**
     * Validates a file and returns errors without throwing.
     *
     * @param file     the file to validate
     * @param category the target category
     * @return list of validation errors (empty if valid)
     */
    public List<String> validateAndGetErrors(MultipartFile file, FileCategory category) {
        try {
            validate(file, category);
            return List.of();
        } catch (FileValidationException e) {
            return e.getValidationErrors();
        }
    }

    /**
     * Checks if a file is valid for the given category.
     *
     * @param file     the file to check
     * @param category the target category
     * @return true if valid
     */
    public boolean isValid(MultipartFile file, FileCategory category) {
        return validateAndGetErrors(file, category).isEmpty();
    }

    // =========================================================================
    // Private Methods
    // =========================================================================

    /**
     * Performs common security validations.
     */
    private List<String> validateSecurity(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        var security = storageConfig.getSecurity();

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            errors.add("Filename is required");
            return errors;
        }

        // Extract extension
        String extension = getExtension(originalFilename);

        // Check blocked extensions
        if (extension != null && security.isExtensionBlocked(extension)) {
            errors.add("File type '" + extension + "' is not allowed");
        }

        // Check for path traversal attempts
        if (originalFilename.contains("..") || 
            originalFilename.contains("/") || 
            originalFilename.contains("\\")) {
            log.warn("Potential path traversal attempt detected: {}", originalFilename);
            errors.add("Invalid filename");
        }

        // Check global max size
        if (file.getSize() > storageConfig.getMaxFileSize()) {
            errors.add(String.format("File exceeds maximum allowed size of %s",
                    formatBytes(storageConfig.getMaxFileSize())));
        }

        return errors;
    }

    /**
     * Extracts file extension from filename.
     */
    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }

    /**
     * Formats bytes to human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
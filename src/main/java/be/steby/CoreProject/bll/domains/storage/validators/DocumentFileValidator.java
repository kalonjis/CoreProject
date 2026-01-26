package be.steby.CoreProject.bll.domains.storage.validators;

import be.steby.CoreProject.bll.domains.storage.config.StorageConfiguration;
import be.steby.CoreProject.bll.domains.storage.config.StorageConfiguration.CategoryConfig;
import be.steby.CoreProject.dl.enums.FileCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validator for document files.
 *
 * <p>Validates:
 * <ul>
 *   <li>MIME type (PDF, Word, images)</li>
 *   <li>File size</li>
 *   <li>File extension consistency</li>
 * </ul>
 *
 * <p>Supports category: DOCUMENT
 *
 * @see FileValidator
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentFileValidator implements FileValidator {

    private final StorageConfiguration storageConfig;

    private static final Set<FileCategory> SUPPORTED_CATEGORIES = Set.of(
            FileCategory.DOCUMENT
    );

    /**
     * Mapping of MIME types to expected extensions.
     * Used to detect MIME type spoofing.
     */
    private static final Map<String, Set<String>> MIME_TO_EXTENSIONS = Map.of(
            "application/pdf", Set.of("pdf"),
            "application/msword", Set.of("doc"),
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", Set.of("docx"),
            "image/jpeg", Set.of("jpg", "jpeg"),
            "image/png", Set.of("png")
    );

    @Override
    public boolean supports(FileCategory category) {
        return SUPPORTED_CATEGORIES.contains(category);
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public List<String> validate(MultipartFile file, FileCategory category) {
        List<String> errors = new ArrayList<>();

        // Get category config
        CategoryConfig config = storageConfig.getCategoryConfig("document");

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        // 1. Check MIME type is present
        if (contentType == null || contentType.isBlank()) {
            errors.add("Unable to determine file type");
            return errors;
        }

        // 2. Check against allowed types
        if (config != null && !config.isTypeAllowed(contentType)) {
            errors.add("File type '" + contentType + "' is not allowed for documents");
        }

        // 3. Check file size
        long maxSize = config != null ? config.getMaxFileSize() : storageConfig.getMaxFileSize();
        if (file.getSize() > maxSize) {
            errors.add(String.format("Document too large. Maximum: %s, Actual: %s",
                    formatBytes(maxSize), formatBytes(file.getSize())));
        }

        // 4. Validate extension matches MIME type (detect spoofing)
        if (originalFilename != null && !originalFilename.isBlank()) {
            String extension = getExtension(originalFilename);
            if (extension != null && !isExtensionValidForMime(contentType, extension)) {
                log.warn("MIME type mismatch detected: {} with extension .{}",
                        contentType, extension);
                errors.add("File extension does not match content type");
            }
        }

        // 5. Check for empty file
        if (file.isEmpty() || file.getSize() == 0) {
            errors.add("Document file is empty");
        }

        log.debug("Document validated: {}, {} bytes, type: {}",
                originalFilename, file.getSize(), contentType);

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
     * Checks if extension is valid for the given MIME type.
     */
    private boolean isExtensionValidForMime(String mimeType, String extension) {
        Set<String> validExtensions = MIME_TO_EXTENSIONS.get(mimeType.toLowerCase());
        if (validExtensions == null) {
            // Unknown MIME type - allow but log
            log.debug("Unknown MIME type for extension validation: {}", mimeType);
            return true;
        }
        return validExtensions.contains(extension.toLowerCase());
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
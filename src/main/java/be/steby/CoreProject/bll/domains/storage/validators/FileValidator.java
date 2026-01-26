package be.steby.CoreProject.bll.domains.storage.validators;

import be.steby.CoreProject.dl.enums.FileCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Interface for file validators.
 *
 * <p>Validators are responsible for checking files against
 * category-specific rules. Each validator handles specific
 * file types (images, documents, etc.).
 *
 * <p>Implementation pattern:
 * <pre>
 * {@code
 * @Component
 * public class ImageFileValidator implements FileValidator {
 *     @Override
 *     public boolean supports(FileCategory category) {
 *         return category == FileCategory.AVATAR || category == FileCategory.PRODUCT_IMAGE;
 *     }
 * }
 * }
 * </pre>
 *
 * @see FileValidationService
 */
public interface FileValidator {

    /**
     * Checks if this validator supports the given category.
     *
     * @param category the file category
     * @return true if this validator should handle the category
     */
    boolean supports(FileCategory category);

    /**
     * Validates a file.
     *
     * @param file     the file to validate
     * @param category the target category
     * @return list of validation errors (empty if valid)
     */
    List<String> validate(MultipartFile file, FileCategory category);

    /**
     * Returns the priority of this validator.
     * Lower values = higher priority (executed first).
     * Default: 100
     *
     * @return priority value
     */
    default int getPriority() {
        return 100;
    }
}
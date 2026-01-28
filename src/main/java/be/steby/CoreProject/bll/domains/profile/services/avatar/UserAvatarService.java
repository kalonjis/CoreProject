package be.steby.CoreProject.bll.domains.profile.services.avatar;

import be.steby.CoreProject.bll.domains.storage.models.FileUploadResult;
import be.steby.CoreProject.dl.entities.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for managing user avatar operations.
 *
 * <p>Orchestrates file storage and user profile updates for avatar management.
 * Delegates file operations to {@link be.steby.CoreProject.bll.domains.storage.services.FileStorageService}
 * while handling user-specific business logic.</p>
 *
 * @see be.steby.CoreProject.bll.domains.storage.services.FileStorageService
 */
public interface UserAvatarService {

    /**
     * Uploads and sets the user's profile avatar.
     *
     * <p>Stores the image file and updates the user's avatarUrl in a single transaction.</p>
     *
     * @param file the avatar image to upload
     * @param user the user to update
     * @return result containing file metadata and access URL
     * @throws be.steby.CoreProject.bll.domains.storage.exceptions.FileValidationException if file validation fails
     * @throws be.steby.CoreProject.bll.domains.storage.exceptions.FileStorageException if storage fails
     */
    FileUploadResult setAvatar(MultipartFile file, User user);

    /**
     * Removes the user's profile avatar.
     *
     * <p>Clears the avatarUrl from user profile. Optionally deletes the file from storage.</p>
     *
     * @param user          the user to update
     * @param deleteFile    if true, also deletes the file from storage
     */
    void removeAvatar(User user, boolean deleteFile);

    /**
     * Removes the user's profile avatar without deleting the file.
     *
     * @param user the user to update
     */
    default void removeAvatar(User user) {
        removeAvatar(user, false);
    }
}
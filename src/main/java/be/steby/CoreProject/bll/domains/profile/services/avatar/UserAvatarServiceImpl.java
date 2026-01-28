package be.steby.CoreProject.bll.domains.profile.services.avatar;

import be.steby.CoreProject.bll.domains.storage.models.FileUploadResult;
import be.steby.CoreProject.bll.domains.storage.services.FileStorageService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.FileCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of {@link UserAvatarService}.
 *
 * <p>Handles avatar upload, storage, and user profile updates.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAvatarServiceImpl implements UserAvatarService {

    private final FileStorageService fileStorageService;
    private final UserService userService;

    @Override
    @Transactional
    public FileUploadResult setAvatar(MultipartFile file, User user) {
        log.debug("Setting avatar for user: {}", user.getUsername());

        // Store file as AVATAR category
        FileUploadResult result = fileStorageService.store(
                file,
                FileCategory.AVATAR,
                user.getPublicId(),
                "USER"
        );

        // Update user profile
        user.setAvatarUrl(result.getAccessUrl());
        userService.saveUser(user);

        log.info("Avatar set for user {}: {}", user.getUsername(), result.getAccessUrl());

        return result;
    }

    @Override
    @Transactional
    public void removeAvatar(User user, boolean deleteFile) {
        log.debug("Removing avatar for user: {}, deleteFile: {}", user.getUsername(), deleteFile);

        String currentAvatarUrl = user.getAvatarUrl();

        if (currentAvatarUrl == null) {
            log.debug("User {} has no avatar to remove", user.getUsername());
            return;
        }

        // Optionally delete file from storage
        if (deleteFile) {
            String publicId = extractPublicIdFromUrl(currentAvatarUrl);
            if (publicId != null) {
                try {
                    fileStorageService.delete(publicId);
                    log.debug("Deleted avatar file: {}", publicId);
                } catch (Exception e) {
                    log.warn("Failed to delete avatar file {}: {}", publicId, e.getMessage());
                }
            }
        }

        // Clear user's avatar URL
        user.setAvatarUrl(null);
        userService.saveUser(user);

        log.info("Avatar removed for user {}", user.getUsername());
    }

    /**
     * Extracts file publicId from access URL.
     *
     * @param url the access URL
     * @return the publicId or null if extraction fails
     */
    private String extractPublicIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        // URL format: /api/files/{publicId}
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }

        return null;
    }
}
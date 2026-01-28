package be.steby.CoreProject.pl.domains.profile.avatar.controller;

import be.steby.CoreProject.bll.domains.profile.services.avatar.UserAvatarService;
import be.steby.CoreProject.bll.domains.storage.models.FileUploadResult;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.domains.profile.avatar.models.responses.AvatarResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller for user avatar management.
 *
 * <p>Provides endpoints for uploading and removing user profile avatars.</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST   /api/profile/avatar → Upload and set avatar</li>
 *   <li>DELETE /api/profile/avatar → Remove avatar</li>
 * </ul>
 *
 * @see UserAvatarService
 */
@RestController
@RequestMapping("/api/profile/avatar")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Avatar", description = "User avatar management operations")
public class AvatarController {

    private final UserAvatarService userAvatarService;

    // =========================================================================
    // Endpoints
    // =========================================================================

    /**
     * Uploads and sets the user's profile avatar.
     *
     * <p>Accepts an image file, validates it, stores it, and updates the user's profile.</p>
     *
     * @param file the avatar image to upload
     * @param user the authenticated user
     * @return avatar response with the new URL
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload avatar",
            description = "Uploads an image and sets it as the user's profile avatar"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed (invalid type, size, or dimensions)"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "413", description = "File too large")
    })
    public ResponseEntity<AvatarResponse> uploadAvatar(
            @Parameter(description = "Avatar image file", required = true)
            @RequestParam("file") MultipartFile file,

            @AuthenticationPrincipal User user
    ) {
        log.info("Avatar upload request: filename={}, size={}, user={}",
                file.getOriginalFilename(), file.getSize(), user.getUsername());

        FileUploadResult result = userAvatarService.setAvatar(file, user);

        return ResponseEntity.ok(AvatarResponse.from(result));
    }

    /**
     * Removes the user's profile avatar.
     *
     * @param deleteFile if true, also deletes the file from storage
     * @param user       the authenticated user
     * @return 204 No Content
     */
    @DeleteMapping
    @Operation(
            summary = "Remove avatar",
            description = "Removes the user's profile avatar"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avatar removed successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Void> removeAvatar(
            @Parameter(description = "Also delete the file from storage")
            @RequestParam(value = "deleteFile", defaultValue = "false") boolean deleteFile,

            @AuthenticationPrincipal User user
    ) {
        log.info("Avatar remove request: user={}, deleteFile={}", user.getUsername(), deleteFile);

        userAvatarService.removeAvatar(user, deleteFile);

        return ResponseEntity.noContent().build();
    }
}
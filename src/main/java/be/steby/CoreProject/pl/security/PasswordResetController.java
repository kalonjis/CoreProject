package be.steby.CoreProject.pl.security;


import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.bll.services.UserService;
import be.steby.CoreProject.bll.services.security.impl.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.pl.security.models.RequestPasswordForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordResetController {

  // Required dependencies for password reset functionality
  public final UserService userService;
  public final PasswordResetTokenServiceImpl passwordResetTokenService;
  public final MailerService mailerService;

  /**
   * Endpoint to reset the user's password using the provided token.
   *
   * @param token The token received from the password reset email.
   * @param form  The password reset form containing the new password.
   * @return ResponseEntity with a message about the result of the operation.
   */
//  @PreAuthorize("isAnonymous()")
//  @PostMapping("/reset-password")
//  public ResponseEntity<Map<String, String>> resetPassword(@RequestParam String token, @Valid @RequestBody PasswordResetForm form) {
//    PasswordResetToken passwordToken = passwordResetTokenService.getOne(token);
//    Map<String, String> response = new HashMap<>();
//
//         //Vérifier si le token a expiré
//        if (passwordToken.getExpiryDate().isBefore(LocalDateTime.now())) {
//            String requestNewTokenUrl = BACK_URL + "/password/request-passwordtoken?token=" + token;
//            String message = "Link has expired. Please restart procedure.";
//            response.put("error", message);
//            response.put("url", requestNewTokenUrl);
//            return ResponseEntity.badRequest()
//                    .header("Content-Type", "application/json")
//                    .body(response);
//        }
//
//    // Reset the password
//    User user = passwordToken.getUser();
//    authService.resetPassword(user, form.password());
//
//    response.put("message", "Thank you. Your password has been successfully modified. You can now use it to connect to your favorite app.");
//    return ResponseEntity.ok(response);
//  }


  /**
   * Endpoint to request a new password reset token if the old one has expired.
   *
   * @param token The expired token.
   * @return ResponseEntity with a message indicating that a new token has been sent.
   */
//  @GetMapping("/request-passwordtoken")
//  public ResponseEntity<Map<String, String>> requestNewToken(@RequestParam String token) {
//    PasswordResetToken passwordToken = passwordResetTokenService.getOne(token);
//    Map<String, String> response = new HashMap<>();
//
//    // If the token is invalid
//    if (passwordToken == null) {
//      response.put("error", "Invalid token");
//      return ResponseEntity.badRequest().body(response);
//    }
//    // Generate a new token and send the reset email
//    String newToken = passwordResetTokenService.generateNewToken(token, PasswordResetToken.class, 600L).getToken();
//    mailerService.sendPasswordResetEmail(newToken);
//
//        response.put("message", "A new email with instructions for password resetting has been sent to your email box.");
//        return ResponseEntity.ok(response);
//    }


  /**
   * Endpoint to request a password reset email if the user has forgotten their password.
   *
   * @param form Contains the user's email address.
   * @return ResponseEntity with a message indicating that the reset email has been sent.
   */
  @PreAuthorize("isAnonymous()")
  @PostMapping("/request-password")
  public ResponseEntity<Map<String, String>> requestPassword(@Valid @RequestBody RequestPasswordForm form) {
    Map<String, String> response = new HashMap<>();
    User user = userService.getUserByEmail(form.email());

    // Generate a token and send the reset email
    PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(user);
    mailerService.sendPasswordResetEmail(token.getToken(), user);

    response.put("message", "Check your inbox. If your e-mail address matches our database, you will receive an e-mail asking you to reset your password.");
    return ResponseEntity.ok(response);
  }


  /**
   * Endpoint for authenticated users to change their password.
   *
   * @param request Contains the current password, new password, and password confirmation.
   * @return ResponseEntity with a success or error message.
   */
//    @PostMapping("/change-password")
//    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordForm request) {
//        Map<String, String> response = new HashMap<>();
//
//    // Check if the user is authenticated
//    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//    if (authentication == null || !authentication.isAuthenticated()) {
//      response.put("error", "User is not authenticated");
//      return ResponseEntity.badRequest()
//        .header("Content-Type", "application/json")
//        .body(response);
//    }
//
//    // Ensure the new password and confirmation match
//    if (!request.newPassword().equals(request.confirmPassword())) {
//      response.put("error", "New password and confirmation do not match");
//      return ResponseEntity.badRequest()
//        .header("Content-Type", "application/json")
//        .body(response);
//    }
//      // Get the username of the authenticated user
//        String username = authentication.getName(); // Récupère le nom d'utilisateur (ou principal)
//      // Change the password
//        authService.changePassword(username, request.currentPassword(), request.newPassword());
//
//        // Réponse de succès avec message de confirmation
//        response.put("message", "Thank you. Your password changed successfully.");
//        return ResponseEntity.ok()
//                .header("Content-Type", "application/json")
//                .body(response);
//    }



}

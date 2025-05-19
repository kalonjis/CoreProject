package be.steby.CoreProject.pl.security;


import be.steby.CoreProject.bll.domains.password.models.PasswordChangeRequest;
import be.steby.CoreProject.bll.domains.password.models.PasswordResetRequest;
import be.steby.CoreProject.bll.domains.password.services.PasswordService;
import be.steby.CoreProject.pl.security.models.ChangePasswordForm;
import be.steby.CoreProject.pl.security.models.PasswordResetForm;
import be.steby.CoreProject.pl.security.models.RequestPasswordForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordController {

  // Required dependencies for password reset functionality
  private final PasswordService passwordService;

  /**
   * Endpoint to request a password reset email if the user has forgotten their password.
   *
   * @param form Contains the user's email address.
   * @return ResponseEntity with a message indicating that the reset email has been sent.
   */

  @PostMapping("/request-password-reset")
  public ResponseEntity<Map<String, String>> requestPassword(@Valid @RequestBody RequestPasswordForm form, HttpServletRequest request) {

    passwordService.requestPasswordReset(form.email(), request);

    Map<String, String> response = new HashMap<>();
    response.put("message", "Check your inbox. If your e-mail address matches our database, you will receive an e-mail asking you to reset your password.");
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint to reset the user's password using the provided token.
   *
   * @param token The token received from the password reset email.
   * @param form  The password reset form containing the new password.
   * @return ResponseEntity with a message about the result of the operation.
   */
  @PutMapping("/reset-password")
  public ResponseEntity<Map<String, String>> resetPassword(@RequestParam String token, @Valid @RequestBody PasswordResetForm form, HttpServletRequest request) {

    passwordService.resetPassword(PasswordResetRequest.fromForm(form), token, request);

    Map<String, String> response = new HashMap<>();
    response.put("message", "Thank you. Your password has been successfully modified. You can now use it to connect to your favorite app.");
    return ResponseEntity.ok(response);
  }


  /**
   * Endpoint to request a new password reset token if the old one has expired.
   *
   * @param token The expired token.
   * @return ResponseEntity with a message indicating that a new token has been sent.
   */
  @GetMapping("/request-password-token")
  public ResponseEntity<Map<String, String>> requestNewToken(@RequestParam String token, HttpServletRequest httpRequest) {
    passwordService.requestPasswordToken(token, httpRequest);
    Map<String, String> response = new HashMap<>();
    response.put("message", "A new email with instructions for password resetting has been sent to your email box.");
    return ResponseEntity.ok(response);
    }


  /**
   * Endpoint for authenticated users to change their password.
   *
   * @param form Contains the current password, new password, and password confirmation.
   * @return ResponseEntity with a success or error message.
   */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordForm form, HttpServletRequest request) {

      passwordService.changePassword(PasswordChangeRequest.fromForm(form), request );

      Map<String, String> response = new HashMap<>();
      response.put("message", "Thank you. Your password changed successfully.");
      return ResponseEntity.ok()
              .header("Content-Type", "application/json")
              .body(response);
    }



}

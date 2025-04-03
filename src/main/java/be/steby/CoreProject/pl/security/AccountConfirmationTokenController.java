package be.steby.CoreProject.pl.security;


import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.bll.services.security.AuthService;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;



@RequiredArgsConstructor
@RestController
@RequestMapping("/api/account-confirmation")
public class AccountConfirmationTokenController {

  // Required dependencies injected via constructor
  private final AuthService authService;
  private final DeviceService deviceService;

  /**
   * Handles account confirmation when a user clicks the registration confirmation link.
   *
   * @param token The token sent to the user for account confirmation.
   * @return ResponseEntity with the status and a message.
   */
  @GetMapping("/activation")
  public ResponseEntity<Map<String, String>> confirmAccount(@RequestParam String token, HttpServletRequest request) {
      User user = authService.confirmNewUserAccount(token);
      deviceService.detectAndRegisterDevice(request, user, false);
      Map<String, String> response = new HashMap<>();
      response.put("message", "Thank you. Your account has been successfully activated. You can now use it to connect to your favorite app.");
      return ResponseEntity.ok()
              .header("Content-Type", "application/json")
              .body(response);
  }


  /**
   * Handles the request for a new confirmation token if the old one has expired.
   *
   * @param token The expired token.
   * @return ResponseEntity with the status and a message.
   */
  @GetMapping("/request-activation")
  public ResponseEntity<Map<String, String>> requestActivation(@RequestParam String token) {
      authService.requestActivation(token);
      Map<String, String> response = new HashMap<>();
      response.put("message", "A new confirmation email has been sent.");
      return ResponseEntity.ok()
          .header("Content-Type", "application/json")
          .body(response);
  }


    /**
     * Handles the request for a new confirmation link using the username.
     * This is used when a user tries to log in but their account is not yet activated.
     *
     * @param username The username for which to send a new activation link.
     * @return ResponseEntity with the status and a message.
     */
    @GetMapping("/request-confirmation-by-username")
    public ResponseEntity<Map<String, String>> requestConfirmationByUsername(@RequestParam String username) {
        authService.requestConfirmationLinkByUsername(username);
        Map<String, String> response = new HashMap<>();
        response.put("message", "A new confirmation email has been sent to your registered email address.");
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(response);
    }
}
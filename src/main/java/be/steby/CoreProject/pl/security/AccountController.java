package be.steby.CoreProject.pl.security;


import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.bll.domains.account.services.AccountService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.security.models.AccountDeactivationForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;



@RequiredArgsConstructor
@RestController
@RequestMapping("/api/account")
public class AccountController {

  // Required dependencies injected via constructor
  private final AccountService accountService;
  private final UserService userService;
  private final DeviceService deviceService;

  /**
   * Handles account confirmation when a user clicks the registration confirmation link.
   *
   * @param token The token sent to the user for account confirmation.
   * @return ResponseEntity with the status and a message.
   */
  @GetMapping("/activation")
  public ResponseEntity<Map<String, String>> confirmAccount(@RequestParam String token, HttpServletRequest request) {
      User user = accountService.confirmNewUserAccount(token, request);
      //deviceService.detectAndRegisterDevice(request, user, false);
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
  public ResponseEntity<Map<String, String>> requestActivation(@RequestParam String token, HttpServletRequest request) {
      accountService.requestActivation(token, request);
      Map<String, String> response = new HashMap<>();
      response.put("message", "A new confirmation email has been sent.");
      return ResponseEntity.ok()
          .header("Content-Type", "application/json")
          .body(response);
  }


    @PostMapping("/deactivation-request")
    public ResponseEntity<Map<String, String>> requestDeactivation(
            @Valid @RequestBody AccountDeactivationForm form,
            HttpServletRequest request
        ) {
        DeactivationRequest deactivationRequest = form.toBusiness();
        User user = userService.getAuthenticatedUser();
        accountService.requestDeactivation(user, deactivationRequest,  request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "A new deactivation email has been sent for confirmation.");
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(response);
    }

    @GetMapping("/deactivation")
    public ResponseEntity<Map<String, String>> deactivateAccount(
            @RequestParam String token,
            HttpServletRequest request
        ) {
        accountService.deactivateAccount(token, request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Your account has been successfully deactivated. ");
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(response);
    }
}
//package be.steby.CoreProject.pl.security;
//
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//
//
//@RequiredArgsConstructor
//@RestController
//public class AccountConfirmationTokenController {
//
//  // Required dependencies injected via constructor
//  private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
//  private final AuthService authService;
//  private final MailerService mailerService;
//
//  /**
//   * Handles account confirmation when a user clicks the registration confirmation link.
//   *
//   * @param token The token sent to the user for account confirmation.
//   * @return ResponseEntity with the status and a message.
//   */
//  @GetMapping("/registrationConfirm")
//  public ResponseEntity<Map<String, String>> confirmAccount(@RequestParam String token) {
//    // Retrieve the token details from the service
//
//    AccountConfirmationToken userToken = accountConfirmationTokenService.getOne(token);
//    Map<String, String> response = new HashMap<>();
//
//        // Récupérer l'utilisateur correspondant au token
//        User user = userToken.getUser();
//
//    // Confirm the user's email as valid
//    authService.confirmUserEmail(user);
//
//    // If the account is already activated
//    if (user.isEnabled()) {
//      response.put("type", "activation");
//      response.put("message", "Account is already activated !");
//      return ResponseEntity.badRequest()
//        .header("Content-Type", "application/json")
//        .body(response);
//    }
//
//    // If the token has expired
//    if (userToken.getExpiryDate().isBefore(LocalDateTime.now())) {
//      String requestNewTokenUrl = BACK_URL + "/request-confirmtoken?token=" + token;
//      response.put("type", "expiration");
//      response.put("message", "Link has expired. Please request a new one at the following link: ");
//      response.put("url", requestNewTokenUrl);
//      return ResponseEntity.badRequest()
//        .header("Content-Type", "application/json")
//        .body(response);
//    }
//
//    // Activate the user's account
//    authService.enableUser(user);
//    // Send the welcome email
//    mailerService.sendWelcomeEmail(user);
//
//    // Success response
//    response.put("message", "Thank you. Your account has been successfully activated. You can now use it to connect to your favorite app.");
//    return ResponseEntity.ok()
//      .header("Content-Type", "application/json")
//      .body(response);
//  }
//
//
//  /**
//   * Handles the request for a new confirmation token if the old one has expired.
//   *
//   * @param token The expired token.
//   * @return ResponseEntity with the status and a message.
//   */
//  @GetMapping("/request-confirmtoken")
//  public ResponseEntity<Map<String, String>> requestNewToken(@RequestParam String token) {
//    // Retrieve the old token from the service
//    AccountConfirmationToken verificationToken = accountConfirmationTokenService.getOne(token);
//
//    Map<String, String> response = new HashMap<>();
//    // If the token is invalid
//    if (verificationToken == null) {
//      response.put("error", "Invalid token");
//      return ResponseEntity.badRequest()
//        .header("Content-Type", "application/json")
//        .body(response);
//    }
//    // Generate a new token
//    String newToken = accountConfirmationTokenService.generateNewToken(token, AccountConfirmationToken.class, 86400L).getToken();
//    // Send the new confirmation email
//    mailerService.sendNewAccountConfirmation(newToken);
//
//    // Success response
//    response.put("message", "A new confirmation email has been sent.");
//    return ResponseEntity.ok()
//      .header("Content-Type", "application/json")
//      .body(response);
//  }
//}
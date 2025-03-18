package be.steby.CoreProject.bll.services.impl;


import be.steby.CoreProject.bll.services.MailerService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.utils.MailerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
@Slf4j
public class MailerServiceImpl implements MailerService {
  private final MailerUtil mailerUtil;

  @Value("${url.front_server}")
  private String FRONT_URL;

  private final String PASSWORD_URL = "/api/password";
  private final String ACCOUNT_URL = "/api/account-confirmation";
  private final String USER_URL = "/api/user";
  private final String DEVICE_URL = "/api/user";

  // region Password

  /**
   * Sends a password reset email to the user using a reset token.
   *
   * @param token the token used for resetting the password.
   */
  @Async
  @Override
  public void sendPasswordReset(String token, User user) {
    String resetUrl = FRONT_URL + PASSWORD_URL + "/reset-password?token=" + token;
    Context context = new Context();
    context.setVariable("username", user.getUsername());
    context.setVariable("url", resetUrl);
    context.setVariable("token", token);

    mailerUtil.sendMail("Password Reset", "passwords/NewPasswordRequest", context, user.getEmail());
  }

  @Async
  @Override
  public void sendPasswordResetRefresh(String newToken, User user) {
    String resetUrl = FRONT_URL + PASSWORD_URL + "/reset-password?token=" + newToken;
    Context context = new Context();
    context.setVariable("username", user.getUsername());
    context.setVariable("url", resetUrl);
    context.setVariable("token", newToken);

    mailerUtil.sendMail("Renouvellement de votre demande de réinitialisation", "passwords/PasswordResetRefresh", context, user.getEmail());
  }


  @Async
  @Override
  public void sendPasswordChangeConfirmation(User user) {
    Context context = new Context();
    context.setVariable("username", user.getFirstname() + " " + user.getLastname());
    mailerUtil.sendMail("Password Change Confirmation", "passwords/passwordChangeConfirmation", context, user.getEmail());
  }

  // endregion

  // region AccountConfirmation

  @Async
  @Override
  public void sendSignUpConfirmation(String token, User user) {
    String confirmationUrl = FRONT_URL + ACCOUNT_URL + "/activation?token=" + token;
    Context context = new Context();
    context.setVariable("username", user.getUsername());
    context.setVariable("temporaryPassword", "The password you defined");
    context.setVariable("url", confirmationUrl);
    mailerUtil.sendMail("Account confirmation", "accounts/signupConfirmation", context, user.getEmail());
  }


  @Async
  @Override
  public void sendAccountConfirmation(String token, User user, String temporaryPassword) {
    String confirmationUrl = FRONT_URL + ACCOUNT_URL + "/activation?token=" + token;
    Context context = new Context();
    context.setVariable("username", user.getFirstname() + " " + user.getLastname());
    context.setVariable("temporaryPassword", temporaryPassword);
    context.setVariable("url", confirmationUrl);
    mailerUtil.sendMail("Account confirmation", "accounts/accountConfirmation", context, user.getEmail());
  }

  @Async
  @Override
  public void sendNewAccountConfirmation(String token, User user) {
    String confirmationUrl = FRONT_URL + ACCOUNT_URL + "/activation?token=" + token;
    Context context = new Context();
    context.setVariable("username", user.getFirstname() + " " + user.getLastname());
    context.setVariable("url", confirmationUrl);
    mailerUtil.sendMail("Account confirmation", "accounts/newAccountConfirmationRequest", context, user.getEmail());
  }

  @Async
  @Override
  public void sendWelcome(User user) {
    Context context = new Context();
    String username = user.getFirstname() == null ? user.getUsername() : user.getFirstname() + " " + user.getLastname();
    context.setVariable("username", username);
    mailerUtil.sendMail("Welcome", "accounts/GreetingComfirmedUser", context, user.getEmail());
  }
  // endregion

  // region EmailAddress

  @Async
  @Override
  public void sendChangeEmailRequest(String token, User user) {
    String oldEmailConfirmationUrl = FRONT_URL + USER_URL + "/email-verification?token=" + token;
    String cancelChangeUrl = FRONT_URL + USER_URL + "/cancel-email-change?token=" + token;
    Context context = new Context();
    context.setVariable("username", user.getFirstname()+ " " + user.getLastname());
    context.setVariable("oldEmailConfirmationUrl", oldEmailConfirmationUrl);
    context.setVariable("cancelChangeUrl", cancelChangeUrl);
    mailerUtil.sendMail("Email confirmation", "emailAddresses/changeEmailRequest", context, user.getEmail());
  }

  @Async
  @Override
  public void sendChangeEmailVerification(String token, User user, String newEmail) {
    String newEmailConfirmationUrl = FRONT_URL + USER_URL + "/email-confirmation?token=" + token;
    Context context = new Context();
    context.setVariable("username", user.getFirstname()+ " " + user.getLastname());
    context.setVariable("newEmailConfirmationUrl", newEmailConfirmationUrl);
    mailerUtil.sendMail("Email confirmation", "emailAddresses/changeEmailVerification", context, newEmail);
  }

  @Async
  @Override
  public void sendChangeEmailConfirmation(String token, User user, String oldEmail, String newEmail) {
    String newEmailConfirmationUrl = FRONT_URL + USER_URL + "/email-confirmation?token=" + token;
    Context context = new Context();
    String username = user.getFirstname() == null ? user.getUsername() : user.getFirstname() + " " + user.getLastname();
    context.setVariable("username", username);
    context.setVariable("oldEmail", oldEmail);
    context.setVariable("newEmail", newEmail);
    mailerUtil.sendMail("Email confirmation", "emailAddresses/changeEmailConfirmation", context, oldEmail);
    mailerUtil.sendMail("Email confirmation", "emailAddresses/changeEmailConfirmation", context, newEmail);
  }

  //endregion

  // region Device

  @Async
  @Override
  public void sendNewDeviceAlert(User user, Device device, String token) {
    String newDeviceConfirmationUrl = FRONT_URL + DEVICE_URL + "/device-confirmation?token=" + token;
    Context context = new Context();
    String username = user.getFirstname() == null ? user.getUsername() : user.getFirstname() + " " + user.getLastname();
    String formattedTime = formatTime(device.getLastSeen());
    context.setVariable("username", username);
    context.setVariable("deviceType", device.getDeviceType());
    context.setVariable("confirmUrl", newDeviceConfirmationUrl);
    context.setVariable("location", device.getLocation());
    context.setVariable("timestamp", formattedTime);
    mailerUtil.sendMail("Security Alert: New Device Login", "devices/newDeviceAlert", context, user.getEmail());

  }

  @Async
  @Override
  public void sendBlacklistedDeviceAlert(User user, Device device, String token) {
    String newDeviceConfirmationUrl = FRONT_URL + DEVICE_URL + "/device-confirmation?token=" + token;
    Context context = new Context();
    String username = user.getFirstname() == null ? user.getUsername() : user.getFirstname() + " " + user.getLastname();
    String formattedTime = formatTime(device.getLastSeen());
    context.setVariable("username", username);
    context.setVariable("deviceType", device.getDeviceType());
    context.setVariable("confirmUrl", newDeviceConfirmationUrl);
    context.setVariable("location", device.getLocation());
    context.setVariable("timestamp", formattedTime);
    mailerUtil.sendMail("Security Alert: Blacklisted Device Login Attempt", "devices/blacklistedDeviceAlert", context, user.getEmail());
  }

  // endregion

  private String formatTime(Instant timestamp){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC")); // ou ZoneId.systemDefault() pour le fuseau local
    return formatter.format(timestamp) + "UTC";
  }

}
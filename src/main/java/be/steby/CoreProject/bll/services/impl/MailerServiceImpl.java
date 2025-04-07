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


  // region Password

  /**
   * Sends a password reset email to the user using a reset token.
   *
   * @param token the token used for resetting the password.
   */
  @Async
  @Override
  public void sendPasswordReset(String token, User user) {
    String resetUrl = FRONT_URL + "/auth/reset-password?token=" + token;
    String username = defineUsername(user);
    Context context = new Context();

    context.setVariable("username", username);
    context.setVariable("url", resetUrl);
    context.setVariable("token", token);

    mailerUtil.sendMail("Password Reset", "passwords/NewPasswordRequest", context, user.getEmail());
  }

  @Async
  @Override
  public void sendPasswordResetRefresh(String newToken, User user) {
    String resetUrl = FRONT_URL + "/auth/reset-password?token=" + newToken;
    String username = defineUsername(user);

    Context context = new Context();
    context.setVariable("username", username);
    context.setVariable("url", resetUrl);
    context.setVariable("token", newToken);

    mailerUtil.sendMail("Renouvellement de votre demande de réinitialisation", "passwords/PasswordResetRefresh", context, user.getEmail());
  }


  @Async
  @Override
  public void sendPasswordChangeConfirmation(User user) {
    String username = defineUsername(user);

    Context context = new Context();
    context.setVariable("username", username);
    mailerUtil.sendMail("Password Change Confirmation", "passwords/passwordChangeConfirmation", context, user.getEmail());
  }

  // endregion

  // region AccountConfirmation

  @Async
  @Override
  public void sendSignUpConfirmation(String token, User user) {
    String confirmationUrl = FRONT_URL + "/auth/account-confirmation?token=" + token;
    String username = defineUsername(user);

    Context context = new Context();
    context.setVariable("username", username);
    context.setVariable("temporaryPassword", "The password you defined");
    context.setVariable("url", confirmationUrl);
    mailerUtil.sendMail("Account confirmation", "accounts/signupConfirmation", context, user.getEmail());
  }


  @Async
  @Override
  public void sendAccountConfirmation(String token, User user, String temporaryPassword) {
    String confirmationUrl = FRONT_URL + "/auth/account-confirmation?token=" + token;
    String username = defineUsername(user);

    Context context = new Context();
    context.setVariable("username", username);
    context.setVariable("temporaryPassword", temporaryPassword);
    context.setVariable("url", confirmationUrl);
    mailerUtil.sendMail("Account confirmation", "accounts/accountConfirmation", context, user.getEmail());
  }

  @Async
  @Override
  public void sendNewAccountConfirmation(String token, User user) {
    String confirmationUrl = FRONT_URL + "/auth/account-confirmation?token=" + token;
    String username = defineUsername(user);

    Context context = new Context();
    context.setVariable("username", username);
    context.setVariable("url", confirmationUrl);
    mailerUtil.sendMail("Account confirmation", "accounts/newAccountConfirmationRequest", context, user.getEmail());
  }

  @Async
  @Override
  public void sendWelcome(User user) {
    String username = defineUsername(user);

    Context context = new Context();
    context.setVariable("username", username);
    mailerUtil.sendMail("Welcome", "accounts/GreetingComfirmedUser", context, user.getEmail());
  }
  // endregion

  // region EmailAddress

  @Async
  @Override
  public void sendChangeEmailRequest(String token, User user) {
    String oldEmailConfirmationUrl = FRONT_URL  + "/auth/verify-email?token=" + token + "&action=verify";
    String cancelChangeUrl = FRONT_URL +  "/auth/verify-email?token=" + token + "&action=cancel";
    String username = defineUsername(user);

    Context context = new Context();
    context.setVariable("username", username);
    context.setVariable("oldEmailConfirmationUrl", oldEmailConfirmationUrl);
    context.setVariable("cancelChangeUrl", cancelChangeUrl);
    mailerUtil.sendMail("Email confirmation", "emailAddresses/changeEmailRequest", context, user.getEmail());
  }

  @Async
  @Override
  public void sendChangeEmailVerification(String token, User user, String newEmail) {
    String newEmailConfirmationUrl = FRONT_URL  + "/auth/verify-email?token=" + token + "&action=confirm";
    String cancelChangeUrl = FRONT_URL +  "/auth/verify-email?token=" + token + "&action=cancel";
    String username = defineUsername(user);

    Context context = new Context();
    context.setVariable("username", username);
    context.setVariable("newEmailConfirmationUrl", newEmailConfirmationUrl);
    context.setVariable("cancelChangeUrl", cancelChangeUrl);
    mailerUtil.sendMail("Email confirmation", "emailAddresses/changeEmailVerification", context, newEmail);
  }

  @Async
  @Override
  public void sendChangeEmailConfirmation(String token, User user, String oldEmail, String newEmail) {
    String newEmailConfirmationUrl = FRONT_URL  + "/email-confirmation?token=" + token;
    Context context = new Context();
    String username = defineUsername(user);

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
    String confirmDeviceUrl = FRONT_URL + "/auth/device-confirmation?token=" + token + "&action=confirm";
    String rejectDeviceUrl = FRONT_URL + "/auth/device-confirmation?token=" + token + "&action=reject";

    Context context = new Context();
    String username = defineUsername(user);
    String formattedTime = formatTime(device.getLastSeen());

    context.setVariable("username", username);
    context.setVariable("deviceType", device.getDeviceType());
    context.setVariable("confirmDeviceUrl", confirmDeviceUrl);
    context.setVariable("revokeDeviceUrl", rejectDeviceUrl);
    context.setVariable("location", device.getLocation());
    context.setVariable("timestamp", formattedTime);
    context.setVariable("token", token);

    mailerUtil.sendMail("Security Alert: New Device Login", "devices/newDeviceAlert", context, user.getEmail());
  }

  @Async
  @Override
  public void sendBlacklistedDeviceAlert(User user, Device device, String token) {
    String whitelistDeviceUrl = FRONT_URL + "/auth/device-confirmation?token=" + token + "&action=confirm";

    Context context = new Context();
    String username = defineUsername(user);
    String formattedTime = formatTime(device.getLastSeen());

    context.setVariable("username", username);
    context.setVariable("deviceType", device.getDeviceType());
    context.setVariable("whitelistDeviceUrl", whitelistDeviceUrl);
    context.setVariable("location", device.getLocation());
    context.setVariable("timestamp", formattedTime);
    context.setVariable("token", token);

    mailerUtil.sendMail("Security Alert: Blacklisted Device Login Attempt", "devices/blacklistedDeviceAlert", context, user.getEmail());
  }

  // endregion


  private String defineUsername(User user){
    return user.getFirstname() == null ? user.getUsername() : user.getFirstname() + " " + user.getLastname();
  }

  private String formatTime(Instant timestamp){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC")); // ou ZoneId.systemDefault() pour le fuseau local
    return formatter.format(timestamp) + "UTC";
  }

}
package be.steby.CoreProject.bll.common.services.mailer;


import be.steby.CoreProject.bll.domains.account.services.DeactivationMessageService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.il.utils.MailerUtil;
import lombok.RequiredArgsConstructor;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.account.services.DeactivationMessageService;
import be.steby.CoreProject.il.utils.MailerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Service
@RequiredArgsConstructor
@Slf4j
public class MailerServiceImpl implements MailerService {
    private final MailerUtil mailerUtil;

    private final DeactivationMessageService deactivationMessageService;

    @Value("${url.front_server}")
    private String FRONT_URL;

    @Override
    public void sendPasswordReset(String token, User user) {

    }

    @Override
    public void sendPasswordResetRefresh(String newToken, User user) {

    }

    @Override
    public void sendAccountConfirmation(String token, User user, String temporaryPassword) {

    }

    @Override
    public void sendSignUpConfirmation(String token, User user) {
        String confirmationUrl = FRONT_URL + "/auth/account-confirmation?token=" + token;


        Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("temporaryPassword", "The password you defined");
        context.setVariable("url", confirmationUrl);
        mailerUtil.sendMail("Account confirmation", "accounts/signupConfirmation", context, user.getEmail());
    }

    @Override
    public void sendNewAccountConfirmation(String token, User user) {

    }

    @Override
    public void sendWelcome(User user) {

    }

    @Override
    public void sendAccountDeactivationRequest(String token, User user, DeactivationReason deactivationReason, String reasonDetails) {

    }

    @Override
    public void sendAccountDeactivationConfirmation(User user, DeactivationReason deactivationReason, String reasonDetails) {

    }

    @Override
    public void sendAccountReactivationRequest(String token, User user) {

    }

    @Override
    public void sendAccountReactivationConfirmation(User user) {

    }

    @Override
    public void sendPasswordChangeConfirmation(User user) {

    }

    @Override
    public void sendChangeEmailRequest(String token, User user) {

    }

    @Override
    public void sendChangeEmailVerification(String token, User user, String newEmail) {

    }

    @Override
    public void sendChangeEmailCancellation(User user) {

    }

    @Override
    public void sendChangeEmailConfirmation(String token, User user, String newEmail, String email) {

    }

    @Override
    public void sendNewDeviceAlert(User user, Device device, String token) {

    }

    @Override
    public void sendBlacklistedDeviceAlert(User user, Device device, String token) {

    }


    @Override
    public void sendTwoFactorEnabledConfirmation(User user, TwoFactorType type) {
        String username = user.getFirstname() != null && !user.getFirstname().isBlank()
                ? user.getFirstname()
                : user.getUsername();

        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("twoFactorType", type.getDisplayName());
        context.setVariable("twoFactorDescription", type.getRequirementDescription());

        // Email subject based on type
        String subject = "Two-Factor Authentication Enabled";

        mailerUtil.sendMail(
                subject,
                "auth/twoFactorEnabled",
                context,
                user.getEmail()
        );
    }
}
//
//  // region Password
//
//  /**
//   * Sends a password reset email to the user using a reset token.
//   *
//   * @param token the token used for resetting the password.
//   */
//  @Override
//  public void sendPasswordReset(String token, User user) {
//    String resetUrl = FRONT_URL + "/auth/reset-password?token=" + token;
//    String username = defineUsername(user);
//    Context context = new Context();
//
//    context.setVariable("username", username);
//    context.setVariable("url", resetUrl);
//    context.setVariable("token", token);
//
//    mailerUtil.sendMail("Password Reset", "passwords/NewPasswordRequest", context, user.getEmail());
//  }
//
//  @Override
//  public void sendPasswordResetRefresh(String newToken, User user) {
//    String resetUrl = FRONT_URL + "/auth/reset-password?token=" + newToken;
//    String username = defineUsername(user);
//
//    Context context = new Context();
//    context.setVariable("username", username);
//    context.setVariable("url", resetUrl);
//    context.setVariable("token", newToken);
//
//    mailerUtil.sendMail("Renouvellement de votre demande de réinitialisation", "passwords/PasswordResetRefresh", context, user.getEmail());
//  }
//
//
//  @Override
//  public void sendPasswordChangeConfirmation(User user) {
//    String username = defineUsername(user);
//
//    Context context = new Context();
//    context.setVariable("username", username);
//    mailerUtil.sendMail("Password Change Confirmation", "passwords/passwordChangeConfirmation", context, user.getEmail());
//  }
//
//  // endregion
//
////  // region Account
////
////  @Override
////  public void sendSignUpConfirmation(String token, User user) {
////    String confirmationUrl = FRONT_URL + "/auth/account-confirmation?token=" + token;
////    String username = defineUsername(user);
////
////    Context context = new Context();
////    context.setVariable("username", username);
////    context.setVariable("temporaryPassword", "The password you defined");
////    context.setVariable("url", confirmationUrl);
////    mailerUtil.sendMail("Account confirmation", "accounts/signupConfirmation", context, user.getEmail());
////  }
////
////
////  @Override
////  public void sendAccountConfirmation(String token, User user, String temporaryPassword) {
////    String confirmationUrl = FRONT_URL + "/auth/account-confirmation?token=" + token;
////    String username = defineUsername(user);
////
////    Context context = new Context();
////    context.setVariable("username", username);
////    context.setVariable("temporaryPassword", temporaryPassword);
////    context.setVariable("url", confirmationUrl);
////    mailerUtil.sendMail("Account confirmation", "accounts/accountConfirmation", context, user.getEmail());
////  }
////
////  @Override
////  public void sendNewAccountConfirmation(String token, User user) {
////    String confirmationUrl = FRONT_URL + "/auth/account-confirmation?token=" + token;
////    String username = defineUsername(user);
////
////    Context context = new Context();
////    context.setVariable("username", username);
////    context.setVariable("url", confirmationUrl);
////    mailerUtil.sendMail("Account confirmation", "accounts/newAccountConfirmationRequest", context, user.getEmail());
////  }
////
////  @Override
////  public void sendWelcome(User user) {
////    String username = defineUsername(user);
////
////    Context context = new Context();
////    context.setVariable("username", username);
////    mailerUtil.sendMail("Welcome", "accounts/GreetingComfirmedUser", context, user.getEmail());
////  }
////
////    @Override
////    public void sendAccountDeactivationRequest(String token, User user, DeactivationReason deactivationReason, String reasonDetails) {
////        String username = defineUsername(user);
////        String requestMessage = deactivationMessageService.getDeactivationRequestMessage(deactivationReason, reasonDetails);
////        String subject = deactivationMessageService.getDeactivationRequestSubjectLine(deactivationReason);
////
////        // ✅ FIX: Utilise directement l'enum field
////        boolean canReactivate = deactivationReason != null ? deactivationReason.allowsReactivation() : true;
////
////        String confirmationUrl = FRONT_URL + "/auth/account-deactivation?token=" + token;
////
////        Context context = new Context();
////        context.setVariable("username", username);
////        context.setVariable("requestMessage", requestMessage);
////        context.setVariable("subjectLine", subject);
////        context.setVariable("deactivationReason", deactivationReason);
////        context.setVariable("reasonDetails", reasonDetails);
////        context.setVariable("canReactivate", canReactivate);
////        context.setVariable("url", confirmationUrl);
////
////        mailerUtil.sendMail(subject, "accounts/AccountDeactivationRequest", context, user.getEmail());
////    }
////
////    @Override
////    public void sendAccountDeactivationConfirmation(User user, DeactivationReason deactivationReason, String reasonDetails) {
////        String username = defineUsername(user);
////        String message = deactivationMessageService.getConfirmationMessage(deactivationReason, reasonDetails);
////        String subject = deactivationMessageService.getSubjectLine(deactivationReason);
////
////        // ✅ FIX: Utilise directement l'enum field
////        boolean canReactivate = deactivationReason != null ? deactivationReason.allowsReactivation() : true;
////
////        Context context = new Context();
////        context.setVariable("username", username);
////        context.setVariable("message", message);
////        context.setVariable("subjectLine", subject);
////        context.setVariable("deactivationReason", deactivationReason);
////        context.setVariable("reasonDetails", reasonDetails);
////        context.setVariable("canReactivate", canReactivate);
////
////        mailerUtil.sendMail(subject, "accounts/AccountDeactivationConfirmation", context, user.getEmail());
////    }
////
////
////    @Override
////    public void sendAccountReactivationRequest(String token, User user) {
////        String username = defineUsername(user);
////        String subject = "Welcome Back! Confirm Your Account Reactivation – MyFavApp";
////        String confirmationUrl = FRONT_URL + "/auth/account-reactivation?token=" + token;
////
////        Context context = new Context();
////        context.setVariable("username", username);
////        context.setVariable("subjectLine", subject);
////        context.setVariable("url", confirmationUrl);
////        context.setVariable("originalDeactivationReason", user.getDeactivationReason());
////        context.setVariable("deactivatedDuration", getDurationSinceDeactivation(user));
////
////        mailerUtil.sendMail(subject, "accounts/AccountReactivationRequest", context, user.getEmail());
////    }
////
////
////    @Override
////    public void sendAccountReactivationConfirmation(User user) {
////        String username = defineUsername(user);
////        String subject = "Account Successfully Reactivated – MyFavApp";
////
////        Context context = new Context();
////        context.setVariable("username", username);
////        context.setVariable("subjectLine", subject);
////        context.setVariable("deactivatedDuration", getDurationSinceDeactivation(user));
////        context.setVariable("originalDeactivationReason", user.getDeactivationReason());
////
////        mailerUtil.sendMail(subject, "accounts/AccountReactivationConfirmation", context, user.getEmail());
////    }
////
////    // Méthode helper pour calculer la durée
////    private String getDurationSinceDeactivation(User user) {
////        if (user.getDeactivatedAt() == null) {
////            return "recently";
////        }
////
////        Duration duration = Duration.between(user.getDeactivatedAt(), Instant.now());
////        long days = duration.toDays();
////
////        if (days == 0) {
////            return "today";
////        } else if (days == 1) {
////            return "yesterday";
////        } else if (days < 7) {
////            return days + " days ago";
////        } else if (days < 30) {
////            long weeks = days / 7;
////            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
////        } else {
////            long months = days / 30;
////            return months == 1 ? "1 month ago" : months + " months ago";
////        }
////    }
////  // endregion
//
//  // region EmailAddress
//
//  @Override
//  public void sendChangeEmailRequest(String token, User user) {
//    String oldEmailConfirmationUrl = FRONT_URL  + "/auth/verify-email?token=" + token + "&action=verify";
//    String cancelChangeUrl = FRONT_URL +  "/auth/verify-email?token=" + token + "&action=cancel";
//    String username = defineUsername(user);
//
//    Context context = new Context();
//    context.setVariable("username", username);
//    context.setVariable("oldEmailConfirmationUrl", oldEmailConfirmationUrl);
//    context.setVariable("cancelChangeUrl", cancelChangeUrl);
//    mailerUtil.sendMail("Email change request", "emailAddresses/changeEmailRequest", context, user.getEmail());
//  }
//
//  @Override
//  public void sendChangeEmailVerification(String token, User user, String newEmail) {
//    String newEmailConfirmationUrl = FRONT_URL  + "/auth/verify-email?token=" + token + "&action=confirm";
//    String cancelChangeUrl = FRONT_URL +  "/auth/verify-email?token=" + token + "&action=cancel";
//    String username = defineUsername(user);
//
//    Context context = new Context();
//    context.setVariable("username", username);
//    context.setVariable("newEmailConfirmationUrl", newEmailConfirmationUrl);
//    context.setVariable("cancelChangeUrl", cancelChangeUrl);
//    mailerUtil.sendMail("Email change confirmation", "emailAddresses/changeEmailVerification", context, newEmail);
//  }
//
//
//  @Override
//  public void sendChangeEmailCancellation(User user) {
//    String username = defineUsername(user);
//
//    Context context = new Context();
//    context.setVariable("username", username);
//    mailerUtil.sendMail("Email change cancellation", "emailAddresses/changeEmailCancellation", context, user.getEmail());
//  }
//
//  @Override
//  public void sendChangeEmailConfirmation(String token, User user, String oldEmail, String newEmail) {
//    String newEmailConfirmationUrl = FRONT_URL  + "/email-confirmation?token=" + token;
//    Context context = new Context();
//    String username = defineUsername(user);
//
//    context.setVariable("username", username);
//    context.setVariable("oldEmail", oldEmail);
//    context.setVariable("newEmail", newEmail);
//    mailerUtil.sendMail("Email change success", "emailAddresses/changeEmailConfirmation", context, oldEmail);
//    mailerUtil.sendMail("Email change success", "emailAddresses/changeEmailConfirmation", context, newEmail);
//  }
//
//  //endregion
//
//  // region Device
//
//  @Override
//  public void sendNewDeviceAlert(User user, Device device, String token) {
//    String confirmDeviceUrl = FRONT_URL + "/auth/device-confirmation?token=" + token + "&action=confirm";
//    String rejectDeviceUrl = FRONT_URL + "/auth/device-confirmation?token=" + token + "&action=reject";
//
//    Context context = new Context();
//    String username = defineUsername(user);
//    String formattedTime = formatTime(device.getLastSeen());
//
//    context.setVariable("username", username);
//    context.setVariable("deviceType", device.getDeviceType());
//    context.setVariable("confirmDeviceUrl", confirmDeviceUrl);
//    context.setVariable("revokeDeviceUrl", rejectDeviceUrl);
//    context.setVariable("location", device.getLocation());
//    context.setVariable("timestamp", formattedTime);
//    context.setVariable("token", token);
//
//    mailerUtil.sendMail("Security Alert: New Device Login", "devices/newDeviceAlert", context, user.getEmail());
//  }
//
//  @Override
//  public void sendBlacklistedDeviceAlert(User user, Device device, String token) {
//    String whitelistDeviceUrl = FRONT_URL + "/auth/device-confirmation?token=" + token + "&action=confirm";
//
//    Context context = new Context();
//    String username = defineUsername(user);
//    String formattedTime = formatTime(device.getLastSeen());
//
//    context.setVariable("username", username);
//    context.setVariable("deviceType", device.getDeviceType());
//    context.setVariable("whitelistDeviceUrl", whitelistDeviceUrl);
//    context.setVariable("location", device.getLocation());
//    context.setVariable("timestamp", formattedTime);
//    context.setVariable("token", token);
//
//    mailerUtil.sendMail("Security Alert: Blacklisted Device Login Attempt", "devices/blacklistedDeviceAlert", context, user.getEmail());
//  }
//
//  // endregion
//
//
//  private String defineUsername(User user){
//    return user.getFirstname() == null ? user.getUsername() : user.getFirstname() + " " + user.getLastname();
//  }
//
//  private String formatTime(Instant timestamp){
//    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//            .withZone(ZoneId.of("UTC")); // ou ZoneId.systemDefault() pour le fuseau local
//    return formatter.format(timestamp) + "UTC";
//  }
//
//}